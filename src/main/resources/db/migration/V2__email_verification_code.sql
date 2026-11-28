-- Email verification switches from a link with a token to a 6-digit code.
-- failed_attempts: counts wrong attempts so the code can be invalidated past the limit (brute-force protection).
-- For EMAIL_VERIFY, token_hash stores the BCrypt hash (60 chars) of the code; PASSWORD_RESET still uses SHA-256.
ALTER TABLE account_tokens
  ADD COLUMN failed_attempts INT UNSIGNED NOT NULL DEFAULT 0 AFTER used_at;

-- Old link-based verification tokens can no longer be used; invalidate them so users request a new code.
UPDATE account_tokens
SET used_at = CURRENT_TIMESTAMP(6)
WHERE purpose = 'EMAIL_VERIFY' AND used_at IS NULL;
