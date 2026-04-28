-- Earliest date a task may be performed. NULL means "as soon as possible"
-- (the task shows up immediately on the dashboard once created). When set,
-- the dashboard hides the task until earliest_date <= today, while the
-- dedicated Tasks list groups it under "Kommande" / "Upcoming".
ALTER TABLE scheduled_task
    ADD COLUMN earliest_date DATE;
