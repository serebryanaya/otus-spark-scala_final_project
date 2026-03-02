## Проектная работа по курсу OTUS Spark Developer
# Анализатор системных логов приложений, запущенных на виртуальной машине
Проект предназначен анализа системных логов с использованием Apache Spark, Kafka, PostgreSQL и Grafana. Данные проходят полный цикл от симуляции потока логов до визуализации аналитики в дашборде.

## Архитектура


![Диаграмма без названия-2](https://github.com/user-attachments/assets/2a90b06f-bfcc-43b2-8bba-f559564f9476)


### Этап 1: Загрузка данных (LogProcessor)

#### Класс service.emulator.LogProcessor:

- Читает файл-сэмпл логов OpenStack nova с сайта Kaggle (https://www.kaggle.com/datasets/parisakalaki/openstack-logs?resource=download)
- Парсит логи с помощью Spark
- Сохраняет данные в PostgreSQL (таблица nova_logs)

### Этап 2: Отправка в Kafka (Producer)

#### Класс service.kafka.Producer:

- Читает данные из PostgreSQL
- Преобразует их в JSON
- С помощью Spark отправляет в Kafka топик nova-logs-raw
- Работает по Cron и запускается каждые 2 минуты

### Этап 3: Анализ и витрина (Consumer)

#### Класс service.kafka.Consumer:

- Читает поток данных из Kafka  с помощью Spark Structured Streaming
- Вычисляет статистику по ошибкам
- Создает витрину в Postgres (таблица nova_errors)

### Этап 4: Визуализация (Grafana)

Дашборд показывает временной ряд количества ошибок в разрезе сервисов


<img width="1297" height="729" alt="Снимок экрана 2026-03-02 в 19 47 13" src="https://github.com/user-attachments/assets/11a8bd1a-6651-4e7c-be6b-32d3654524a0" />


## Системные требования

- Docker и Docker Compose
- SBT (для сборки)
- 8+ GB RAM

## Технологический стек

- Scala 2.12 - язык разработки
- Apache Spark 3.5.5 - обработка данных
- Kafka - очередь сообщений
- Kafka UI - веб-интерфейс для Kafka
- PostgreSQL - база данных
- Grafana - визуализация
- Docker - контейнеризация
- SBT - сборка проекта

## Установка и запуск
#### 1. Клонировать репозиторий
- git clone <repository-url>
- cd project_final

#### 2. Собрать проект
sbt clean assembly

#### 3. Запустить все сервисы
docker-compose up -d

#### 4. Проверить статус
docker-compose ps

#### 5. Смотреть логи
docker-compose logs -f error-detector
docker exec error-detector tail -f /var/log/producer-cron.log  

#### 6. Пример запроса для визуализации в Grafana (bar chart)

SELECT
  DATE_TRUNC('hour', window_start) as time,
  component,
  SUM(count)::float as total_errors
FROM nova_errors
WHERE $__timeFilter(window_start)
GROUP BY DATE_TRUNC('hour', window_start), component
ORDER BY time ASC, component

#### 7. Остановка сервисов и очистка

docker-compose down -v ; \
docker stop $(docker ps -aq); \
docker rm -f $(docker ps -aq) ; \
docker rmi -f $(docker images -aq) ;  \
docker network prune -f  ; \
docker volume prune -f -a 
