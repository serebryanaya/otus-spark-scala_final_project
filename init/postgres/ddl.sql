\c otus;

CREATE TABLE IF NOT EXISTS nova_logs (
    id SERIAL PRIMARY KEY,
    level VARCHAR(10),
    component VARCHAR(100),
    request_id VARCHAR(50),
    instance_id VARCHAR(50),
    message TEXT,
    processing_time TIMESTAMP,
    is_error BOOLEAN,
    is_warning BOOLEAN,
    hour INTEGER,
    date DATE
);

CREATE INDEX idx_nova_logs_level ON nova_logs(level);

GRANT ALL PRIVILEGES ON TABLE nova_logs TO db_user;

-- Сообщение об успехе
DO $$
BEGIN
    RAISE NOTICE 'Database initialized successfully';
END $$;