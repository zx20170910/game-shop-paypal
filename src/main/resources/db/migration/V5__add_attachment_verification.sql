ALTER TABLE fulfillment_attachments
    ADD COLUMN verified_at TIMESTAMP(6) NULL AFTER status,
    ADD COLUMN verified_by VARCHAR(128) NULL AFTER verified_at,
    ADD KEY idx_attachment_status (fulfillment_id, status);
