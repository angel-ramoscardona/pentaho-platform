-- Migrate data from old Quartz 1.7.2 tables to new 2.3.2 tables ----
-- Quartz 1.x tables are prefixed with QRTZ5_ while 2.x tables are prefixed with QRTZ6_
-- Script will leave the old tables unmodified and populate the new tables built with the create_quartz-oracle.sql
-- with the 1.X data.  Make sure you are connected to the quartz schema as the quartz user.
INSERT INTO QRTZ6_JOB_DETAILS
  (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)
SELECT
  'PentahoQuartzScheduler', JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_STATEFUL, IS_STATEFUL, REQUESTS_RECOVERY, JOB_DATA
FROM QRTZ5_JOB_DETAILS;

INSERT INTO QRTZ6_TRIGGERS
  (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, PRIORITY, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA)
SELECT
  'PentahoQuartzScheduler', TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, PRIORITY, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA
FROM QRTZ5_TRIGGERS;

INSERT INTO QRTZ6_CRON_TRIGGERS
  (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID)
SELECT
  'PentahoQuartzScheduler', TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID
FROM QRTZ5_CRON_TRIGGERS;

INSERT INTO QRTZ6_SIMPLE_TRIGGERS
  (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED)
SELECT
  'PentahoQuartzScheduler', TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED
FROM QRTZ5_SIMPLE_TRIGGERS;

-- Writing SCHED_TIME as FIRED_TIME because it is not defined anywhere
INSERT INTO QRTZ6_FIRED_TRIGGERS
  (SCHED_NAME, ENTRY_ID, TRIGGER_NAME, TRIGGER_GROUP, INSTANCE_NAME, FIRED_TIME, SCHED_TIME, PRIORITY, STATE, JOB_NAME, JOB_GROUP, IS_NONCONCURRENT, REQUESTS_RECOVERY)
SELECT
  'PentahoQuartzScheduler', ENTRY_ID, TRIGGER_NAME, TRIGGER_GROUP, INSTANCE_NAME, FIRED_TIME, FIRED_TIME, PRIORITY, STATE, JOB_NAME, JOB_GROUP, IS_STATEFUL, REQUESTS_RECOVERY
FROM QRTZ5_FIRED_TRIGGERS;

INSERT INTO QRTZ6_CALENDARS
  (SCHED_NAME, CALENDAR_NAME, CALENDAR)
SELECT
  'PentahoQuartzScheduler', CALENDAR_NAME, CALENDAR
FROM QRTZ5_CALENDARS;

INSERT INTO QRTZ6_PAUSED_TRIGGER_GRPS
  (SCHED_NAME, TRIGGER_GROUP)
SELECT
  'PentahoQuartzScheduler', TRIGGER_GROUP
FROM QRTZ6_PAUSED_TRIGGER_GRPS;

INSERT INTO QRTZ6_SCHEDULER_STATE
  (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL)
SELECT
  'PentahoQuartzScheduler', INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL
FROM QRTZ5_SCHEDULER_STATE;

COMMIT;
