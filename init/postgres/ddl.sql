\c otus;

CREATE TABLE IF NOT EXISTS nova_logs (
    id SERIAL PRIMARY KEY,
    level VARCHAR(10),
    component VARCHAR(100),
    request_id VARCHAR(50),
    instance_id VARCHAR(50),
    message TEXT,
    processing_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS nova_errors (
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    level VARCHAR(50) NOT NULL,
    component VARCHAR(255) NOT NULL,
    count BIGINT NOT NULL DEFAULT 0,
    processing_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_nova_logs_level ON nova_logs(level);
CREATE INDEX idx_nova_errors_window_start ON nova_errors (window_start);
CREATE INDEX idx_nova_errors_level ON nova_errors (level);

GRANT ALL PRIVILEGES ON TABLE nova_logs TO db_user;
GRANT ALL PRIVILEGES ON TABLE nova_errors TO db_user;

DO $$
BEGIN
    RAISE NOTICE 'Database initialized successfully';
END $$;