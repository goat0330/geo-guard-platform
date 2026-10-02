-- Additional table definitions and additive task-chain columns from local business migrations.
-- No business records, private deployment metadata, or destructive statements.

CREATE TABLE data_geological_fold (
    id BIGSERIAL PRIMARY KEY,
    wkt TEXT,
    name VARCHAR(200) NOT NULL,
    fold_summary TEXT,
    length VARCHAR(100),
    width VARCHAR(100),
    geom GEOMETRY(MULTILINESTRING, 4326)
);

CREATE TABLE IF NOT EXISTS data_geological_fault (
    id BIGSERIAL PRIMARY KEY,
    wkt TEXT NOT NULL,
    name VARCHAR(255),
    strike VARCHAR(50),
    dip_direction VARCHAR(50),
    dip_angle VARCHAR(50),
    length VARCHAR(50),
    width VARCHAR(50),
    feature TEXT,
    property VARCHAR(255),
    geom GEOMETRY(MultiLineString, 4326)
    );

CREATE TABLE IF NOT EXISTS dz_auto_mode_execution_trace (
    id BIGINT PRIMARY KEY,
    action_type VARCHAR(64) NOT NULL,
    action_name VARCHAR(100) NOT NULL,
    biz_type INTEGER,
    biz_id BIGINT,
    task_id BIGINT,
    batch_flag BOOLEAN NOT NULL DEFAULT FALSE,
    execute_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    ended_at TIMESTAMP(6),
    failure_reason VARCHAR(1000),
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dz_auto_mode_minute_summary (
    id BIGINT PRIMARY KEY,
    window_start TIMESTAMP(6) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    action_name VARCHAR(100) NOT NULL,
    execute_count INTEGER NOT NULL DEFAULT 0,
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dz_auto_mode_ai_hosting_record (
    id BIGINT PRIMARY KEY,
    record_name VARCHAR(100) NOT NULL,
    opened_at TIMESTAMP(6) NOT NULL,
    closed_at TIMESTAMP(6),
    duration_minutes INTEGER NOT NULL DEFAULT 0,
    duration_text VARCHAR(32) NOT NULL DEFAULT '0分',
    open_user_id BIGINT,
    open_user_name VARCHAR(100),
    open_user_role VARCHAR(255),
    close_user_id BIGINT,
    close_user_name VARCHAR(100),
    close_user_role VARCHAR(255),
    create_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dz_task_process_chain_summary
(
    id                       BIGSERIAL PRIMARY KEY,
    node_id                  BIGINT,
    chain_id                 VARCHAR(64),
    parent_chain_id          VARCHAR(64),
    parent_node_id           BIGINT,
    link_name                VARCHAR(100),
    trigger_reason           VARCHAR(500),
    operator_id              BIGINT,
    operator_name            VARCHAR(100),
    operator_role            VARCHAR(100),
    trigger_time             TIMESTAMP,
    biz_type                 INTEGER,
    biz_id                   BIGINT,
    task_id                  BIGINT,
    source_type              INTEGER,
    node_category            INTEGER,
    stage_type               INTEGER,
    node_create_date         TIMESTAMP,
    node_update_date         TIMESTAMP,
    display_biz_type         INTEGER NOT NULL,
    display_biz_id           BIGINT  NOT NULL,
    root_chain_id            VARCHAR(64),
    root_biz_type            INTEGER,
    root_biz_id              BIGINT,
    root_source_type         INTEGER,
    chain_segment_type       INTEGER,
    current_status           INTEGER,
    current_status_label     VARCHAR(32),
    risk_level               INTEGER,
    risk_level_source        INTEGER,
    unit_id                  VARCHAR(64),
    province                 VARCHAR(255),
    city                     VARCHAR(255),
    county                   VARCHAR(255),
    street                   VARCHAR(255),
    village                  VARCHAR(255),
    province_code            VARCHAR(255),
    city_code                VARCHAR(255),
    county_code              VARCHAR(255),
    street_code              VARCHAR(255),
    village_code             VARCHAR(255),
    pilot_area_1             INTEGER,
    pilot_area_2             INTEGER,
    responsible_person       VARCHAR(500),
    responsible_person_phone VARCHAR(500),
    status                   INTEGER,
    task_source_type         INTEGER,
    dynamic_risk_level       INTEGER,
    scene_photo              TEXT,
    inspecting_require       TEXT,
    text_record              TEXT,
    def_id                   BIGINT,
    handle_id                BIGINT,
    report_id                BIGINT,
    risk_id                  BIGINT,
    plan_type                INTEGER,
    deleted                  SMALLINT  NOT NULL DEFAULT 0,
    create_date              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.data_short_term_temporary_rainfall_observation (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    device_id varchar(64) NOT NULL,
    log_time timestamp(0) without time zone NOT NULL,
    rainfall double precision NOT NULL,
    cumulative_rainfall double precision NOT NULL,
    create_time timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_short_term_temporary_rainfall_observation UNIQUE (device_id, log_time),
    CONSTRAINT ck_short_term_temporary_observation_rainfall_nonnegative CHECK (rainfall >= 0)
);

CREATE TABLE IF NOT EXISTS public.data_short_term_temporary_rainfall_forecast (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    device_id varchar(64) NOT NULL,
    issue_time timestamp(0) without time zone NOT NULL,
    forecast_time timestamp(0) without time zone NOT NULL,
    period_hours integer NOT NULL,
    rainfall double precision NOT NULL,
    create_time timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_short_term_temporary_rainfall_forecast UNIQUE (device_id, issue_time, period_hours),
    CONSTRAINT ck_short_term_temporary_forecast_period CHECK (period_hours IN (1, 12, 24)),
    CONSTRAINT ck_short_term_temporary_forecast_rainfall_nonnegative CHECK (rainfall >= 0)
);

ALTER TABLE dz_task_process_chain_node
    ADD COLUMN IF NOT EXISTS parent_chain_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS root_chain_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS display_biz_type INTEGER,
    ADD COLUMN IF NOT EXISTS display_biz_id BIGINT,
    ADD COLUMN IF NOT EXISTS chain_segment_type INTEGER,
    ADD COLUMN IF NOT EXISTS root_biz_type INTEGER,
    ADD COLUMN IF NOT EXISTS root_biz_id BIGINT,
    ADD COLUMN IF NOT EXISTS root_source_type INTEGER;

CREATE INDEX idx_data_geological_fold_geom ON data_geological_fold USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_data_geological_fault_geom
    ON data_geological_fault USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_trace_started
    ON dz_auto_mode_execution_trace (started_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_trace_ended
    ON dz_auto_mode_execution_trace (ended_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_trace_action_status
    ON dz_auto_mode_execution_trace (action_type, status, ended_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dz_auto_mode_minute_summary_window_action
    ON dz_auto_mode_minute_summary (window_start, action_type);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_minute_summary_window
    ON dz_auto_mode_minute_summary (window_start DESC, id DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dz_auto_mode_ai_hosting_record_name
    ON dz_auto_mode_ai_hosting_record (record_name);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_ai_hosting_opened
    ON dz_auto_mode_ai_hosting_record (opened_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_ai_hosting_closed
    ON dz_auto_mode_ai_hosting_record (closed_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_dz_auto_mode_ai_hosting_running
    ON dz_auto_mode_ai_hosting_record (closed_at)
    WHERE closed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_node_root_chain
    ON dz_task_process_chain_node (root_chain_id, create_date DESC, id DESC)
    WHERE deleted = 0 AND root_chain_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_node_display_biz
    ON dz_task_process_chain_node (display_biz_type, display_biz_id, create_date DESC, id DESC)
    WHERE deleted = 0 AND display_biz_type IS NOT NULL AND display_biz_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_node_segment
    ON dz_task_process_chain_node (chain_segment_type, create_date DESC, id DESC)
    WHERE deleted = 0 AND chain_segment_type IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_task_process_chain_summary_display
    ON dz_task_process_chain_summary (display_biz_type, display_biz_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_summary_segment_update
    ON dz_task_process_chain_summary (chain_segment_type, update_date DESC, node_id DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_summary_risk
    ON dz_task_process_chain_summary (risk_level, update_date DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_summary_status
    ON dz_task_process_chain_summary (current_status, update_date DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_summary_unit
    ON dz_task_process_chain_summary (unit_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_summary_area
    ON dz_task_process_chain_summary (county_code, street_code, village_code)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_task_process_chain_summary_root_chain
    ON dz_task_process_chain_summary (root_chain_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_short_term_temporary_observation_log_time
    ON public.data_short_term_temporary_rainfall_observation (log_time, device_id);

CREATE INDEX IF NOT EXISTS idx_short_term_temporary_observation_device_latest
    ON public.data_short_term_temporary_rainfall_observation (device_id, log_time DESC);

CREATE INDEX IF NOT EXISTS idx_short_term_temporary_forecast_target_time
    ON public.data_short_term_temporary_rainfall_forecast (forecast_time, issue_time DESC, device_id);
