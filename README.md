## Проектная работа по курсу OTUS Spark Developer
# Анализатор системных логов приложений, запущенных на виртуальной машине
Проект предназначен анализа системных логов с использованием Apache Spark, Kafka, PostgreSQL, ClickHouse и Grafana. Данные проходят полный цикл от симуляции потока логов до визуализации аналитики в дашборде.

## Архитектура

![Диаграмма без названия](https://github.com/user-attachments/assets/388cc710-a096-4df4-b5bd-120a71b9e83b)

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

### Этап 3: Анализ и витрина (Consumer)

#### Класс service.kafka.Consumer:

- Читает поток данных из Kafka  с помощью Spark Structured Streaming
- Вычисляет статистику по ошибкам
- Создает витрину в ClickHouse (таблица nova_error_stats)

### Этап 4: Визуализация (Grafana)

Дашборд показывает временной ряд количества ошибок в разрезе сервисов


<img width="963" height="389" alt="Снимок экрана 2026-02-24 в 15 22 05" src="https://github.com/user-attachments/assets/5dfeeaf6-477a-4c55-813f-6249b3f8c5da" />


## Системные требования

- Docker и Docker Compose
- SBT (для сборки)
- 8+ GB RAM
- JVM 8+

## Технологический стек

- Scala 2.12 - язык разработки
- Apache Spark 3.5.5 - обработка данных
- Kafka - очередь сообщений
- Kafka UI - веб-интерфейс для Kafka
- PostgreSQL - хранилище сырых данных
- ClickHouse - аналитическая БД
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

