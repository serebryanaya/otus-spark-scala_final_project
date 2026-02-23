package service.emulator

import config.AppConfig
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object LogProcessor {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("LogProcessor")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    println(s"Reading log file from: ${AppConfig.LOG_FILE_PATH}")

    // Читаем файл
    val rawDF = spark.read.text(AppConfig.LOG_FILE_PATH).toDF("raw_line")
    val totalCount = rawDF.count()
    println(s"Total lines: $totalCount")

    // Парсим только существующие поля
    val parsedDF = rawDF
      .withColumn("level",
        when($"raw_line".contains("INFO"), "INFO")
          .when($"raw_line".contains("WARNING"), "WARNING")
          .when($"raw_line".contains("ERROR"), "ERROR")
          .when($"raw_line".contains("DEBUG"), "DEBUG")
          .otherwise("UNKNOWN"))
      .withColumn("component",
        regexp_extract($"raw_line", """(?:INFO|WARNING|ERROR|DEBUG)\s+([a-zA-Z0-9.]+)""", 1))
      .withColumn("request_id",
        regexp_extract($"raw_line", """req-([a-f0-9-]+)""", 1))
      .withColumn("instance_id",
        regexp_extract($"raw_line", """instance:\s*([a-f0-9-]+)""", 1))
      .withColumn("message", $"raw_line")

    // Добавляем вычисляемые поля
    val enrichedDF = parsedDF
      .withColumn("processing_time", current_timestamp())
      .withColumn("is_error", $"level" === "ERROR")
      .withColumn("is_warning", $"level" === "WARNING")
      .withColumn("hour", hour($"processing_time"))
      .withColumn("date", to_date($"processing_time"))
      .select(
        $"level",
        $"component",
        $"request_id",
        $"instance_id",
        $"message",
        $"processing_time",
        $"is_error",
        $"is_warning",
        $"hour",
        $"date"
      )

    val validCount = enrichedDF.filter($"level" =!= "UNKNOWN").count()
    println(s"Valid records: $validCount/$totalCount")

    if (validCount > 0) {
      println("Sample of parsed data:")
      enrichedDF.select("level", "component", "request_id", "instance_id", "message")
        .show(10, false)

      // Запись в PostgreSQL
      val jdbcProps = new java.util.Properties()
      jdbcProps.setProperty("user", AppConfig.POSTGRES_USER)
      jdbcProps.setProperty("password", AppConfig.POSTGRES_PASSWORD)
      jdbcProps.setProperty("driver", AppConfig.POSTGRES_DRIVER)

      enrichedDF.write
        .mode("append")
        .jdbc(AppConfig.POSTGRES_URL, AppConfig.POSTGRES_TABLE, jdbcProps)

      println(s"Successfully wrote $validCount records to PostgreSQL")
    } else {
      println("No valid records found")
      rawDF.show(10, false)
    }

    spark.stop()
  }
}