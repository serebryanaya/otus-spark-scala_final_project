package service.kafka

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
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

      // Читаем из Kafka
      val kafkaDF = spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", AppConfig.KAFKA_BOOTSTRAP_SERVERS)
        .option("subscribe", AppConfig.KAFKA_TOPIC_RAW)
        .option("startingOffsets", "earliest")  // Читаем с начала
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "10000")
        .option("groupIdPrefix", "spark-consumer") // Добавляем group.id
        .load()

      val parsedData = kafkaDF
        .selectExpr("CAST(value AS STRING) as json")
        .select(from_json(col("json"), logRecordSchema).alias("data"))
        .select("data.*")
        .filter(col("level") === "ERROR") // Только те, что распарсились


      val errorStats = parsedData
        .withWatermark("processing_time", "1 hour") // Увеличили с 10 минут до 1 часа
        .groupBy(
          window(col("processing_time"), "5 minutes"),
          col("level"),
          col("component")
        )
        .agg(count("*").as("count"))
        .select(
          col("window.start").as("window_start"),
          col("window.end").as("window_end"),
          col("level"),
          col("component"),
          col("count"),
          current_timestamp().as("processing_time")
        )


      val query = errorStats.writeStream
        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
          // batchDF - это микро-батч данных, которые нужно записать
          // batchId - уникальный идентификатор батча

          println(s"Processing batch $batchId with ${batchDF.count()} rows")

          if (!batchDF.isEmpty) {
            // Записываем батч в PostgreSQL
            batchDF.write
              .mode("append") // Добавляем данные (не перезаписываем)
              .jdbc(
                AppConfig.POSTGRES_URL, // URL базы данных
                AppConfig.POSTGRES_TABLE_ERRORS, // Имя таблицы
                jdbcProps // Настройки подключения
              )

            println(s"✅ Batch $batchId written to PostgreSQL")
          }
        }
        .outputMode("update") // Для агрегаций с watermark лучше update
        .trigger(Trigger.ProcessingTime("10 seconds"))
        .option("checkpointLocation", "/tmp/spark-checkpoints/postgres-write")
        .start()

      // ДИАГНОСТИКА 4: Статистика по уровням
//
//      errorStats.writeStream
//        .format("console")
//        .outputMode("complete")
//        .trigger(Trigger.ProcessingTime("10 seconds"))
//        .start()

      println("🟢 All diagnostic queries started!")
      println("  - Raw messages → console")
      println("  - Parse errors → console")
      println("  - Parsed messages → console")
      println("  - Level statistics → console")

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