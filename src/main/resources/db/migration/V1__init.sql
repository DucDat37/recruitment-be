-- =============================================================
-- CSDL hệ thống tuyển dụng tích hợp AI - phiên bản 2
-- Mục tiêu: MySQL 8.0.16 trở lên (InnoDB, utf8mb4)
-- Tác giả: được sinh từ bản thiết kế v1 sau khi rà soát
-- Lưu ý: các giá trị ENUM là giả định, cần đối chiếu với nghiệp vụ.
-- =============================================================

-- Database được chọn qua spring.datasource.url (recruitment_db), Flyway không tạo database.
-- Không dùng DROP ở đây: Flyway chỉ chạy migration một lần và ghi lịch sử vào flyway_schema_history.

-- =============================================================
-- 1. TÀI KHOẢN VÀ XÁC THỰC
-- =============================================================

CREATE TABLE users (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email             VARCHAR(320)    NOT NULL,
  password_hash     VARCHAR(255)    NOT NULL,
  role              ENUM('CANDIDATE','RECRUITER','ADMIN') NOT NULL,
  status            ENUM('ACTIVE','LOCKED','DISABLED')    NOT NULL DEFAULT 'ACTIVE',
  email_verified_at DATETIME(6)     NULL,
  failed_login_count INT UNSIGNED   NOT NULL DEFAULT 0,      -- số lần đăng nhập sai liên tiếp
  locked_until      DATETIME(6)     NULL,                    -- khóa tạm thời đến thời điểm này
  created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_role_status (role, status)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
  id          CHAR(36)        NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  token_hash  CHAR(64)        NOT NULL,
  expires_at  DATETIME(6)     NOT NULL,
  revoked_at  DATETIME(6)     NULL,
  replaced_by CHAR(36)        NULL,                      -- token mới được cấp khi xoay vòng
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_refresh_tokens_hash (token_hash),
  KEY idx_refresh_tokens_user (user_id),
  KEY idx_refresh_tokens_replaced_by (replaced_by),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by)
    REFERENCES refresh_tokens (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Token dùng một lần cho xác thực email (EMAIL_VERIFY) và đặt lại mật khẩu (PASSWORD_RESET)
CREATE TABLE account_tokens (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  purpose     ENUM('EMAIL_VERIFY','PASSWORD_RESET') NOT NULL,
  token_hash  CHAR(64)        NOT NULL,              -- SHA-256 của token gửi qua email
  expires_at  DATETIME(6)     NOT NULL,
  used_at     DATETIME(6)     NULL,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_account_tokens_hash (token_hash),
  KEY idx_account_tokens_user_purpose (user_id, purpose),
  CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- 2. DOANH NGHIỆP, NHÀ TUYỂN DỤNG, ỨNG VIÊN
-- =============================================================

CREATE TABLE companies (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(200)    NOT NULL,
  slug        VARCHAR(220)    NOT NULL,
  tax_code    VARCHAR(30)     NULL,
  description TEXT            NULL,
  website     VARCHAR(500)    NULL,
  logo_key    VARCHAR(600)    NULL,
  address     VARCHAR(500)    NULL,
  industry    VARCHAR(120)    NULL,
  size_range  VARCHAR(40)     NULL,
  status      ENUM('PENDING','VERIFIED','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
  verified_at DATETIME(6)     NULL,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_companies_slug (slug),
  UNIQUE KEY uq_companies_tax_code (tax_code),   -- MySQL cho phép nhiều NULL trong UNIQUE
  KEY idx_companies_status (status)
) ENGINE=InnoDB;

CREATE TABLE recruiter_profiles (
  user_id         BIGINT UNSIGNED NOT NULL,
  company_id      BIGINT UNSIGNED NOT NULL,
  full_name       VARCHAR(150)    NOT NULL,
  job_title       VARCHAR(120)    NULL,
  phone           VARCHAR(30)     NULL,
  membership_role ENUM('OWNER','ADMIN','MEMBER') NOT NULL DEFAULT 'MEMBER',
  joined_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id),
  KEY idx_recruiter_company (company_id),
  CONSTRAINT fk_recruiter_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_recruiter_company FOREIGN KEY (company_id)
    REFERENCES companies (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Mới: hồ sơ cá nhân của ứng viên (trước đây thiếu)
CREATE TABLE candidate_profiles (
  user_id    BIGINT UNSIGNED NOT NULL,
  full_name  VARCHAR(150)    NOT NULL,
  headline   VARCHAR(200)    NULL,
  phone      VARCHAR(30)     NULL,
  address    VARCHAR(250)    NULL,
  created_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id),
  CONSTRAINT fk_candidate_profile_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Quan hệ 1-1 với users (khóa chính là user_id)
CREATE TABLE candidate_preferences (
  candidate_id      BIGINT UNSIGNED NOT NULL,
  desired_title     VARCHAR(180)    NULL,
  preferred_locations JSON          NULL,
  employment_types  JSON            NULL,
  salary_min        DECIMAL(15,2)   NULL,
  salary_currency   CHAR(3)         NOT NULL DEFAULT 'VND',
  remote_preference ENUM('ONSITE','HYBRID','REMOTE','ANY') NOT NULL DEFAULT 'ANY',
  updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (candidate_id),
  CONSTRAINT chk_candidate_pref_salary CHECK (salary_min IS NULL OR salary_min >= 0),
  CONSTRAINT fk_candidate_pref_user FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- 3. KỸ NĂNG VÀ DANH MỤC NGHỀ
-- =============================================================

CREATE TABLE skills (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name            VARCHAR(120)    NOT NULL,
  normalized_name VARCHAR(120)    NOT NULL,
  category        VARCHAR(80)     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_skills_normalized_name (normalized_name)
) ENGINE=InnoDB;

CREATE TABLE skill_aliases (
  id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  skill_id BIGINT UNSIGNED NOT NULL,
  alias    VARCHAR(150)    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_skill_aliases_skill_alias (skill_id, alias),
  CONSTRAINT fk_skill_aliases_skill FOREIGN KEY (skill_id)
    REFERENCES skills (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE job_categories (
  id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NULL,
  name      VARCHAR(120)    NOT NULL,
  slug      VARCHAR(150)    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_job_categories_slug (slug),
  KEY idx_job_categories_parent (parent_id),
  CONSTRAINT fk_job_categories_parent FOREIGN KEY (parent_id)
    REFERENCES job_categories (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================
-- 4. TIN TUYỂN DỤNG
-- =============================================================

CREATE TABLE jobs (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id            BIGINT UNSIGNED NOT NULL,
  created_by            BIGINT UNSIGNED NOT NULL,
  category_id           BIGINT UNSIGNED NULL,
  title                 VARCHAR(200)    NOT NULL,
  slug                  VARCHAR(240)    NOT NULL,
  description           TEXT            NOT NULL,
  requirements          TEXT            NULL,
  benefits              TEXT            NULL,
  employment_type       ENUM('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','FREELANCE') NOT NULL,
  workplace_type        ENUM('ONSITE','HYBRID','REMOTE') NOT NULL,
  experience_level      ENUM('NO_EXPERIENCE','JUNIOR','MIDDLE','SENIOR','LEAD','MANAGER') NULL,
  min_experience_months INT UNSIGNED    NOT NULL DEFAULT 0,
  salary_min            DECIMAL(15,2)   NULL,
  salary_max            DECIMAL(15,2)   NULL,
  salary_currency       CHAR(3)         NOT NULL DEFAULT 'VND',
  location              VARCHAR(250)    NULL,
  openings              INT UNSIGNED    NOT NULL DEFAULT 1,
  status                ENUM('DRAFT','PUBLISHED','PAUSED','CLOSED','EXPIRED') NOT NULL DEFAULT 'DRAFT',
  published_at          DATETIME(6)     NULL,
  expires_at            DATETIME(6)     NULL,
  created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_jobs_slug (slug),
  KEY idx_jobs_company (company_id),
  KEY idx_jobs_status_published (status, published_at),
  KEY idx_jobs_category (category_id),
  KEY idx_jobs_created_by (created_by),
  CONSTRAINT chk_jobs_salary CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max),
  CONSTRAINT chk_jobs_openings CHECK (openings >= 1),
  CONSTRAINT fk_jobs_company FOREIGN KEY (company_id)
    REFERENCES companies (id) ON DELETE RESTRICT,
  CONSTRAINT fk_jobs_created_by FOREIGN KEY (created_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_jobs_category FOREIGN KEY (category_id)
    REFERENCES job_categories (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE job_skills (
  job_id      BIGINT UNSIGNED NOT NULL,
  skill_id    BIGINT UNSIGNED NOT NULL,
  is_required BOOLEAN         NOT NULL DEFAULT TRUE,
  weight      DECIMAL(5,2)    NOT NULL DEFAULT 1.00,
  PRIMARY KEY (job_id, skill_id),
  KEY idx_job_skills_skill (skill_id),
  CONSTRAINT chk_job_skills_weight CHECK (weight >= 0),
  CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE,
  CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id)
    REFERENCES skills (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- 5. CV (RESUME)
-- =============================================================

CREATE TABLE resumes (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  candidate_id       BIGINT UNSIGNED NOT NULL,
  original_filename  VARCHAR(255)    NOT NULL,
  storage_key        VARCHAR(700)    NOT NULL,
  content_type       VARCHAR(100)    NOT NULL,
  size_bytes         BIGINT UNSIGNED NOT NULL,
  sha256             CHAR(64)        NOT NULL,
  is_default         BOOLEAN         NOT NULL DEFAULT FALSE,
  parse_status       ENUM('PENDING','PROCESSING','DONE','FAILED') NOT NULL DEFAULT 'PENDING',
  created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  deleted_at         DATETIME(6)     NULL,
  -- Cột sinh tự động: chỉ có giá trị khi CV là mặc định và chưa bị xóa.
  -- UNIQUE trên cột này đảm bảo mỗi ứng viên có tối đa một CV mặc định đang hoạt động.
  default_candidate_id BIGINT UNSIGNED
    AS (CASE WHEN is_default AND deleted_at IS NULL THEN candidate_id END) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uq_resumes_storage_key (storage_key),
  UNIQUE KEY uq_resumes_owner (id, candidate_id),          -- phục vụ FK ghép từ applications và recommendation_batches
  UNIQUE KEY uq_resumes_default (default_candidate_id),
  KEY idx_resumes_candidate (candidate_id, deleted_at),
  KEY idx_resumes_candidate_sha (candidate_id, sha256),
  CONSTRAINT fk_resumes_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Mới: kỹ năng đã trích xuất từ CV, dùng để tính skill_score
CREATE TABLE resume_skills (
  resume_id  BIGINT UNSIGNED NOT NULL,
  skill_id   BIGINT UNSIGNED NOT NULL,
  confidence DECIMAL(5,4)    NULL,
  PRIMARY KEY (resume_id, skill_id),
  KEY idx_resume_skills_skill (skill_id),
  CONSTRAINT chk_resume_skills_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
  CONSTRAINT fk_resume_skills_resume FOREIGN KEY (resume_id)
    REFERENCES resumes (id) ON DELETE CASCADE,
  CONSTRAINT fk_resume_skills_skill FOREIGN KEY (skill_id)
    REFERENCES skills (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- 6. ỨNG TUYỂN
-- =============================================================

CREATE TABLE applications (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_id             BIGINT UNSIGNED NOT NULL,
  candidate_id       BIGINT UNSIGNED NOT NULL,
  resume_id          BIGINT UNSIGNED NULL,
  resume_snapshot_key VARCHAR(700)   NOT NULL,   -- bản sao CV tại thời điểm nộp
  cover_letter       TEXT            NULL,
  status             ENUM('APPLIED','REVIEWING','INTERVIEWING','OFFERED','HIRED','REJECTED','WITHDRAWN')
                     NOT NULL DEFAULT 'APPLIED',
  applied_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_applications_job_candidate (job_id, candidate_id),   -- mỗi ứng viên chỉ nộp một lần cho một job
  KEY idx_applications_candidate (candidate_id, applied_at),
  KEY idx_applications_job_status (job_id, status),
  CONSTRAINT fk_applications_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE RESTRICT,
  CONSTRAINT fk_applications_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE RESTRICT,
  -- Đảm bảo CV được nộp thuộc đúng ứng viên đã nộp (NULL được bỏ qua kiểm tra)
  CONSTRAINT fk_applications_resume_owner FOREIGN KEY (resume_id, candidate_id)
    REFERENCES resumes (id, candidate_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE application_status_history (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  application_id BIGINT UNSIGNED NOT NULL,
  changed_by     BIGINT UNSIGNED NULL,
  old_status     ENUM('APPLIED','REVIEWING','INTERVIEWING','OFFERED','HIRED','REJECTED','WITHDRAWN') NULL,
  new_status     ENUM('APPLIED','REVIEWING','INTERVIEWING','OFFERED','HIRED','REJECTED','WITHDRAWN') NOT NULL,
  note           TEXT            NULL,
  changed_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_status_history_application (application_id, changed_at),
  CONSTRAINT fk_status_history_application FOREIGN KEY (application_id)
    REFERENCES applications (id) ON DELETE CASCADE,
  CONSTRAINT fk_status_history_changed_by FOREIGN KEY (changed_by)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================
-- 7. MÔ HÌNH AI VÀ KẾT QUẢ XỬ LÝ
-- =============================================================

CREATE TABLE model_versions (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  model_name          VARCHAR(120)    NOT NULL,
  version             VARCHAR(60)     NOT NULL,
  task                ENUM('EXTRACTION','EMBEDDING','MATCHING','RECOMMENDATION') NOT NULL,
  artifact_uri        VARCHAR(700)    NULL,
  embedding_dimensions INT UNSIGNED   NULL,
  metrics             JSON            NULL,
  status              ENUM('DRAFT','ACTIVE','RETIRED') NOT NULL DEFAULT 'DRAFT',
  created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_model_versions_name_version (model_name, version),
  KEY idx_model_versions_task_status (task, status)
) ENGINE=InnoDB;

CREATE TABLE resume_extractions (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resume_id        BIGINT UNSIGNED NOT NULL,
  model_version_id BIGINT UNSIGNED NOT NULL,
  extracted_text   LONGTEXT        NULL,
  extracted_data   JSON            NULL,
  confidence       DECIMAL(5,4)    NULL,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_resume_extractions_resume_model (resume_id, model_version_id),
  CONSTRAINT chk_resume_extractions_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
  CONSTRAINT fk_resume_extractions_resume FOREIGN KEY (resume_id)
    REFERENCES resumes (id) ON DELETE CASCADE,
  CONSTRAINT fk_resume_extractions_model FOREIGN KEY (model_version_id)
    REFERENCES model_versions (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE resume_embeddings (
  resume_id        BIGINT UNSIGNED NOT NULL,
  model_version_id BIGINT UNSIGNED NOT NULL,
  vector_ref       VARCHAR(255)    NOT NULL,   -- khóa vector trong kho vector (ví dụ FAISS, pgvector, Qdrant)
  source_hash      CHAR(64)        NOT NULL,   -- hash nội dung đầu vào, dùng để biết khi nào cần tính lại
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (resume_id, model_version_id),
  CONSTRAINT fk_resume_embeddings_resume FOREIGN KEY (resume_id)
    REFERENCES resumes (id) ON DELETE CASCADE,
  CONSTRAINT fk_resume_embeddings_model FOREIGN KEY (model_version_id)
    REFERENCES model_versions (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE job_embeddings (
  job_id           BIGINT UNSIGNED NOT NULL,
  model_version_id BIGINT UNSIGNED NOT NULL,
  vector_ref       VARCHAR(255)    NOT NULL,
  source_hash      CHAR(64)        NOT NULL,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (job_id, model_version_id),
  CONSTRAINT fk_job_embeddings_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE,
  CONSTRAINT fk_job_embeddings_model FOREIGN KEY (model_version_id)
    REFERENCES model_versions (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Điểm được chuẩn hóa về thang 0-1
CREATE TABLE match_results (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resume_id        BIGINT UNSIGNED NOT NULL,
  job_id           BIGINT UNSIGNED NOT NULL,
  model_version_id BIGINT UNSIGNED NOT NULL,
  score            DECIMAL(6,5)    NOT NULL,
  semantic_score   DECIMAL(6,5)    NULL,
  skill_score      DECIMAL(6,5)    NULL,
  explanation      JSON            NULL,
  resume_source_hash CHAR(64)      NOT NULL,
  job_source_hash    CHAR(64)      NOT NULL,
  computed_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_match_results_triple (resume_id, job_id, model_version_id),
  KEY idx_match_results_job_score (job_id, score),
  CONSTRAINT chk_match_score CHECK (score >= 0 AND score <= 1),
  CONSTRAINT chk_match_semantic CHECK (semantic_score IS NULL OR (semantic_score >= 0 AND semantic_score <= 1)),
  CONSTRAINT chk_match_skill CHECK (skill_score IS NULL OR (skill_score >= 0 AND skill_score <= 1)),
  CONSTRAINT fk_match_results_resume FOREIGN KEY (resume_id)
    REFERENCES resumes (id) ON DELETE CASCADE,
  CONSTRAINT fk_match_results_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE,
  CONSTRAINT fk_match_results_model FOREIGN KEY (model_version_id)
    REFERENCES model_versions (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE recommendation_batches (
  id               CHAR(36)        NOT NULL,
  candidate_id     BIGINT UNSIGNED NOT NULL,
  resume_id        BIGINT UNSIGNED NOT NULL,
  model_version_id BIGINT UNSIGNED NOT NULL,
  generated_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_rec_batches_candidate (candidate_id, generated_at),
  CONSTRAINT fk_rec_batches_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE RESTRICT,
  -- CV của lượt gợi ý phải thuộc đúng ứng viên
  CONSTRAINT fk_rec_batches_resume_owner FOREIGN KEY (resume_id, candidate_id)
    REFERENCES resumes (id, candidate_id) ON DELETE RESTRICT,
  CONSTRAINT fk_rec_batches_model FOREIGN KEY (model_version_id)
    REFERENCES model_versions (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE recommendation_items (
  batch_id  CHAR(36)        NOT NULL,
  job_id    BIGINT UNSIGNED NOT NULL,
  rank_no   INT UNSIGNED    NOT NULL,
  score     DECIMAL(6,5)    NOT NULL,
  reason    JSON            NULL,
  PRIMARY KEY (batch_id, job_id),
  UNIQUE KEY uq_rec_items_batch_rank (batch_id, rank_no),   -- thứ hạng là duy nhất trong mỗi lượt gợi ý
  CONSTRAINT chk_rec_items_rank CHECK (rank_no >= 1),
  CONSTRAINT chk_rec_items_score CHECK (score >= 0 AND score <= 1),
  CONSTRAINT fk_rec_items_batch FOREIGN KEY (batch_id)
    REFERENCES recommendation_batches (id) ON DELETE CASCADE,
  CONSTRAINT fk_rec_items_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE ai_processing_jobs (
  id            CHAR(36)        NOT NULL,
  resume_id     BIGINT UNSIGNED NULL,
  job_id        BIGINT UNSIGNED NULL,
  task_type     ENUM('PARSE_RESUME','EMBED_RESUME','EMBED_JOB','MATCH') NOT NULL,
  status        ENUM('QUEUED','RUNNING','SUCCEEDED','FAILED') NOT NULL DEFAULT 'QUEUED',
  attempts      INT UNSIGNED    NOT NULL DEFAULT 0,
  max_attempts  INT UNSIGNED    NOT NULL DEFAULT 3,
  error_message TEXT            NULL,
  created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  started_at    DATETIME(6)     NULL,
  completed_at  DATETIME(6)     NULL,
  PRIMARY KEY (id),
  KEY idx_ai_jobs_status_created (status, created_at),
  KEY idx_ai_jobs_resume (resume_id),
  KEY idx_ai_jobs_job (job_id),
  -- Mỗi loại tác vụ phải có đúng các khóa đích tương ứng
  CONSTRAINT chk_ai_jobs_targets CHECK (
       (task_type IN ('PARSE_RESUME','EMBED_RESUME') AND resume_id IS NOT NULL)
    OR (task_type = 'EMBED_JOB' AND job_id IS NOT NULL)
    OR (task_type = 'MATCH' AND resume_id IS NOT NULL AND job_id IS NOT NULL)
  ),
  CONSTRAINT chk_ai_jobs_attempts CHECK (attempts <= max_attempts),
  CONSTRAINT fk_ai_jobs_resume FOREIGN KEY (resume_id)
    REFERENCES resumes (id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_jobs_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- 8. TƯƠNG TÁC CỦA ỨNG VIÊN
-- =============================================================

CREATE TABLE saved_jobs (
  candidate_id BIGINT UNSIGNED NOT NULL,
  job_id       BIGINT UNSIGNED NOT NULL,
  created_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (candidate_id, job_id),
  KEY idx_saved_jobs_job (job_id),
  CONSTRAINT fk_saved_jobs_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_saved_jobs_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE company_followers (
  candidate_id BIGINT UNSIGNED NOT NULL,
  company_id   BIGINT UNSIGNED NOT NULL,
  created_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (candidate_id, company_id),
  KEY idx_company_followers_company (company_id),
  CONSTRAINT fk_company_followers_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_company_followers_company FOREIGN KEY (company_id)
    REFERENCES companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE candidate_job_events (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  job_id       BIGINT UNSIGNED NOT NULL,
  event_type   ENUM('VIEW','SAVE','APPLY','RECOMMEND_CLICK','RECOMMEND_DISMISS') NOT NULL,
  occurred_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  metadata     JSON            NULL,
  PRIMARY KEY (id),
  KEY idx_events_candidate_time (candidate_id, occurred_at),
  KEY idx_events_job_type_time (job_id, event_type, occurred_at),
  CONSTRAINT fk_events_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_events_job FOREIGN KEY (job_id)
    REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE job_alerts (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  name         VARCHAR(120)    NOT NULL,
  filters      JSON            NOT NULL,
  frequency    ENUM('INSTANT','DAILY','WEEKLY') NOT NULL DEFAULT 'DAILY',
  enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_job_alerts_candidate (candidate_id),
  KEY idx_job_alerts_schedule (enabled, frequency),
  CONSTRAINT fk_job_alerts_candidate FOREIGN KEY (candidate_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- 9. THÔNG BÁO, BÁO CÁO, KIỂM TOÁN, CẤU HÌNH
-- =============================================================

CREATE TABLE notifications (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL,
  type       VARCHAR(60)     NOT NULL,
  title      VARCHAR(200)    NOT NULL,
  body       TEXT            NULL,
  payload    JSON            NULL,
  read_at    DATETIME(6)     NULL,
  created_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_notifications_user_read (user_id, read_at, created_at),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE notification_preferences (
  user_id                    BIGINT UNSIGNED NOT NULL,
  email_enabled              BOOLEAN         NOT NULL DEFAULT TRUE,
  in_app_enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
  job_recommendations_enabled BOOLEAN        NOT NULL DEFAULT TRUE,
  updated_at                 DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id),
  CONSTRAINT fk_notification_prefs_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE reports (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reporter_id BIGINT UNSIGNED NOT NULL,
  target_type ENUM('JOB','COMPANY','USER') NOT NULL,
  target_id   BIGINT UNSIGNED NOT NULL,           -- liên kết đa hình, kiểm tra tồn tại ở tầng ứng dụng
  reason      VARCHAR(120)    NOT NULL,
  details     TEXT            NULL,
  status      ENUM('OPEN','REVIEWING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'OPEN',
  reviewed_by BIGINT UNSIGNED NULL,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  resolved_at DATETIME(6)     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_reports_once (reporter_id, target_type, target_id),
  KEY idx_reports_target (target_type, target_id),
  KEY idx_reports_status_created (status, created_at),
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_reports_reviewer FOREIGN KEY (reviewed_by)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  actor_id    BIGINT UNSIGNED NULL,               -- NULL khi hành động do hệ thống thực hiện
  action      VARCHAR(100)    NOT NULL,
  entity_type VARCHAR(80)     NOT NULL,
  entity_id   VARCHAR(100)    NOT NULL,
  details     JSON            NULL,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_audit_entity (entity_type, entity_id),
  KEY idx_audit_actor_time (actor_id, created_at),
  KEY idx_audit_created (created_at),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE system_settings (
  setting_key VARCHAR(120)    NOT NULL,           -- đổi tên từ `key` vì là từ khóa dành riêng trong MySQL
  value       JSON            NOT NULL,
  updated_by  BIGINT UNSIGNED NULL,
  updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (setting_key),
  CONSTRAINT fk_system_settings_user FOREIGN KEY (updated_by)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================
-- CHANGELOG so với v1
-- [SỬA LỖI] applications: UNIQUE(job_id, candidate_id) thay cho UNIQUE riêng từng cột
-- [SỬA LỖI] match_results: UNIQUE(resume_id, job_id, model_version_id)
-- [SỬA LỖI] recommendation_items: UNIQUE(batch_id, rank_no); bỏ UNIQUE trên batch_id
-- [SỬA LỖI] model_versions: UNIQUE(model_name, version)
-- [SỬA LỖI] resume_extractions: UNIQUE(resume_id, model_version_id)
-- [SỬA LỖI] resumes: bỏ UNIQUE(candidate_id); thêm cột sinh default_candidate_id cho CV mặc định
-- [SỬA LỖI] users.email: thêm UNIQUE
-- [SỬA LỖI] FK ghép (resume_id, candidate_id) ở applications và recommendation_batches
-- [MỚI] candidate_profiles, resume_skills
-- [MỚI] account_tokens: token xác thực email và đặt lại mật khẩu, dùng một lần
-- [SỬA ĐỔI] users: thêm failed_login_count, locked_until để khóa tạm thời khi đăng nhập sai
-- [SỬA ĐỔI] refresh_tokens: thêm replaced_by để truy vết chuỗi xoay vòng token
-- [SỬA ĐỔI] Các cột trạng thái, loại, vai trò chuyển sang ENUM; thêm CHECK cho lương, điểm, thứ hạng, số lần thử
-- [SỬA ĐỔI] system_settings.key -> setting_key; reports có UNIQUE chống báo cáo trùng
-- [SỬA ĐỔI] Quan hệ 1-1 (recruiter_profiles, candidate_preferences, notification_preferences) được coi là 1-1
-- [SỬA ĐỔI] Thêm chỉ mục cho các truy vấn chính (lọc job, lịch sử ứng tuyển, thông báo chưa đọc, audit)
-- =============================================================
