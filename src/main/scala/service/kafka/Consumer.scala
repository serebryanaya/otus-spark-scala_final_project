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
    StructField("processing_time", TimestampType, true)
  ))

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SparkAnalyzer")
      .master("local[*]")
      .config("spark.sql.streaming.checkpointLocation", "/tmp/spark-checkpoints")
      .config("spark.sql.streaming.forceDeleteTempCheckpointLocation", "true")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val jdbcProps = new java.util.Properties()
    jdbcProps.setProperty("user", AppConfig.POSTGRES_USER)
    jdbcProps.setProperty("password", AppConfig.POSTGRES_PASSWORD)
    jdbcProps.setProperty("driver", AppConfig.POSTGRES_DRIVER)
    jdbcProps.setProperty("batchsize", "10000")
    jdbcProps.setProperty("rewriteBatchedInserts", "true")

    import spark.implicits._

    try {
      println("🟡 Starting Kafka consumer...")
      println(s"Connecting to Kafka: ${AppConfig.KAFKA_BOOTSTRAP_SERVERS}")
      println(s"Subscribing to topic: ${AppConfig.KAFKA_TOPIC_RAW}")

      // Чистая Kafka конфигурация без лишних опций
      val kafkaDF = spark.readStream
        .format(AppConfig.KAFKA_FORMAT)
        .option("kafka.bootstrap.servers", AppConfig.KAFKA_BOOTSTRAP_SERVERS)
        .option("subscribe", AppConfig.KAFKA_TOPIC_RAW)
        .option("startingOffsets", "earliest")
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "10000")
        .load()

      // Парсим JSON
      val parsedData = kafkaDF
        .selectExpr("CAST(value AS STRING) as json")
        .select(from_json(col("json"), logRecordSchema).as("data"))
        .select("data.*")

      // Фильтруем только ошибки
//      val errors = parsedData.filter(col("level") === "ERROR")
      val errors = parsedData

      // Агрегируем ошибки по часам
      val errorStats = errors
//        .withWatermark("processing_time", "1 minute")
        .groupBy(
//          window(col("processing_time"), "1 hour"),
          col("level"),
          col("component")
        )
        .agg(count("*").as("count"))
        .select(
//          col("window.start").as("window_start"),
//          col("window.end").as("window_end"),
          col("level"),
          col("component"),
          col("count"),
          current_timestamp().as("processing_time")
        )

      // Записываем только в nova_errors
      val errorStatsQuery = errorStats.writeStream
        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
          val count = batchDF.count()
          println(s"\n📊 ERROR STATS BATCH $batchId at ${java.time.Instant.now()}")
          println(s"Error groups: $count")

          if (count > 0) {
            println("Error groups found:")
            batchDF.show(10, false)

            val totalErrors = batchDF.agg(sum("count")).collect()(0).getLong(0)
            println(s"Total individual error records in this batch: $totalErrors")

            try {
              batchDF.write
                .mode("update")
                .jdbc(AppConfig.POSTGRES_URL, AppConfig.POSTGRES_TABLE_ERRORS, jdbcProps)
              println(s"✅ Successfully wrote $count error groups (representing $totalErrors errors) to ${AppConfig.POSTGRES_TABLE_ERRORS}")
            } catch {
              case e: Exception =>
                println(s"❌ Failed to write error stats: ${e.getMessage}")
                e.printStackTrace()
            }
          } else {
            println("ℹ️ No error groups in this batch")
          }
        }
        .trigger(Trigger.ProcessingTime("1 minute"))
        .option("checkpointLocation", "/tmp/spark-checkpoints/error-stats")
        .start()

      println("🟢 Streaming query started!")
      println("  - Error stats -> nova_errors (ONLY)")

      // Ожидаем завершения
      spark.streams.awaitAnyTermination()

    } catch {
      case e: Exception =>
        println(s"❌ Error in Consumer: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
}