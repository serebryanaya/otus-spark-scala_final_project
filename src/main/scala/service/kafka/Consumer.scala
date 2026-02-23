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


//      val query = errorAnalysis.writeStream
//        .outputMode("append")
//        .format("console")
//        .option("truncate", "false")
//        .trigger(Trigger.ProcessingTime("10 seconds"))
//        .start()

      // Сохраняем аналитику в ClickHouse
//      val clickhouseSink = errorAnalysis.writeStream
//        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
//          batchDF.write
//
//            .format("clickhouse")
//            .option("host", "clickhouse")
//            .option("protocol", "https")
//            .option("http_port", "8123")
//            .option("database", "default")
//            .option("table", AppConfig.CLICKHOUSE_TABLE_ERROR)
//            .option("user", AppConfig.CLICKHOUSE_USER)
//            .option("password", AppConfig.CLICKHOUSE_PASSWORD)
//            .option("ssl", "true")
//            .mode("append")
//            .save()

//          batchDF.write
//            .mode("append")
//            .format("jdbc")
//            .option("url", AppConfig.CLICKHOUSE_URL)
//            .option("dbtable", AppConfig.CLICKHOUSE_TABLE_ERROR)
//            .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
//            .option("user", AppConfig.CLICKHOUSE_USER)
//            .option("password", AppConfig.CLICKHOUSE_PASSWORD)
//            .option("batchsize", "10000")
//            .save()
//        }
//        .trigger(Trigger.ProcessingTime("30 seconds"))
//        .start()

      // Считаем общую статистику
      val generalStats = parsedData
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

      val statsToClickhouse = generalStats.writeStream
        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
          batchDF.write
            .mode("append")
            .format("jdbc")
            .option("url", AppConfig.CLICKHOUSE_URL)
            .option("dbtable", AppConfig.CLICKHOUSE_TABLE_STATS)
            .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
            .option("user", AppConfig.CLICKHOUSE_USER)
            .option("password", AppConfig.CLICKHOUSE_PASSWORD)
//            .option("batchsize", "10000")
            .save()
        }
        .trigger(Trigger.ProcessingTime("1 minute"))
        .start()

//      clickhouseSink.awaitTermination()
      statsToClickhouse.awaitTermination()
//      query.awaitTermination()
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