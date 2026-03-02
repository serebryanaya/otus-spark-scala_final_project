package config

object AppConfig {
  val LOG_FILE_PATH = "/app/logs/openstack-nova-sample"

  // PostgreSQL
  val POSTGRES_USER = "db_user"
  val POSTGRES_PASSWORD = "password"
  private val POSTGRES_DB_NAME = "otus"
  val POSTGRES_TABLE_RAW_LOGS = "nova_logs"
  val POSTGRES_TABLE_ERRORS = "nova_errors"
  val POSTGRES_URL: String = "jdbc:postgresql://postgres:5432/" + POSTGRES_DB_NAME
  val POSTGRES_DRIVER = "org.postgresql.Driver"

  // Kafka
  val KAFKA_BOOTSTRAP_SERVERS = "kafka:9092"
  val KAFKA_TOPIC_RAW = "nova-logs-raw"
}
