-- Remove obsolete enabled column from users table if it exists
ALTER TABLE users DROP COLUMN enabled;
