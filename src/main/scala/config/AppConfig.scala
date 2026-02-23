package config

object AppConfig {
  val LOG_FILE_PATH = "/app/logs/openstack-nova-sample"

  // PostgreSQL
  val POSTGRES_USER = "db_user"
  val POSTGRES_PASSWORD = "password"
  private val POSTGRES_DB_NAME = "otus"
  val POSTGRES_TABLE = "nova_logs"
  val POSTGRES_URL: String = "jdbc:postgresql://postgres:5432/" + POSTGRES_DB_NAME
  val POSTGRES_DRIVER = "org.postgresql.Driver"

  // Kafka
  val KAFKA_BOOTSTRAP_SERVERS = "kafka:9092"
  val KAFKA_TOPIC_RAW = "nova-logs-raw"

  // ClickHouse
  val CLICKHOUSE_URL = "jdbc:clickhouse://clickhouse:8123/"
  val CLICKHOUSE_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver"
  val CLICKHOUSE_TABLE_ERROR = "nova_error_analysis"
  val CLICKHOUSE_TABLE_STATS = "nova_error_stats"
  val CLICKHOUSE_USER = "default"
  val CLICKHOUSE_PASSWORD = ""

  val JDBC_FORMAT = "jdbc"
  val KAFKA_FORMAT = "kafka"

}
