-- Add unique constraint to prevent duplicate feedback per grievance by same user
ALTER TABLE feedback
  ADD CONSTRAINT uq_feedback_grievance_user UNIQUE (grievance_id, user_id);
