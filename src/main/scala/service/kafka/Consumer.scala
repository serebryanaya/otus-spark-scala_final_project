package service.kafka

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{Trigger}
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

      val kafkaDF = spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", AppConfig.KAFKA_BOOTSTRAP_SERVERS)
        .option("subscribe", AppConfig.KAFKA_TOPIC_RAW)
        .option("startingOffsets", "earliest")
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "10000")
        .option("groupIdPrefix", "spark-consumer")
        .load()

      val parsedData = kafkaDF
        .selectExpr("CAST(value AS STRING) as json")
        .select(from_json(col("json"), logRecordSchema).alias("data"))
        .select("data.*")
        .filter(col("level") === "ERROR")


      val errorStats = parsedData
        .withWatermark("processing_time", "1 hour")
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

          println(s"Processing batch $batchId with ${batchDF.count()} rows")

          if (!batchDF.isEmpty) {
            batchDF.write
              .mode("append")
              .jdbc(
                AppConfig.POSTGRES_URL,
                AppConfig.POSTGRES_TABLE_ERRORS,
                jdbcProps
              )

            println(s"✅ Batch $batchId written to PostgreSQL")
          }
        }
        .outputMode("update")
        .trigger(Trigger.ProcessingTime("10 seconds"))
        .option("checkpointLocation", "/tmp/spark-checkpoints/postgres-write")
        .start()

      query.awaitTermination()

    } catch {
      case e: Exception =>
        println(s"Error in Consumer: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
}