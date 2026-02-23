FROM apache/spark:3.5.5

USER root

RUN apt-get update && apt-get install -y \
    netcat-openbsd \
    postgresql-client \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Копирование jar файлов приложения
COPY target/scala-2.12/project_final-assembly*.jar /app/app.jar

# Добавление Spark в PATH
ENV SPARK_HOME=/opt/spark
ENV PATH=$SPARK_HOME/bin:$PATH

# Копирование лог файла
COPY logs/openstack-nova-sample /app/logs/openstack-nova-sample

# Скрипт для запуска приложения
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

WORKDIR /app

ENTRYPOINT ["/app/start.sh"]