CREATE DATABASE IF NOT EXISTS default;

CREATE TABLE IF NOT EXISTS default.nova_error_analysis (
    window_start DateTime,
    window_end DateTime,
    component String,
    instance_id String,
    error_count UInt32,
    error_messages String,
    processing_time DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY (window_start, component);

CREATE TABLE IF NOT EXISTS default.nova_error_stats (
    window_start DateTime,
    window_end DateTime,
    level String,
    component String,
    count UInt64,
    avg_message_length Float64,
    processing_time DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY (window_start, level);