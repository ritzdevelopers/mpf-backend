-- Enquiry message was VARCHAR(255); home-loan eligibility notes overflowed it.
ALTER TABLE enquiries
  MODIFY COLUMN message TEXT NULL;
