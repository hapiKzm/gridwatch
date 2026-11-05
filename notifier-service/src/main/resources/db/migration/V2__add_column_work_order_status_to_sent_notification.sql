ALTER TABLE sent_notification ADD COLUMN work_order_status TEXT NOT NULL DEFAULT 'OPEN';
ALTER TABLE sent_notification DROP CONSTRAINT sent_notification_pkey;
ALTER TABLE sent_notification ADD PRIMARY KEY (work_order_id, work_order_status);
