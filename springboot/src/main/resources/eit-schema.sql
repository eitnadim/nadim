-- ============================================================
-- EIT Framework v2.5  —  Complete PostgreSQL Schema
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Roles ───────────────────────────────────────────────────
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='anon') THEN CREATE ROLE anon NOLOGIN; END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='authenticated') THEN CREATE ROLE authenticated NOLOGIN; END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='postgrest_user') THEN CREATE ROLE postgrest_user LOGIN PASSWORD 'postgrest_pass'; END IF;
END $$;
GRANT anon, authenticated TO postgrest_user;

-- ── updated_at trigger function ──────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

-- ── Users ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ── Projects ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS projects (
    id                   UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id             UUID        REFERENCES users(id) ON DELETE SET NULL,
    name                 VARCHAR(200) NOT NULL,
    description          TEXT,
    java_version         VARCHAR(20)  NOT NULL DEFAULT 'Java 17',
    build_tool           VARCHAR(20)  NOT NULL DEFAULT 'Maven',
    group_id             VARCHAR(200) NOT NULL,
    artifact_id          VARCHAR(200) NOT NULL,
    package_name         VARCHAR(300),
    spring_boot_version  VARCHAR(20)  NOT NULL DEFAULT '3.2.x',
    git_invite_email     VARCHAR(255),
    git_repo_url         VARCHAR(500),
    git_repo_name        VARCHAR(300),
    status               VARCHAR(30)  NOT NULL DEFAULT 'IDLE',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_projects_owner  ON projects(owner_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
DROP TRIGGER IF EXISTS trg_projects_updated_at ON projects;
CREATE TRIGGER trg_projects_updated_at BEFORE UPDATE ON projects FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ── fw_configs  (one table for ALL JSON config types) ────────
CREATE TABLE IF NOT EXISTS fw_configs (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id    UUID        REFERENCES projects(id) ON DELETE CASCADE,
    alias_name    VARCHAR(200) NOT NULL,
    config_type   VARCHAR(50)  NOT NULL,  -- form_crud | grid | chart | custom_sql
    schema_name   VARCHAR(100),
    table_name    VARCHAR(200),
    display_type  VARCHAR(50),
    configuration JSONB        NOT NULL,  -- full JSON body
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_fw_configs_project     ON fw_configs(project_id);
CREATE INDEX IF NOT EXISTS idx_fw_configs_type        ON fw_configs(config_type);
CREATE INDEX IF NOT EXISTS idx_fw_configs_schema_table ON fw_configs(schema_name, table_name);
-- Allow querying inside the JSON body
CREATE INDEX IF NOT EXISTS idx_fw_configs_json        ON fw_configs USING GIN (configuration);
DROP TRIGGER IF EXISTS trg_fw_configs_updated_at ON fw_configs;
CREATE TRIGGER trg_fw_configs_updated_at BEFORE UPDATE ON fw_configs FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ── Custom classes ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS custom_classes (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_name   VARCHAR(255) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    class_name  VARCHAR(255),
    file_size   BIGINT,
    uploaded_by UUID        REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_custom_classes_project ON custom_classes(project_id);

-- ── Build logs ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS build_logs (
    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id   UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    triggered_by UUID        REFERENCES users(id),
    commit_hash  VARCHAR(100),
    branch       VARCHAR(200),
    status       VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    logs         TEXT,
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    finished_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_build_logs_project ON build_logs(project_id);

-- ── Deploy logs ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS deploy_logs (
    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id   UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    build_log_id UUID        REFERENCES build_logs(id),
    triggered_by UUID        REFERENCES users(id),
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    deploy_url   VARCHAR(500),
    logs         TEXT,
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    finished_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_deploy_logs_project ON deploy_logs(project_id);

-- ── Deploy approvals (3-level, any 1 accepts) ────────────────
CREATE TABLE IF NOT EXISTS deploy_approvals (
    id             UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    deploy_log_id  UUID        NOT NULL REFERENCES deploy_logs(id) ON DELETE CASCADE,
    approver_email VARCHAR(255) NOT NULL,
    approver_name  VARCHAR(120),
    level          SMALLINT    NOT NULL DEFAULT 1,  -- 1, 2, 3
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED
    token          VARCHAR(100) NOT NULL UNIQUE,  -- secure token in email link
    sent_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    responded_at   TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_approvals_deploy    ON deploy_approvals(deploy_log_id);
CREATE INDEX IF NOT EXISTS idx_approvals_token     ON deploy_approvals(token);

-- ── Git invitations ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS git_invitations (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id    UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    invited_email VARCHAR(255) NOT NULL,
    status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    sent_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    accepted_at   TIMESTAMPTZ,
    resent_count  SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_git_inv_project ON git_invitations(project_id);

-- ── Server configuration ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS server_configs (
    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id   UUID        REFERENCES projects(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    ip_address   VARCHAR(100) NOT NULL,
    port         INTEGER      NOT NULL DEFAULT 22,
    username     VARCHAR(100),
    env          VARCHAR(30)  NOT NULL DEFAULT 'production', -- dev | staging | production
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── S3 configuration ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS s3_configs (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id    UUID        REFERENCES projects(id) ON DELETE CASCADE,
    bucket_name   VARCHAR(200) NOT NULL,
    region        VARCHAR(50),
    base_url      VARCHAR(500),
    access_key    VARCHAR(200),
    secret_key    VARCHAR(500),
    path_prefix   VARCHAR(300),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Passito auth configuration ────────────────────────────────
CREATE TABLE IF NOT EXISTS passito_configs (
    id               UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id       UUID        REFERENCES projects(id) ON DELETE CASCADE,
    platform         VARCHAR(20)  NOT NULL, -- web | mobile
    client_id        VARCHAR(300),
    client_secret    VARCHAR(500),
    redirect_uri     VARCHAR(500),
    scopes           VARCHAR(500),
    bundle_id        VARCHAR(300),  -- mobile only
    deep_link_scheme VARCHAR(200),  -- mobile only
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Grants ───────────────────────────────────────────────────
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON users TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON
    projects, fw_configs, custom_classes, build_logs, deploy_logs,
    deploy_approvals, git_invitations, server_configs, s3_configs, passito_configs
TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgrest_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO postgrest_user;

-- ── Seed admin user (password = admin123) ────────────────────
INSERT INTO users (name, email, password_hash, role)
VALUES ('EIT Admin', 'admin@eit.dev',
        '$2a$10$YnPRMrYTf07bRJYZYDjRpuPZi6sPF/tFjO/03gTKeuBFAZqHRS9mW', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
