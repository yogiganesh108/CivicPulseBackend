-- Add columns to store reopen evidence (image + note)
ALTER TABLE grievances ADD COLUMN reopen_image_data LONGBLOB NULL;
ALTER TABLE grievances ADD COLUMN reopen_image_type VARCHAR(255) NULL;
ALTER TABLE grievances ADD COLUMN reopen_note VARCHAR(2000) NULL;
