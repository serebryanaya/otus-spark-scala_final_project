package service.kafka

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import config.AppConfig
import org.apache.spark.sql.types._

object Consumer {

  val logRecordSchema: StructType = StructType(Array(
    StructField("level", StringType, true),
    StructField("component", StringType, true),
    StructField("request_id", StringType, true),
    StructField("instance_id", StringType, true),
    StructField("message", StringType, true),
    StructField("processing_time", TimestampType, true),
    StructField("is_error", BooleanType, true),
    StructField("is_warning", BooleanType, true),
    StructField("hour", IntegerType, true),
    StructField("date", DateType, true)
  ))

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SparkAnalyzer")
      .master("local[*]")
      .config("spark.driver.extraClassPath", "clickhouse-jdbc-0.1.54.jar")
      .config("spark.executor.extraClassPath", "clickhouse-jdbc-0.1.54.jar")
      .getOrCreate()

    import spark.implicits._

    // Читаем из Kafka
    try {
      val kafkaDF = spark.readStream
        .format(AppConfig.KAFKA_FORMAT)
        .option("kafka.bootstrap.servers", AppConfig.KAFKA_BOOTSTRAP_SERVERS)
        .option("subscribe", AppConfig.KAFKA_TOPIC_RAW)
        .option("startingOffsets", "earliest")
        .load()

      // Парсим JSON
      val parsedData = kafkaDF
        .selectExpr("CAST(value AS STRING) as json")
        .select(from_json($"json", logRecordSchema).as("data"))
        .select("data.*")

      // Фильтруем ошибки
      val errors = parsedData.filter($"is_error" === true)

      // Аналитика по ошибкам
      val errorAnalysis = errors
        .withWatermark("processing_time", "10 minutes")
        .groupBy(
          window($"processing_time", "5 minutes"),
          $"component",
          $"instance_id"
        )
        .agg(
          count("*").as("error_count"),
          concat_ws(",", collect_list($"message")).as("error_messages")
        )

      // Сохраняем аналитику в ClickHouse
      val clickhouseSink = errorAnalysis.writeStream
        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>

          batchDF.write
            .mode("append")
            .format("jdbc")
            .option("url", AppConfig.CLICKHOUSE_URL)
            .option("dbtable", AppConfig.CLICKHOUSE_TABLE_ERROR)
            .option("driver", "ru.yandex.clickhouse.ClickHouseDriver")
            .option("user", AppConfig.CLICKHOUSE_USER)
            .option("password", AppConfig.CLICKHOUSE_PASSWORD)
            .option("batchsize", "10000")
            .save()
        }
        .trigger(Trigger.ProcessingTime("30 seconds"))
        .start()

      // Считаем общую статистику
      val generalStats = errors
        .withWatermark("processing_time", "10 minutes")
        .groupBy(
          window($"processing_time", "1 hour"),
          $"level",
          $"component"
        )
        .agg(
          count("*").as("count"),
          avg(length($"message")).as("avg_message_length")
        )
        .select(
          $"window.start".as("window_start"),
          $"window.end".as("window_end"),
          $"level",
          $"component",
          $"count",
          $"avg_message_length",
          current_timestamp().as("processing_time")  // добавляем время обработки
        )

      val statsToClickhouse = generalStats.writeStream
        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
          batchDF.write
            .mode("append")
            .format("jdbc")
            .option("url", AppConfig.CLICKHOUSE_URL)
            .option("dbtable", AppConfig.CLICKHOUSE_TABLE_STATS)
            .option("driver", "ru.yandex.clickhouse.ClickHouseDriver")
            .option("user", AppConfig.CLICKHOUSE_USER)
            .option("password", AppConfig.CLICKHOUSE_PASSWORD)
            .option("batchsize", "1000")
            .save()
        }
        .trigger(Trigger.ProcessingTime("1 minute"))
        .start()

      clickhouseSink.awaitTermination()
      statsToClickhouse.awaitTermination()
    }

    catch {
      case e: Exception =>
        println(s"Error in Consumer: ${e.getMessage}")
        e.printStackTrace()
    }
    finally {
      spark.stop()
    }
  }
}