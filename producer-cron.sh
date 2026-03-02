#!/bin/bash

echo "Starting scheduled producer at $(date)" >> /var/log/producer-cron.log

spark-submit \
  --class service.kafka.Producer \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.5 \
  /app/app.jar >> /var/log/producer-cron.log 2>&1

echo "Producer finished at $(date)" >> /vçar/log/producer-cron.log