#!/bin/bash

# Ожидание доступности сервисов
echo "Waiting for services to be ready..."

echo "POSTGRES_HOST: $POSTGRES_HOST"
echo "POSTGRES_USER: $POSTGRES_USER"
echo "POSTGRES_PASSWORD: $POSTGRES_PASSWORD"
echo "POSTGRES_DB: $POSTGRES_DB"

# Ожидание PostgreSQL
echo "Waiting for PostgreSQL..."
until nc -z $POSTGRES_HOST 5432; do
    echo "Waiting for PostgreSQL port to be open..."
    sleep 2
done

sleep 5

echo "KAFKA_BOOTSTRAP_SERVERS: $KAFKA_BOOTSTRAP_SERVERS"
echo "KAFKA_TOPIC: $KAFKA_TOPIC"

# Ожидание Kafka
KAFKA_HOST=$(echo $KAFKA_BOOTSTRAP_SERVERS | cut -d: -f1)
KAFKA_PORT=$(echo $KAFKA_BOOTSTRAP_SERVERS | cut -d: -f2)

until nc -z $KAFKA_HOST $KAFKA_PORT; do
  echo "Waiting for Kafka ($KAFKA_HOST:$KAFKA_PORT)..."
  sleep 2
done

# Ожидание ClickHouse
until nc -z $CLICKHOUSE_HOST $CLICKHOUSE_PORT; do
  echo "Waiting for CLickhouse ($CLICKHOUSE_HOST:$CLICKHOUSE_PORT)..."
  sleep 2
done

echo "All services are ready. Starting Spark application..."

# Запуск Spark приложения
spark-submit \
  --class service.emulator.LogProcessor \
  --master local[*] \
  /app/app.jar

# После загрузки в Postgres, запускаем Kafka Producer
spark-submit \
  --class service.kafka.Producer \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.5 \
  /app/app.jar

# Затем запускаем анализ Spark
spark-submit \
  --class service.kafka.Consumer \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.5,com.clickhouse:clickhouse-jdbc:0.9.5,ru.yandex.clickhouse:clickhouse-jdbc:0.1.54 \
  /app/app.jar