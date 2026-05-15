-- TODO-style tasks have no due date; relax the column to allow NULL.
-- target_count stays NOT NULL — TODOs always store 1.
ALTER TABLE scheduled_task
    ALTER COLUMN deadline DROP NOT NULL;
