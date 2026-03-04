-- ── Task Manager — PostgreSQL Initialization ─────────────────────────────────
-- Creates the three service-specific databases.
-- The default 'taskmanager' database from the monolith is no longer used.
-- ─────────────────────────────────────────────────────────────────────────────

-- Auth Service database
SELECT 'CREATE DATABASE auth_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db') \gexec

-- Task Service database
SELECT 'CREATE DATABASE task_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'task_db') \gexec

-- Notification Service database
SELECT 'CREATE DATABASE notification_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db') \gexec

-- Grant the postgres superuser access (already has it, but explicit for clarity)
GRANT ALL PRIVILEGES ON DATABASE auth_db         TO postgres;
GRANT ALL PRIVILEGES ON DATABASE task_db         TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
