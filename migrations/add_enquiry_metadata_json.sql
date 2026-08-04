-- Store lead intelligence metadata from website tracking
ALTER TABLE enquiries
  ADD COLUMN IF NOT EXISTS metadata_json JSON NULL AFTER property_id,
  ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(20) NULL AFTER phone;
