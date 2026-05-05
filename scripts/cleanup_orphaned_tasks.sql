-- SQL Script to cleanup orphaned Approval Tasks
-- These tasks point to Campaigns or Expenditures that do not exist in the database.

-- 1. Identify orphaned CAMPAIGN tasks
-- View them first:
-- SELECT * FROM approval_tasks WHERE type = 'CAMPAIGN' AND target_id NOT IN (SELECT id FROM campaigns);

-- 2. Identify orphaned EXPENDITURE/EVIDENCE tasks
-- View them first:
-- SELECT * FROM approval_tasks WHERE (type = 'EXPENDITURE' OR type = 'EVIDENCE') AND target_id NOT IN (SELECT id FROM expenditures);

-- 3. Cleanup orphaned tasks
-- WARNING: This will permanently delete the task records.

-- DELETE FROM approval_tasks 
-- WHERE type = 'CAMPAIGN' 
-- AND target_id NOT IN (SELECT id FROM campaigns);

-- DELETE FROM approval_tasks 
-- WHERE (type = 'EXPENDITURE' OR type = 'EVIDENCE') 
-- AND target_id NOT IN (SELECT id FROM expenditures);

-- 4. Check for orphaned notifications (optional)
-- SELECT * FROM notifications WHERE target_type = 'CAMPAIGN' AND target_id NOT IN (SELECT id FROM campaigns);
-- SELECT * FROM notifications WHERE target_type = 'EXPENDITURE' AND target_id NOT IN (SELECT id FROM expenditures);
