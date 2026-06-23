-- Allow draft (2) and scheduled (3) blog statuses.
-- Legacy constraint only allowed status 0 (inactive) and 1 (published).
ALTER TABLE blogs DROP CHECK blogs_chk_1;
ALTER TABLE blogs ADD CONSTRAINT blogs_chk_1 CHECK (status >= 0 AND status <= 3);
