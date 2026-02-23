package service.kafka

import config.AppConfig
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Producer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaProducer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    println("Reading data from PostgreSQL...")

    // 1. Читаем из PostgreSQL
    val sourceDF = spark.read
      .format(AppConfig.JDBC_FORMAT)
      .option("url", AppConfig.POSTGRES_URL)
      .option("dbtable", AppConfig.POSTGRES_TABLE)
      .option("user", AppConfig.POSTGRES_USER)
      .option("password", AppConfig.POSTGRES_PASSWORD)
      .option("driver", AppConfig.POSTGRES_DRIVER)
      .load()

    val rowCount = sourceDF.count()
    println(s"Read $rowCount rows from PostgreSQL")

    if (rowCount == 0) {
      println("No data found in PostgreSQL. Exiting.")
      spark.stop()
      return
    }

    println("Sample of data to be sent to Kafka:")
    sourceDF.show(5, false)

    // 2. Преобразуем в JSON для Kafka
    val kafkaMessages = sourceDF
      .select(to_json(struct("*")).as("value"))
      .selectExpr("CAST(value AS STRING)")

    // 3. Отправляем в Kafka
    println(s"Sending $rowCount messages to Kafka topic '${AppConfig.KAFKA_TOPIC_RAW}'...")

    kafkaMessages.write
      .format(AppConfig.KAFKA_FORMAT)
      .option("kafka.bootstrap.servers", AppConfig.KAFKA_BOOTSTRAP_SERVERS)
      .option("topic", AppConfig.KAFKA_TOPIC_RAW)
      .save()

    println("Successfully sent messages to Kafka")

    spark.stop()
  }
}