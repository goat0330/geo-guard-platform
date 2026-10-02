-- Local empty business schema. No users, credentials, or business records.
-- PostgreSQL with PostGIS is required. Run once against an empty project database.
CREATE EXTENSION IF NOT EXISTS postgis;

-- Required by the original bigint nextval defaults. This is an empty database,
-- so each counter starts at 1; existing business sequence values are not copied.
CREATE SEQUENCE public.data_expert_expert_id_seq;
CREATE SEQUENCE public.data_rainfall_entity_grid_mapping_id_seq;
CREATE SEQUENCE public.data_tp_device_curve_hourly_id_seq;
CREATE SEQUENCE public.data_tp_device_status_current_id_seq;
CREATE SEQUENCE public.data_tp_warning_event_id_seq;
CREATE SEQUENCE public.dz_def_resp_start_sms_config_id_seq;
CREATE SEQUENCE public.dz_risk_assessment_id_seq;
CREATE SEQUENCE public.data_geo_disaster_prediction_id_seq;
CREATE SEQUENCE public.dz_risk_assessment_warning_relation_id_seq;
CREATE SEQUENCE public.dz_sms_send_batch_id_seq;
CREATE SEQUENCE public.dz_task_process_chain_node_id_seq;

CREATE TABLE public.ai_chat_history (
    id character varying(128),
    user_id bigint,
    type character varying(128),
    title character varying(512),
    create_date timestamp(6) without time zone,
    update_date timestamp(6) without time zone,
    delete_flag integer DEFAULT 0,
    pinned_at integer DEFAULT 0
);

CREATE TABLE public.ai_chat_history_detail (
    id character varying(128),
    user_id bigint,
    history_id character varying(128),
    inputs json,
    query text,
    answer text,
    message_metadata json,
    create_date timestamp(6) without time zone,
    files jsonb
);

CREATE TABLE public.ai_chat_history_detail_feedbacks (
    id character varying(128),
    user_id bigint,
    history_id character varying(128),
    history_detail_id character varying(128),
    rating integer,
    content text,
    create_date timestamp(6) without time zone,
    update_date timestamp(6) without time zone
);

CREATE TABLE public.data_ad_region (
    id character varying(64) NOT NULL,
    pcode character varying(64),
    name character varying(255),
    level smallint,
    level_name character varying(255),
    province character varying(255),
    city character varying(255),
    county character varying(255),
    street character varying(255),
    village character varying(255),
    community character varying(255),
    center character varying(255),
    wkt text,
    area numeric(20,4),
    geom geometry(Geometry,4490),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_alarm (
    id bigint NOT NULL,
    code character varying(255),
    city character varying(255),
    status smallint NOT NULL DEFAULT 0,
    create_date timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source character varying(255) NOT NULL,
    message text,
    time date,
    publish_date timestamp without time zone,
    valid_end_date timestamp without time zone,
    key_tips character varying(500),
    pending_status integer DEFAULT 0,
    update_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id character varying(255),
    source_type integer,
    valid_start_date timestamp without time zone,
    level_streets_json text,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_engineering_geology (
    wkt text,
    rock_group character varying(50) NOT NULL,
    characteristics character varying(500),
    project_name character varying(100) NOT NULL,
    geom geometry(MultiPolygon,4490)
);

CREATE TABLE public.data_expert (
    expert_id bigint NOT NULL DEFAULT nextval('data_expert_expert_id_seq'::regclass),
    source_id character varying(64) NOT NULL,
    account character varying(64),
    expert_name character varying(100) NOT NULL,
    gender character varying(16),
    nation character varying(32),
    birth_date date,
    id_card_no character varying(32),
    native_place character varying(100),
    education character varying(64),
    graduation_date date,
    graduation_school character varying(255),
    major character varying(255),
    degree character varying(64),
    organization_name character varying(255),
    mailing_address character varying(500),
    phone character varying(32),
    email character varying(128),
    professional_title_level character varying(64),
    professional_title character varying(128),
    title_obtained_date date,
    position_name character varying(128),
    speciality_direction character varying(255),
    specialty_tech_desc character varying(1000),
    declared_major_direction character varying(1000),
    remark character varying(1000),
    admin_region_change_flag character varying(32),
    expert_status character varying(32),
    old_source_id character varying(64),
    business_id character varying(64),
    file_id character varying(64),
    is_old smallint,
    avatar_url character varying(1000),
    review_opinion character varying(1000),
    row_num integer,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    del_flag character(1) DEFAULT '0'::bpchar,
    PRIMARY KEY (expert_id)
);

CREATE TABLE public.data_geological_hazard (
    id character varying(64) NOT NULL,
    geological_environment text,
    stratum_code character varying(20),
    township character varying(50),
    is_hazard_point smallint,
    danger_level character varying(10),
    monitor_method character varying(100),
    control_finish_time timestamp without time zone,
    threat_object character varying(500),
    stable_trend character varying(10),
    disaster_time character varying(50),
    control_suggest character varying(30),
    hazard_cancel_time timestamp without time zone,
    is_cancel smallint,
    create_time timestamp without time zone,
    report_date timestamp without time zone,
    is_monitor_point smallint,
    area numeric(12,2),
    grid_code character varying(20),
    x_coordinate numeric(15,2),
    width numeric(12,2),
    inspector_phone character varying(20),
    grid_group character varying(20),
    hazard_confirm_time timestamp without time zone,
    monitor_build_time timestamp without time zone,
    other_induce_cause text,
    city character varying(20),
    other_stratum character varying(255),
    top_depth numeric(12,2),
    hazard_length numeric(12,2),
    inspector_name character varying(50),
    survey_times smallint,
    bottom_score numeric(12,2),
    longitude numeric(15,6),
    manage_level character varying(10),
    village character varying(50),
    top_meter numeric(12,2),
    status_flag smallint,
    latitude numeric(15,6),
    monitor_name character varying(50),
    hazard_name character varying(40),
    height numeric(12,2),
    move_finish_time timestamp without time zone,
    hazard_type character varying(10),
    national_code character varying(50),
    cancel_reason text,
    scale_level character varying(10),
    hazard_serial_no character varying(40),
    induce_cause text,
    monitor_phone character varying(20),
    y_coordinate numeric(18,2),
    ownership_type character varying(10),
    stable_analysis text,
    hazard_code character varying(25),
    is_professional_monitor smallint,
    county character varying(10),
    risk_level character varying(10),
    monitor_user_id character varying(64),
    main_feature text,
    update_time timestamp without time zone,
    longitude_degree numeric(12,2),
    province character varying(20),
    longitude_minute numeric(12,2),
    threat_population integer,
    deform_feature text,
    longitude_meter numeric(12,2),
    create_by character varying(20),
    length numeric(12,2),
    is_project_control smallint,
    remark text,
    major_hazard_code character varying(50),
    stable_status character varying(10),
    audit_status character varying(10),
    is_evacuation smallint,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_geological_structure (
    id character varying(32) NOT NULL,
    wkt text NOT NULL,
    geom geometry(MultiLineString,4490),
    name character varying(255),
    trend character varying(50),
    dip_direction character varying(50),
    dip_angle character varying(50),
    length text,
    width text,
    feature text,
    nature character varying(100),
    fold_summary text,
    layer character varying(255),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_geomorphological (
    id bigint NOT NULL,
    wkt text NOT NULL,
    type character varying(100) NOT NULL,
    feature text,
    geom geometry(Geometry,4490),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_hazard_point (
    id character varying(50),
    name character varying(100),
    type_code character varying(50),
    grid_code character varying(50),
    unique_disaster_id character varying(50),
    province character varying(50),
    city character varying(50),
    county character varying(50),
    street character varying(50),
    village character varying(50),
    grid_group character varying(50),
    x_coordinate double precision,
    y_coordinate double precision,
    longitude double precision,
    latitude double precision,
    length_m double precision,
    width_m double precision,
    height_m double precision,
    area_sqm double precision,
    volume_cbm double precision,
    scale_grade character varying(50),
    management_level character varying(50),
    threatened_population integer,
    threatened_property_value double precision,
    risk_grade character varying(50),
    disaster_history_time character varying(50),
    geological_environment text,
    deformation_features text,
    stability_analysis text,
    stability_status character varying(50),
    stability_trend character varying(50),
    trigger_factors text,
    potential_hazards text,
    pre_disaster_prediction text,
    monitoring_method text,
    monitoring_person_id character varying(50),
    report_date date,
    history_sn character varying(36),
    data_flag integer,
    is_cancelled integer,
    operation_type character varying(1),
    review_status character varying(1),
    created_by character varying(32),
    created_time timestamp(6) without time zone,
    has_survey_data integer,
    category character varying(2),
    geom geometry,
    slope_unit_id character varying(64),
    slope_unit_name character varying(255),
    pilot_area_1 integer,
    pilot_area_2 integer,
    province_code character varying(255),
    city_code character varying(255),
    county_code character varying(255),
    street_code character varying(255),
    village_code character varying(255),
    wkt text
);

CREATE TABLE public.data_house (
    id character varying(50) NOT NULL,
    house_unit_id character varying(100),
    building_code character varying(100) NOT NULL,
    building_name character varying(255) NOT NULL,
    province_code character varying(6),
    city_code character varying(6),
    county_code character varying(6),
    street_code character varying(20),
    community_code character varying(20),
    village_code character varying(20),
    grid_code character varying(20),
    street_address_code character varying(100),
    door_plate_number character varying(20),
    community_name character varying(50),
    building_number character varying(50),
    unit_number character varying(10),
    floor_number character varying(10),
    room_number character varying(10),
    longitude numeric(15,10) NOT NULL,
    latitude numeric(15,10) NOT NULL,
    create_time timestamp(6) without time zone,
    update_time timestamp(6) without time zone,
    geometry geometry(Geometry,4490),
    pilot_area_1 smallint,
    pilot_area_2 smallint,
    area numeric(20,4),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_house_cim (
    id character varying(50) NOT NULL,
    house_unit_id character varying(50) NOT NULL,
    building_code character varying(100) NOT NULL,
    building_name character varying(255) NOT NULL,
    province_code character varying(6) NOT NULL,
    city_code character varying(6) NOT NULL,
    county_code character varying(6) NOT NULL,
    street_code character varying(20) NOT NULL,
    community_code character varying(20) NOT NULL,
    village_code character varying(20),
    grid_code character varying(20),
    street_address_code character varying(100) NOT NULL,
    door_plate_number character varying(20),
    community_name character varying(50),
    building_number character varying(50),
    unit_number character varying(10),
    floor_number character varying(10),
    room_number character varying(10),
    longitude numeric(15,10) NOT NULL,
    latitude numeric(15,10) NOT NULL,
    create_time timestamp without time zone,
    update_time timestamp without time zone,
    geometry geometry,
    area numeric(20,2),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_hydro_geology (
    wkt text NOT NULL,
    aquifer_rock character varying(50) NOT NULL,
    aquifer_type character varying(50) NOT NULL,
    water_abundance character varying(20) NOT NULL,
    rock_group_era character varying(300),
    distribution_pos character varying(200),
    burial_conditions character varying(500),
    geom geometry(MultiPolygon,4490)
);

CREATE TABLE public.data_organization (
    id character varying(36) NOT NULL,
    organization_name character varying(255) NOT NULL,
    unified_social_credit_code character varying(64) NOT NULL,
    business_address character varying(255) NOT NULL,
    house_unit_id character varying(50) NOT NULL,
    building_code character varying(50) NOT NULL,
    building_name character varying(100) NOT NULL,
    province_code character varying(6) NOT NULL,
    city_code character varying(6) NOT NULL,
    county_code character varying(6) NOT NULL,
    street_code character varying(20) NOT NULL,
    community_code character varying(20) NOT NULL,
    village_code character varying(20),
    grid_code character varying(20),
    grid_name character varying(50),
    longitude numeric(15,10) NOT NULL,
    latitude numeric(15,10) NOT NULL,
    create_time timestamp without time zone,
    update_time timestamp without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_person (
    id character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    phone_number character varying(30) NOT NULL,
    gender character varying(3),
    birthday date,
    id_type character varying(3),
    id_number character varying(30),
    residence_address character varying(255),
    household_address character varying(255),
    residence_unified_social_credit_code character varying(30),
    residence_unified_address_name character varying(255),
    house_unit_id character varying(50),
    building_code character varying(50),
    province_code character varying(6),
    city_code character varying(6),
    county_code character varying(6),
    street_code character varying(20),
    community_code character varying(20),
    village_code character varying(20),
    grid_code character varying(20),
    grid_name character varying(50),
    community_address_code character varying(20),
    building_number character varying(50),
    unit_number character varying(10),
    floor_number character varying(10),
    house_number character varying(10),
    longitude numeric(15,10),
    latitude numeric(15,10),
    create_time timestamp(6) without time zone,
    update_time timestamp(6) without time zone,
    pilot_area_1 smallint,
    pilot_area_2 smallint,
    age integer,
    PRIMARY KEY (id)
);

CREATE TABLE "public"."data_rainfall_entity_grid_mapping" (
  "id" int8 NOT NULL DEFAULT nextval('data_rainfall_entity_grid_mapping_id_seq'::regclass),
  "entity_type" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "slope_unit_id" varchar(64) COLLATE "pg_catalog"."default",
  "ad_region_id" varchar(64) COLLATE "pg_catalog"."default",
  "ad_region_level" int4,
  "center" varchar(255) COLLATE "pg_catalog"."default",
  "center_lon" float8,
  "center_lat" float8,
  "grid_lon" float8,
  "grid_lat" float8,
  "province" varchar(255) COLLATE "pg_catalog"."default",
  "city" varchar(255) COLLATE "pg_catalog"."default",
  "county" varchar(255) COLLATE "pg_catalog"."default",
  "street" varchar(255) COLLATE "pg_catalog"."default",
  "village" varchar(255) COLLATE "pg_catalog"."default",
  "community" varchar(255) COLLATE "pg_catalog"."default",
  "province_code" varchar(32) COLLATE "pg_catalog"."default",
  "city_code" varchar(32) COLLATE "pg_catalog"."default",
  "county_code" varchar(32) COLLATE "pg_catalog"."default",
  "street_code" varchar(32) COLLATE "pg_catalog"."default",
  "village_code" varchar(32) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "forecast_grid_lon" float8,
  "forecast_grid_lat" float8
)
;

CREATE TABLE public.data_rainfall_forecast_raster_unit (
    id bigint NOT NULL,
    lon double precision NOT NULL,
    lat double precision NOT NULL,
    rainfall double precision NOT NULL,
    create_time timestamp(0) without time zone NOT NULL,
    forecast_time timestamp(0) without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_rainfall_log_raster_unit (
    id bigint NOT NULL,
    lon double precision NOT NULL,
    lat double precision NOT NULL,
    rainfall double precision NOT NULL,
    create_time timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    log_time timestamp(0) without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_risk_zone (
    wkt text,
    o_name character varying(255),
    o_com character varying(255),
    o_com1 character varying(255),
    o_com2 character varying(255),
    o_com3 character varying(255),
    o_com4 character varying(255),
    o_com5 character varying(255),
    o_com6 character varying(255),
    o_com7 character varying(255),
    o_com8 character varying(255),
    o_com9 character varying(255),
    o_com10 character varying(255),
    o_com11 character varying(255),
    o_lclr character varying(255),
    o_lwidth character varying(255),
    o_lalpha character varying(255),
    o_aalpha character varying(255),
    o_aclr character varying(255),
    o_flag character varying(255),
    slope_unit_id character varying(64),
    province character varying(255),
    city character varying(255),
    county character varying(255),
    street character varying(255),
    village character varying(255),
    community character varying(255),
    center character varying(255),
    area numeric(20,4),
    province_code character varying(255),
    city_code character varying(255),
    county_code character varying(255),
    street_code character varying(255),
    village_code character varying(255),
    id character varying(32) NOT NULL,
    geom geometry(Geometry,4490),
    pilot_area_1 integer,
    pilot_area_2 integer,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_road (
    road_name character varying(255),
    road_level character varying(50),
    wkt text NOT NULL,
    slope_unit_id character varying(64),
    id character varying(36)
);

CREATE TABLE public.data_road_geology (
    id character varying(50) NOT NULL,
    road_name character varying(200),
    wkt text NOT NULL,
    geom geometry(Geometry,4490),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_slope_geology (
    id character varying(64) NOT NULL,
    slope_unit_id character varying(64) NOT NULL,
    slope_type character varying(255),
    water_river_valley character varying(255),
    top_elevation numeric(10,2),
    bottom_elevation numeric(10,2),
    slope_length numeric(10,2),
    slope_shape character varying(255),
    landform_feature text,
    stratum_lithology text,
    structure_feature text,
    hydrogeology text,
    land_vegetation text,
    control_structural_plane text,
    stability_factors text,
    stability_current character varying(255),
    stability_trend character varying(255),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_slope_unit (
    id character varying(64) NOT NULL,
    name character varying(255),
    pilot_area_1 integer,
    pilot_area_2 integer,
    urban_area integer,
    province character varying(255),
    city character varying(255),
    county character varying(255),
    street character varying(255),
    village character varying(255),
    community character varying(255),
    center character varying(255),
    wkt text,
    area numeric(20,4),
    province_code character varying(255),
    city_code character varying(255),
    county_code character varying(255),
    street_code character varying(255),
    village_code character varying(255),
    geom geometry(Geometry,4326),
    wkt_3d text,
    detailed_address text,
    slope_angle character varying(255),
    slope_direction character varying(255),
    field_code character varying(64),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_slope_unit_grid_member_relation (
    id bigint NOT NULL,
    unit_id character varying(50) NOT NULL,
    province character varying(50),
    city character varying(50),
    county character varying(50),
    street character varying(100),
    village character varying(100),
    charge_mayor character varying(30),
    charge_mayor_phone character varying(50),
    charge_person character varying(30),
    charge_person_phone character varying(50),
    responsible_person character varying(30),
    responsible_person_phone character varying(50),
    admin_user character varying(30),
    admin_user_phone character varying(50),
    special_manager character varying(30),
    special_manager_phone character varying(50),
    assistant_manager character varying(30),
    assistant_manager_phone character varying(50),
    inspector character varying(30),
    inspector_phone character varying(50),
    monitor character varying(30),
    monitor_phone character varying(50),
    create_time timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted smallint NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

create table data_slope_unit_land_planning_stat
(
    id varchar(64) not null primary key,
    slope_unit_id varchar(64) not null,
    land_name text not null,
    planning_count integer not null default 0,
    intersection_area double precision not null default 0,
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now()
);

CREATE TABLE public.data_stratum (
    wkt text,
    code character varying(50),
    label character varying(100),
    stratum_era character varying(20),
    stratum_system character varying(20),
    stratum_series character varying(20),
    stratum_member character varying(50),
    description text,
    stratum_group character varying(50),
    lithology text,
    geom geometry(MultiPolygon,4490)
);

CREATE TABLE IF NOT EXISTS data_territorial_spatial_planning (
                                                                 id VARCHAR(64) PRIMARY KEY,
    wkt TEXT,
    geom geometry(Geometry, 4490),
    land_category_name TEXT,
    land_type_name TEXT
    );

CREATE TABLE public.data_tp_device_curve_hourly (
    id bigint NOT NULL DEFAULT nextval('data_tp_device_curve_hourly_id_seq'::regclass),
    client_id character varying(64) NOT NULL,
    monitor_point_id character varying(64),
    monitor_type character varying(32) NOT NULL,
    point_time timestamp(0) without time zone,
    source_update_time timestamp(0) without time zone,
    create_date timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    curve_columns_json text,
    point_values_json text,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_tp_device_status_current (
    id bigint NOT NULL DEFAULT nextval('data_tp_device_status_current_id_seq'::regclass),
    device_id character varying(64) NOT NULL,
    client_id character varying(64),
    monitor_point_id character varying(64),
    region_code character varying(12),
    status_code integer NOT NULL,
    status_name character varying(32),
    last_online_time timestamp without time zone,
    source_update_time timestamp without time zone,
    create_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE public.data_tp_warning_event (
    id bigint NOT NULL DEFAULT nextval('data_tp_warning_event_id_seq'::regclass),
    warning_id character varying(64) NOT NULL,
    device_id character varying(64),
    client_id character varying(64),
    monitor_point_id character varying(64),
    monitor_point_name character varying(255),
    warning_level character varying(8),
    warning_time timestamp without time zone,
    disposal_status integer,
    disposal_type character varying(32),
    disposal_time timestamp without time zone,
    disposal_person character varying(64),
    valid_warning integer,
    source_update_time timestamp without time zone,
    create_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    warning_device_name character varying(255),
    PRIMARY KEY (id)
);

CREATE TABLE public.data_water_system_geology (
    id character varying(50) NOT NULL,
    water_system_name character varying(200),
    wkt text NOT NULL,
    geom geometry(Geometry,4490),
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_camera_info (
    id bigint
);

CREATE TABLE public.dz_def_resp_alarm_history (
    id bigint NOT NULL,
    alarm_id bigint,
    def_id bigint,
    alarm_code character varying(255),
    trigger_time timestamp(6) without time zone,
    def_resp_level integer,
    alarm_level integer,
    alarm_source character varying(500),
    alarm_source_type integer,
    create_date timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_def_resp_consultation_confirm (
    id bigint NOT NULL,
    def_id bigint,
    round_no integer NOT NULL DEFAULT 1,
    status integer NOT NULL DEFAULT 0,
    confirm_items_json text NOT NULL,
    report_content text NOT NULL,
    create_by bigint,
    create_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date timestamp without time zone,
    deleted integer NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_def_resp_plan (
    id bigint NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    type smallint NOT NULL DEFAULT 0,
    status smallint NOT NULL DEFAULT 0,
    handle_id bigint,
    reg_id bigint,
    center character varying(255),
    trigger_condition character varying(512),
    county character varying(255) NOT NULL,
    streets character varying(255) NOT NULL,
    responsibility_unit character varying(128) NOT NULL,
    responsible_person character varying(64),
    level smallint NOT NULL,
    create_date timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date timestamp(0) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responsible_person_phone character varying(255),
    deleted smallint NOT NULL,
    task_publish_total bigint NOT NULL DEFAULT 0,
    task_complete_total bigint NOT NULL DEFAULT 0,
    region_scope_type smallint,
    parent_def_id bigint,
    execute_status smallint,
    close_time timestamp(0) without time zone,
    close_reason character varying(512),
    source_alarm_id bigint,
    current_round_no integer NOT NULL DEFAULT 1,
    approval_status integer DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_def_resp_start_sms_config (
    id bigint NOT NULL DEFAULT nextval('dz_def_resp_start_sms_config_id_seq'::regclass),
    biz_key character varying(64) NOT NULL,
    biz_name character varying(64) NOT NULL,
    role_keys text NOT NULL,
    sms_template text NOT NULL,
    status smallint NOT NULL DEFAULT 1,
    sort integer NOT NULL DEFAULT 0,
    remark character varying(500),
    create_by bigint DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by bigint DEFAULT 0,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    del_flag character(1) NOT NULL DEFAULT '0'::bpchar,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_evacuation_plan (
    id bigint NOT NULL,
    handle_id bigint,
    evacuation_area character varying(255),
    resettlement_location character varying(255),
    distance_km numeric(16,6),
    estimated_time_minutes integer
        CONSTRAINT ck_dz_evacuation_plan_estimated_time_nonnegative CHECK (estimated_time_minutes >= 0),
    evacuation_route character varying(255),
    evacuation_road geometry(Geometry,4490),
    create_time timestamp without time zone,
    evacuation_area_wkt text,
    resettlement_wkt text,
    evacuation_area_to_road geometry(Geometry,4490),
    road_to_resettlement geometry(Geometry,4490),
    resettlement_point text,
    residents_info text,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_expert_ext (
    id bigint,
    user_id bigint,
    expert_type integer,
    introduction character varying(1024),
    meeting_count integer DEFAULT 0
);

CREATE TABLE public.dz_msg_notice (
    id bigint,
    title character varying(255),
    content text,
    type character varying(64),
    status integer,
    biz_data text,
    create_date timestamp without time zone,
    update_date timestamp without time zone,
    user_id bigint
);

CREATE TABLE public.dz_oper_log (
    id bigint,
    server_name character varying(128),
    type integer,
    status integer,
    info text,
    create_date timestamp without time zone NOT NULL
);

CREATE TABLE public.dz_prediction_batch_info (
    id bigint NOT NULL,
    prediction_date_str character varying(64) NOT NULL,
    prediction_type integer NOT NULL,
    rain_periods_count integer DEFAULT 0,
    total_precipitation numeric(10,2),
    max_daily_rainfall numeric(10,2),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    remark text,
    precip_definition text,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_process_progress (
    id bigint NOT NULL,
    def_id bigint,
    status smallint,
    create_date timestamp(0) without time zone,
    round_no integer NOT NULL DEFAULT 1,
    action_type smallint NOT NULL DEFAULT 1,
    description character varying(1000),
    PRIMARY KEY (id)
);

CREATE TABLE "public"."dz_report_disaster" (
  "id" int8,
  "user_id" int8,
  "user_name" varchar(255) COLLATE "pg_catalog"."default",
  "user_phone" varchar(128) COLLATE "pg_catalog"."default",
  "user_role" varchar(128) COLLATE "pg_catalog"."default",
  "check_time" timestamp(6),
  "check_center" varchar(255) COLLATE "pg_catalog"."default",
  "check_center_location" varchar(255) COLLATE "pg_catalog"."default",
  "photos" varchar(512) COLLATE "pg_catalog"."default",
  "scene_text_record" varchar(1024) COLLATE "pg_catalog"."default",
  "ai_risk_level" int4,
  "ai_risk_label" varchar(255) COLLATE "pg_catalog"."default",
  "ai_report_detail" varchar(1024) COLLATE "pg_catalog"."default",
  "status" int4,
  "source_type" int4,
  "create_date" timestamp(6),
  "update_date" timestamp(6),
  "ai_vision_props" jsonb,
  "manual_risk_level" int4,
  "manual_risk_remark" varchar(500) COLLATE "pg_catalog"."default",
  "detailed_address" varchar(255) COLLATE "pg_catalog"."default",
  "task_id" int8
)
;

CREATE TABLE public.dz_resettlement_info (
    id bigint NOT NULL,
    name character varying(255),
    lon numeric(16,10),
    lat numeric(16,10),
    create_time timestamp(6) without time zone,
    wkt text,
    village character varying(255),
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_risk_assessment (
    id bigint DEFAULT nextval('dz_risk_assessment_id_seq'::regclass),
    batch_id bigint,
    slope_unit_id character varying(255),
    susceptibility numeric(10,6),
    hazard numeric(10,6),
    vulnerability numeric(10,6),
    risk numeric(10,6),
    dynamic_risk_level integer,
    create_date timestamp(0) without time zone,
    dynamic_risk_value numeric(10,6),
    susceptibility_level integer,
    hazard_level integer,
    vulnerability_level integer,
    risk_level integer,
    dynamic_risk_suggest character varying(1024),
    tem_dynamic_risk_level integer
);

CREATE TABLE public.dz_risk_assessment_c2 (
    slope_unit_id character varying(255),
    dynamic_risk_suggest character varying(1024),
    hazard_level integer,
    create_date timestamp(0) without time zone,
    susceptibility_level integer,
    vulnerability_level integer,
    dynamic_risk_value numeric(10,6),
    id bigint DEFAULT nextval('dz_risk_assessment_id_seq'::regclass),
    batch_id bigint,
    vulnerability numeric(10,6),
    hazard numeric(10,6),
    risk numeric(10,6),
    dynamic_risk_level integer,
    susceptibility numeric(10,6),
    risk_level integer
);

CREATE TABLE public.dz_risk_assessment_steps (
    id bigint NOT NULL DEFAULT nextval('data_geo_disaster_prediction_id_seq'::regclass),
    original_object_id character varying(20),
    grid_code integer,
    area_size double precision,
    shape_length double precision,
    description text,
    mp_area double precision,
    mp_perimeter double precision,
    point_no integer,
    stability character varying(50),
    structure_type_sub character varying(50),
    slope_name character varying(100),
    shape_length_1 double precision,
    structure_val integer,
    landslide_prone integer,
    landslide_prone_py integer,
    shape_length_2 double precision,
    shape_area double precision,
    slope_structure character varying(50),
    elevation_mean double precision,
    elevation_max integer,
    elevation_min integer,
    elevation_diff integer,
    slope_mean character varying(20),
    aspect_mean double precision,
    plan_curvature double precision,
    profile_curvature double precision,
    lithology_desc text,
    susceptibility_val integer,
    disaster_points integer[],
    slope_morphology character varying(50),
    unit_morphology character varying(50),
    vegetation_cover character varying(50),
    adjacent_water character varying(50),
    tectonic_dist character varying(50),
    slope_structure_code integer,
    shuoming_code integer,
    susceptibility_code integer,
    slope_morph_code integer,
    unit_morph_code integer,
    vegetation_cover_code integer,
    adjacent_to_water_code integer,
    structure_code integer,
    assessment_id bigint,
    pop_count double precision,
    econ_sum double precision,
    road_density double precision,
    building_density double precision,
    pop_density double precision,
    rainfall_prob double precision,
    rainfall_past7 double precision,
    rainfall_forecast double precision,
    rainfall_past7_daily jsonb,
    combined_time_prob double precision,
    vulnerability_display_json jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_risk_assessment_warning_relation (
    id bigint NOT NULL DEFAULT nextval('dz_risk_assessment_warning_relation_id_seq'::regclass),
    warning_event_id bigint NOT NULL,
    risk_assessment_id bigint NOT NULL,
    slope_unit_id character varying(64) NOT NULL,
    last_disposal_type character varying(64),
    last_disposal_time timestamp without time zone,
    last_valid_warning integer,
    create_time timestamp without time zone,
    update_time timestamp without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_risk_prediction (
    id bigint,
    batch_id bigint,
    slope_unit_id character varying(64),
    prediction_score_w numeric(10,2),
    risk_level integer,
    create_date timestamp without time zone,
    type integer,
    prediction_date date,
    prediction_date_str character varying(64)
);

CREATE TABLE public.dz_risk_prediction_push (
    id bigint,
    prediction_batch_id bigint,
    title character varying(255),
    content text,
    type integer,
    status integer,
    person character varying(2048),
    person_count integer,
    create_date timestamp without time zone,
    doc_id bigint,
    responsible_person_id bigint,
    responsible_person_name character varying(255)
);

CREATE TABLE public.dz_risk_warning_record (
    id character varying(64) NOT NULL,
    warning_date timestamp(0) without time zone,
    warning_model_id integer,
    warning_model character varying(255),
    state_text character varying(500),
    step_info text,
    warning_time_limit smallint,
    result_display_mode character varying(100),
    analysis_time timestamp(0) without time zone,
    analyst_id character varying(64),
    analyst character varying(100),
    analysis_type character varying(100),
    status character varying(20),
    publisher_id character varying(64),
    publisher character varying(100),
    result_table_name character varying(255),
    forecast_words text,
    delete_status character varying(10),
    publish_object character varying(255),
    max_warning_level integer,
    max_warning_level_text character varying(100),
    is_initiate_approval character varying(10),
    publish_time timestamp(0) without time zone,
    is_area_statistics character varying(10),
    error_message text,
    is_analysis_completed character varying(10),
    is_latest_publish character varying(10),
    is_publish_service character varying(10),
    initiator_id character varying(64),
    initiator_name character varying(100),
    consultation_words text,
    initiate_time timestamp(0) without time zone,
    warning_start_time timestamp(0) without time zone,
    is_encrypted character varying(10),
    consultation text,
    update_time timestamp(0) without time zone,
    sms_template text,
    execution_progress smallint,
    execution_step_info text,
    sms_adjust_content text,
    sms_adjust_time timestamp(0) without time zone,
    sms_adjust_person character varying(100),
    consultation_initiator_id character varying(64),
    consultation_initiator_name character varying(100),
    consultation_initiate_time timestamp(0) without time zone,
    administrative_division character varying(255),
    level character varying(50),
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_sms_send_batch (
    id bigint NOT NULL DEFAULT nextval('dz_sms_send_batch_id_seq'::regclass),
    batch_id character varying(64) NOT NULL,
    biz_type smallint NOT NULL,
    biz_id bigint NOT NULL,
    sms_type character varying(64) NOT NULL,
    target_type character varying(32) NOT NULL,
    content_snapshot text,
    send_status character varying(16) NOT NULL,
    total_count integer NOT NULL DEFAULT 0,
    success_count integer NOT NULL DEFAULT 0,
    fail_count integer NOT NULL DEFAULT 0,
    request_time timestamp without time zone,
    finish_time timestamp without time zone,
    error_msg character varying(1000),
    create_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_sms_send_stat (
    id bigint NOT NULL,
    batch_id character varying(64) NOT NULL,
    create_date timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sms_type character varying(64),
    biz_type smallint NOT NULL,
    receiver_name character varying(100),
    receiver_phone character varying(32),
    sms_content text,
    send_status character varying(32),
    error_msg text,
    send_time timestamp without time zone,
    biz_id bigint NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE "public"."dz_task_dist_list" (
  "id" int8,
  "unit_id" varchar(255) COLLATE "pg_catalog"."default",
  "user_id" int8,
  "risk_id" int8,
  "submit_require" text COLLATE "pg_catalog"."default",
  "scene_photo" varchar(1024) COLLATE "pg_catalog"."default",
  "text_record" varchar(521) COLLATE "pg_catalog"."default",
  "status" int4,
  "create_date" timestamp(0),
  "update_date" timestamp(0),
  "check_time" timestamp(0),
  "check_center" varchar(128) COLLATE "pg_catalog"."default",
  "check_center_location" varchar(255) COLLATE "pg_catalog"."default",
  "submit_time" timestamp(0),
  "source_type" int4 DEFAULT 1,
  "report_id" int8,
  "handle_id" int8,
  "def_id" int8,
  "plan_name" varchar(255) COLLATE "pg_catalog"."default",
  "plan_type" int2,
  "responsible_person" varchar(64) COLLATE "pg_catalog"."default",
  "responsible_person_phone" varchar(255) COLLATE "pg_catalog"."default",
  "delete" int2 DEFAULT 0,
  "inspection_suggestion" varchar(500) COLLATE "pg_catalog"."default",
  "overdue" int2,
  "quota_consumed" int2 DEFAULT 0,
  "reminder_count" int4,
  "last_remind_time" timestamp(0),
  "inspection_suggestion_backup" varchar(500) COLLATE "pg_catalog"."default",
  "close_reason" varchar(512) COLLATE "pg_catalog"."default",
  "closed_time" timestamp(0),
  "remark" varchar(255) COLLATE "pg_catalog"."default",
  "sms_content" text COLLATE "pg_catalog"."default",
  "detailed_address" varchar(255) COLLATE "pg_catalog"."default",
  "report_info" text COLLATE "pg_catalog"."default",
  "task_source" varchar COLLATE "pg_catalog"."default",
  "related_task_id" int8,
  "task_type" varchar(32) COLLATE "pg_catalog"."default",
  "app_push_type" int2,
  "app_push_user_id" int8,
  "app_push_time" timestamp(0),
  "task_create_type" int2,
  "task_create_user_id" int8
)
;

CREATE TABLE public.dz_task_dist_list_add (
    task_id bigint NOT NULL,
    user_id bigint NOT NULL,
    remark text NOT NULL,
    create_date timestamp(0) without time zone,
    reporter_id bigint,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE public.dz_task_dist_list_history (
    id bigint NOT NULL,
    task_id bigint,
    text_record text,
    submit_status integer,
    scene_photo text,
    check_center character varying(128),
    check_center_location character varying(255),
    create_date timestamp(0) without time zone,
    update_date timestamp(0) without time zone,
    user_id bigint,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_task_dist_list_remark (
    id bigint NOT NULL,
    task_id bigint,
    remark character varying(255),
    create_date timestamp(0) without time zone,
    update_date timestamp(0) without time zone,
    user_id bigint,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_task_handle (
    id bigint NOT NULL,
    province character varying(255),
    city character varying(255),
    county character varying(255),
    street character varying(255),
    village character varying(255),
    center character varying(255),
    event_type integer,
    event_level integer,
    resp_status integer,
    handle_process integer,
    influence_scope character varying(255),
    responsible_person character varying(255),
    responsible_person_phone character varying(255),
    responsible_person_role character varying(255),
    reporter character varying(255),
    reporter_date timestamp(6) without time zone,
    slope_unit_id character varying(255),
    people_leave integer,
    create_date timestamp(6) without time zone,
    update_date timestamp(6) without time zone,
    is_skipped smallint,
    detailed_address character varying(255),
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_task_handle_approval (
    id bigint,
    handle_id bigint,
    user_id bigint,
    nickname character varying(128),
    type integer,
    process integer,
    status integer DEFAULT 0,
    create_date timestamp(0) without time zone,
    update_date timestamp(0) without time zone,
    meeting_type integer,
    round_no integer NOT NULL DEFAULT 1
);

CREATE TABLE public.dz_task_handle_detail_content (
    id bigint NOT NULL,
    biz_id bigint,
    biz_type integer NOT NULL,
    content_type integer NOT NULL,
    plan_content text,
    plan_content_json text,
    round_no integer DEFAULT 1,
    deleted integer DEFAULT 0,
    create_date timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    suggest character varying(500),
    user_id bigint,
    user_name character varying(255),
    is_latest smallint NOT NULL DEFAULT 0,
    status integer NOT NULL DEFAULT 0,
    update_date timestamp(6) without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.dz_task_handle_scene_record (
    id bigint NOT NULL,
    handle_id bigint,
    location character varying(255),
    disaster_type character varying(50),
    disaster_name character varying(255),
    check_in_coordinates character varying(128),
    occurrence_time timestamp without time zone,
    cause_type character varying(50),
    cause_remark character varying(255),
    threat_households integer DEFAULT 0,
    threat_people integer DEFAULT 0,
    threat_houses integer DEFAULT 0,
    other_threats character varying(255),
    direct_loss numeric(15,2) DEFAULT 0.00,
    casualties_desc integer DEFAULT 0,
    investigation_unit character varying(255),
    investigator character varying(50),
    investigation_date timestamp without time zone,
    lithology_desc text,
    structure_desc text,
    scale_length numeric(10,2),
    scale_width numeric(10,2),
    scale_depth numeric(10,2),
    scale_volume numeric(15,2),
    stability_status character varying(50),
    dev_trend character varying(50),
    special_characteristics json,
    measures_evacuation character varying(255),
    measures_protection character varying(255),
    measures_monitoring character varying(255),
    measures_hazard_removal character varying(255),
    measures_engineering character varying(255),
    measures_publicity character varying(255),
    measures_traffic_control character varying(255),
    measures_other_suggestions character varying(255),
    photo_panorama character varying(255),
    photo_deformation character varying(255),
    photo_damage character varying(255),
    create_date timestamp without time zone,
    update_date timestamp without time zone,
    disaster_coordinates character varying(128),
    disaster_nature character varying(50),
    threat_asset numeric(15,2) DEFAULT 0.00,
    indirect_loss numeric(15,2) DEFAULT 0.00,
    dead_people integer DEFAULT 0,
    other_danger character varying(500) DEFAULT '无'::character varying,
    slope_structure character varying(50),
    sliding_direction numeric(10,2),
    landside_morphology‌ character varying(255),
    measures_evacuation_remark character varying(255),
    measures_protection_remark character varying(255),
    measures_monitoring_remark character varying(255),
    measures_hazard_removal_remark character varying(255),
    measures_engineering_remark character varying(255),
    measures_publicity_remark character varying(255),
    measures_traffic_control_remark character varying(255),
    measures_other_suggestions_remark character varying(255),
    hazard_extent text,
    risk_extent text,
    PRIMARY KEY (id)
);

CREATE TABLE "public"."dz_task_process_chain_node" (
  "id" int8 NOT NULL DEFAULT nextval('dz_task_process_chain_node_id_seq'::regclass),
  "chain_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_node_id" int8,
  "link_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_reason" varchar(500) COLLATE "pg_catalog"."default",
  "operator_id" int8,
  "operator_name" varchar(100) COLLATE "pg_catalog"."default",
  "trigger_time" timestamp(6),
  "biz_type" int4 NOT NULL,
  "biz_id" int8,
  "task_id" int8,
  "source_type" int4,
  "deleted" int2 NOT NULL DEFAULT 0,
  "create_date" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "update_date" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "operator_role" varchar(100) COLLATE "pg_catalog"."default",
  "node_category" int4,
  "stage_type" int4
)
;

CREATE TABLE public.dz_user_ad_region (
    user_id bigint NOT NULL,
    ad_region_id character varying(64) NOT NULL,
    ad_region_name character varying(100) NOT NULL,
    ad_region_level integer NOT NULL,
    create_time timestamp without time zone,
    update_time timestamp without time zone,
    del_flag character(1) NOT NULL DEFAULT '0'::bpchar
);

CREATE TABLE public.sys_app (
    id bigint,
    key character varying(125),
    name character varying(255)
);

CREATE TABLE public.sys_client (
    id bigint NOT NULL,
    client_id character varying(64) DEFAULT ''::character varying,
    client_key character varying(32) DEFAULT ''::character varying,
    client_secret character varying(255) DEFAULT ''::character varying,
    grant_type character varying(255) DEFAULT ''::character varying,
    device_type character varying(32) DEFAULT ''::character varying,
    active_timeout integer DEFAULT 1800,
    timeout integer DEFAULT 604800,
    status character(1) DEFAULT '0'::bpchar,
    del_flag character(1) DEFAULT '0'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.sys_config (
    config_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    config_name character varying(100) DEFAULT ''::character varying,
    config_key character varying(100) DEFAULT ''::character varying,
    config_value character varying(500) DEFAULT ''::character varying,
    config_type character(1) DEFAULT 'N'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT NULL::character varying,
    PRIMARY KEY (config_id)
);

CREATE TABLE public.sys_dept (
    dept_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    parent_id bigint DEFAULT 0,
    ancestors character varying(500) DEFAULT ''::character varying,
    dept_name character varying(30) DEFAULT ''::character varying,
    dept_category character varying(100) DEFAULT NULL::character varying,
    order_num integer DEFAULT 0,
    leader bigint,
    phone character varying(11) DEFAULT NULL::character varying,
    email character varying(50) DEFAULT NULL::character varying,
    status character(1) DEFAULT '0'::bpchar,
    del_flag character(1) DEFAULT '0'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    PRIMARY KEY (dept_id)
);

CREATE TABLE public.sys_dict_data (
    dict_code bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    dict_sort integer DEFAULT 0,
    dict_label character varying(100) DEFAULT ''::character varying,
    dict_value character varying(100) DEFAULT ''::character varying,
    dict_type character varying(100) DEFAULT ''::character varying,
    css_class character varying(100) DEFAULT NULL::character varying,
    list_class character varying(100) DEFAULT NULL::character varying,
    is_default character(1) DEFAULT 'N'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT NULL::character varying,
    PRIMARY KEY (dict_code)
);

CREATE TABLE public.sys_dict_type (
    dict_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    dict_name character varying(100) DEFAULT ''::character varying,
    dict_type character varying(100) DEFAULT ''::character varying,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT NULL::character varying,
    PRIMARY KEY (dict_id)
);

CREATE TABLE public.sys_logininfor (
    info_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    user_name character varying(50) DEFAULT ''::character varying,
    client_key character varying(32) DEFAULT ''::character varying,
    device_type character varying(32) DEFAULT ''::character varying,
    ipaddr character varying(128) DEFAULT ''::character varying,
    login_location character varying(255) DEFAULT ''::character varying,
    browser character varying(50) DEFAULT ''::character varying,
    os character varying(50) DEFAULT ''::character varying,
    status character(1) DEFAULT '0'::bpchar,
    msg character varying(255) DEFAULT ''::character varying,
    login_time timestamp(6) without time zone,
    PRIMARY KEY (info_id)
);

CREATE TABLE public.sys_menu (
    menu_id bigint NOT NULL,
    menu_name character varying(50) NOT NULL,
    parent_id bigint DEFAULT 0,
    order_num integer DEFAULT 0,
    path character varying(200) DEFAULT ''::character varying,
    component character varying(255) DEFAULT NULL::character varying,
    query_param character varying(255) DEFAULT NULL::character varying,
    is_frame character(1) DEFAULT '1'::bpchar,
    is_cache character(1) DEFAULT '0'::bpchar,
    menu_type character(1) DEFAULT ''::bpchar,
    visible character(1) DEFAULT '0'::bpchar,
    status character(1) DEFAULT '0'::bpchar,
    perms character varying(100) DEFAULT NULL::character varying,
    icon character varying(100) DEFAULT '#'::character varying,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT ''::character varying,
    PRIMARY KEY (menu_id)
);

CREATE TABLE public.sys_notice (
    notice_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    notice_title character varying(50) NOT NULL,
    notice_type character(1) NOT NULL,
    notice_content text,
    status character(1) DEFAULT '0'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(255) DEFAULT NULL::character varying,
    PRIMARY KEY (notice_id)
);

CREATE TABLE public.sys_oper_log (
    oper_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    title character varying(50) DEFAULT ''::character varying,
    business_type integer DEFAULT 0,
    method character varying(100) DEFAULT ''::character varying,
    request_method character varying(10) DEFAULT ''::character varying,
    operator_type integer DEFAULT 0,
    oper_name character varying(50) DEFAULT ''::character varying,
    dept_name character varying(50) DEFAULT ''::character varying,
    oper_url character varying(255) DEFAULT ''::character varying,
    oper_ip character varying(128) DEFAULT ''::character varying,
    oper_location character varying(255) DEFAULT ''::character varying,
    oper_param character varying(4000) DEFAULT ''::character varying,
    json_result character varying(4000) DEFAULT ''::character varying,
    status integer DEFAULT 0,
    error_msg character varying(4000) DEFAULT ''::character varying,
    oper_time timestamp(6) without time zone,
    cost_time bigint DEFAULT 0,
    PRIMARY KEY (oper_id)
);

CREATE TABLE public.sys_oss (
    oss_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    file_name character varying(255) NOT NULL DEFAULT ''::character varying,
    original_name character varying(255) NOT NULL DEFAULT ''::character varying,
    file_suffix character varying(10) NOT NULL DEFAULT ''::character varying,
    url character varying(500) NOT NULL DEFAULT ''::character varying,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    service character varying(20) DEFAULT 'minio'::character varying,
    PRIMARY KEY (oss_id)
);

CREATE TABLE public.sys_oss_config (
    oss_config_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    config_key character varying(50) NOT NULL DEFAULT ''::character varying,
    access_key character varying(255) DEFAULT ''::character varying,
    secret_key character varying(255) DEFAULT ''::character varying,
    bucket_name character varying(255) DEFAULT ''::character varying,
    prefix character varying(255) DEFAULT ''::character varying,
    endpoint character varying(255) DEFAULT ''::character varying,
    domain character varying(255) DEFAULT ''::character varying,
    is_https character(1) DEFAULT 'N'::bpchar,
    region character varying(255) DEFAULT ''::character varying,
    access_policy character(1) NOT NULL DEFAULT '1'::bpchar,
    status character(1) DEFAULT '1'::bpchar,
    ext1 character varying(255) DEFAULT ''::character varying,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT ''::character varying,
    PRIMARY KEY (oss_config_id)
);

CREATE TABLE public.sys_post (
    post_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    dept_id bigint,
    post_code character varying(64) NOT NULL,
    post_category character varying(100) DEFAULT NULL::character varying,
    post_name character varying(50) NOT NULL,
    post_sort integer NOT NULL,
    status character(1) NOT NULL,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT NULL::character varying,
    PRIMARY KEY (post_id)
);

CREATE TABLE public.sys_role (
    role_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    role_name character varying(30) NOT NULL,
    role_key character varying(100) NOT NULL,
    role_sort integer NOT NULL,
    data_scope character(1) DEFAULT '1'::bpchar,
    menu_check_strictly boolean DEFAULT true,
    dept_check_strictly boolean DEFAULT true,
    status character(1) NOT NULL,
    del_flag character(1) DEFAULT '0'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT NULL::character varying,
    app_id bigint,
    PRIMARY KEY (role_id)
);

CREATE TABLE public.sys_role_dept (
    role_id bigint NOT NULL,
    dept_id bigint NOT NULL,
    PRIMARY KEY (role_id, dept_id)
);

CREATE TABLE public.sys_role_menu (
    role_id bigint NOT NULL,
    menu_id bigint NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE public.sys_social (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    auth_id character varying(255) NOT NULL,
    source character varying(255) NOT NULL,
    open_id character varying(255) DEFAULT NULL::character varying,
    user_name character varying(30) NOT NULL,
    nick_name character varying(30) DEFAULT ''::character varying,
    email character varying(255) DEFAULT ''::character varying,
    avatar character varying(500) DEFAULT ''::character varying,
    access_token character varying(255) NOT NULL,
    expire_in bigint,
    refresh_token character varying(255) DEFAULT NULL::character varying,
    access_code character varying(255) DEFAULT NULL::character varying,
    union_id character varying(255) DEFAULT NULL::character varying,
    scope character varying(255) DEFAULT NULL::character varying,
    token_type character varying(255) DEFAULT NULL::character varying,
    id_token character varying(2000) DEFAULT NULL::character varying,
    mac_algorithm character varying(255) DEFAULT NULL::character varying,
    mac_key character varying(255) DEFAULT NULL::character varying,
    code character varying(255) DEFAULT NULL::character varying,
    oauth_token character varying(255) DEFAULT NULL::character varying,
    oauth_token_secret character varying(255) DEFAULT NULL::character varying,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    del_flag character(1) DEFAULT '0'::bpchar,
    PRIMARY KEY (id)
);

CREATE TABLE public.sys_tenant (
    id bigint NOT NULL,
    tenant_id character varying(20) NOT NULL,
    contact_user_name character varying(20) DEFAULT NULL::character varying,
    contact_phone character varying(20) DEFAULT NULL::character varying,
    company_name character varying(50) DEFAULT NULL::character varying,
    license_number character varying(30) DEFAULT NULL::character varying,
    address character varying(200) DEFAULT NULL::character varying,
    intro character varying(200) DEFAULT NULL::character varying,
    domain character varying(200) DEFAULT NULL::character varying,
    remark character varying(200) DEFAULT NULL::character varying,
    package_id bigint,
    expire_time timestamp(6) without time zone,
    account_count integer DEFAULT '-1'::integer,
    status character(1) DEFAULT '0'::bpchar,
    del_flag character(1) DEFAULT '0'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    PRIMARY KEY (id)
);

CREATE TABLE public.sys_tenant_package (
    package_id bigint NOT NULL,
    package_name character varying(20) DEFAULT ''::character varying,
    menu_ids character varying(3000) DEFAULT ''::character varying,
    remark character varying(200) DEFAULT ''::character varying,
    menu_check_strictly boolean DEFAULT true,
    status character(1) DEFAULT '0'::bpchar,
    del_flag character(1) DEFAULT '0'::bpchar,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    PRIMARY KEY (package_id)
);

CREATE TABLE public.sys_user (
    user_id bigint NOT NULL,
    tenant_id character varying(20) DEFAULT '000000'::character varying,
    dept_id bigint,
    user_name character varying(255),
    nick_name character varying(255),
    user_type character varying(10) DEFAULT 'sys_user'::character varying,
    email character varying(50) DEFAULT ''::character varying,
    phonenumber character varying(255) DEFAULT ''::character varying,
    sex character(1) DEFAULT '0'::bpchar,
    avatar bigint,
    password character varying(100) DEFAULT ''::character varying,
    status character(1) DEFAULT '0'::bpchar,
    del_flag character(1) DEFAULT '0'::bpchar,
    login_ip character varying(128) DEFAULT ''::character varying,
    login_date timestamp(6) without time zone,
    create_dept bigint,
    create_by bigint,
    create_time timestamp(6) without time zone,
    update_by bigint,
    update_time timestamp(6) without time zone,
    remark character varying(500) DEFAULT NULL::character varying,
    org_name character varying(100),
    PRIMARY KEY (user_id)
);

CREATE TABLE public.sys_user_post (
    user_id bigint NOT NULL,
    post_id bigint NOT NULL,
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE public.sys_user_role (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_engineering_geology_geom ON public.data_engineering_geology USING gist (geom);

CREATE INDEX idx_data_geological_structure_geom ON public.data_geological_structure USING gist (geom);

CREATE INDEX idx_data_geomorphological_geom ON public.data_geomorphological USING gist (geom);

CREATE INDEX idx_data_house_point_geom ON public.data_house USING gist (st_setsrid(st_makepoint((longitude)::double precision, (latitude)::double precision), 4490));

CREATE INDEX IF NOT EXISTS idx_data_house_building_name ON public.data_house (building_name);

CREATE INDEX idx_data_house_cim_geometry_4490 ON public.data_house_cim USING gist (st_setsrid(geometry, 4490));

CREATE INDEX IF NOT EXISTS idx_data_person_house_unit_id ON public.data_person (house_unit_id);

CREATE INDEX IF NOT EXISTS idx_data_person_building_code ON public.data_person (building_code);

CREATE INDEX "idx_data_rainfall_entity_grid_mapping_ad_region" ON "public"."data_rainfall_entity_grid_mapping" USING btree (
  "ad_region_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "ad_region_level" "pg_catalog"."int4_ops" ASC NULLS LAST
);

CREATE INDEX "idx_data_rainfall_entity_grid_mapping_forecast_grid" ON "public"."data_rainfall_entity_grid_mapping" USING btree (
  "forecast_grid_lon" "pg_catalog"."float8_ops" ASC NULLS LAST,
  "forecast_grid_lat" "pg_catalog"."float8_ops" ASC NULLS LAST
);

CREATE INDEX "idx_data_rainfall_entity_grid_mapping_slope_unit" ON "public"."data_rainfall_entity_grid_mapping" USING btree (
  "slope_unit_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

CREATE UNIQUE INDEX "uk_data_rainfall_entity_grid_mapping_target" ON "public"."data_rainfall_entity_grid_mapping" USING btree (
  "entity_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  COALESCE(slope_unit_id, ''::character varying) COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  COALESCE(ad_region_id, ''::character varying) COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

ALTER TABLE "public"."data_rainfall_entity_grid_mapping" ADD CONSTRAINT "data_rainfall_entity_grid_mapping_pkey" PRIMARY KEY ("id");

CREATE INDEX idx_data_road_slope_unit_id ON public.data_road USING btree (slope_unit_id);

CREATE INDEX idx_data_road_geology_geom ON public.data_road_geology USING gist (geom);

CREATE INDEX idx_slope_geology_unit_id ON public.data_slope_geology USING btree (slope_unit_id);

create unique index uk_slope_unit_land_planning_stat
    on data_slope_unit_land_planning_stat (slope_unit_id, land_name);

create index idx_slope_unit_land_planning_stat_slope_unit_id
    on data_slope_unit_land_planning_stat (slope_unit_id);

CREATE INDEX IF NOT EXISTS idx_data_territorial_spatial_planning_geom
ON data_territorial_spatial_planning
USING GIST (geom);

CREATE INDEX idx_data_tp_device_curve_hourly_client_time ON public.data_tp_device_curve_hourly USING btree (client_id, source_update_time DESC);

CREATE INDEX idx_data_tp_device_curve_hourly_create_date ON public.data_tp_device_curve_hourly USING btree (create_date DESC);

CREATE INDEX idx_data_tp_device_curve_hourly_monitor_type_time ON public.data_tp_device_curve_hourly USING btree (monitor_type, source_update_time);

CREATE UNIQUE INDEX uk_data_tp_device_curve_hourly_source_point ON public.data_tp_device_curve_hourly USING btree (client_id, monitor_type, source_update_time);

CREATE INDEX idx_data_tp_device_status_current_client ON public.data_tp_device_status_current USING btree (client_id);

CREATE INDEX idx_data_tp_device_status_current_region ON public.data_tp_device_status_current USING btree (region_code);

CREATE INDEX idx_data_tp_device_status_current_update_date ON public.data_tp_device_status_current USING btree (update_date DESC);

CREATE UNIQUE INDEX uk_data_tp_device_status_current_device ON public.data_tp_device_status_current USING btree (device_id);

CREATE INDEX idx_data_tp_warning_event_client_time ON public.data_tp_warning_event USING btree (client_id, warning_time DESC);

CREATE INDEX idx_data_tp_warning_event_device_time ON public.data_tp_warning_event USING btree (device_id, warning_time DESC);

CREATE INDEX idx_data_tp_warning_event_monitor_point_time ON public.data_tp_warning_event USING btree (monitor_point_id, warning_time DESC);

CREATE INDEX idx_data_tp_warning_event_update_date ON public.data_tp_warning_event USING btree (update_date DESC);

CREATE INDEX idx_data_tp_warning_event_warning_time ON public.data_tp_warning_event USING btree (warning_time DESC);

CREATE UNIQUE INDEX uk_data_tp_warning_event_warning ON public.data_tp_warning_event USING btree (warning_id);

CREATE INDEX idx_data_water_system_geology_geom ON public.data_water_system_geology USING gist (geom);

CREATE INDEX idx_dz_def_resp_alarm_history_alarm_id ON public.dz_def_resp_alarm_history USING btree (alarm_id);

CREATE INDEX idx_dz_def_resp_alarm_history_def_id ON public.dz_def_resp_alarm_history USING btree (def_id);

CREATE INDEX idx_dz_def_resp_alarm_history_trigger_time ON public.dz_def_resp_alarm_history USING btree (trigger_time);

CREATE INDEX idx_dz_def_resp_consultation_confirm_def_status_time ON public.dz_def_resp_consultation_confirm USING btree (def_id, deleted, status DESC, create_date DESC, id DESC);

CREATE INDEX idx_dz_def_resp_plan_current_round_no ON public.dz_def_resp_plan USING btree (current_round_no);

CREATE UNIQUE INDEX uk_dz_def_resp_start_sms_config_biz_key ON public.dz_def_resp_start_sms_config USING btree (biz_key) WHERE (del_flag = '0'::bpchar);

CREATE INDEX IF NOT EXISTS idx_dz_evacuation_plan_handle_id_id ON public.dz_evacuation_plan (handle_id, id);

CREATE INDEX idx_dz_process_progress_def_round ON public.dz_process_progress USING btree (def_id, round_no, create_date);

CREATE INDEX idx_dz_risk_assessment_warning_relation_assessment ON public.dz_risk_assessment_warning_relation USING btree (risk_assessment_id);

CREATE UNIQUE INDEX uk_dz_risk_assessment_warning_relation_event ON public.dz_risk_assessment_warning_relation USING btree (warning_event_id);

CREATE UNIQUE INDEX uk_dz_risk_assessment_warning_relation_event_assessment ON public.dz_risk_assessment_warning_relation USING btree (warning_event_id, risk_assessment_id);

CREATE INDEX idx_dz_sms_send_batch_biz ON public.dz_sms_send_batch USING btree (biz_type, biz_id);

CREATE INDEX idx_dz_sms_send_batch_status ON public.dz_sms_send_batch USING btree (send_status);

CREATE UNIQUE INDEX uk_dz_sms_send_batch_batch_id ON public.dz_sms_send_batch USING btree (batch_id);

CREATE INDEX idx_dz_sms_send_stat_batch_id ON public.dz_sms_send_stat USING btree (batch_id);

CREATE INDEX idx_dz_sms_send_stat_biz ON public.dz_sms_send_stat USING btree (biz_type, biz_id);

CREATE INDEX idx_dz_sms_send_stat_receiver_phone ON public.dz_sms_send_stat USING btree (receiver_phone);

CREATE INDEX idx_dz_task_handle_approval_handle_round ON public.dz_task_handle_approval USING btree (handle_id, meeting_type, round_no, process, create_date);

CREATE INDEX idx_dz_task_handle_detail_content_biz ON public.dz_task_handle_detail_content USING btree (biz_type, biz_id, create_date DESC NULLS LAST);

CREATE INDEX idx_dz_task_handle_detail_content_consult_status ON public.dz_task_handle_detail_content USING btree (biz_type, biz_id, content_type, deleted, status DESC, create_date DESC, id DESC);

CREATE INDEX idx_dz_task_handle_detail_content_deleted ON public.dz_task_handle_detail_content USING btree (deleted);

CREATE INDEX idx_dz_task_handle_detail_content_type ON public.dz_task_handle_detail_content USING btree (biz_type, biz_id, content_type, round_no, create_date DESC NULLS LAST);

CREATE INDEX idx_dz_task_handle_detail_content_update_date ON public.dz_task_handle_detail_content USING btree (update_date DESC);

CREATE UNIQUE INDEX uk_dz_task_handle_scene_record_handle_id ON public.dz_task_handle_scene_record USING btree (handle_id);

CREATE INDEX "idx_task_process_chain_node_biz" ON "public"."dz_task_process_chain_node" USING btree (
  "biz_type" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "biz_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE deleted = 0;

CREATE INDEX "idx_task_process_chain_node_chain_id" ON "public"."dz_task_process_chain_node" USING btree (
  "chain_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "trigger_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST,
  "id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE deleted = 0;

CREATE INDEX "idx_task_process_chain_node_parent" ON "public"."dz_task_process_chain_node" USING btree (
  "parent_node_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE deleted = 0;

CREATE INDEX "idx_task_process_chain_node_task_id" ON "public"."dz_task_process_chain_node" USING btree (
  "task_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE deleted = 0;

ALTER TABLE "public"."dz_task_process_chain_node" ADD CONSTRAINT "dz_task_process_chain_node_pkey" PRIMARY KEY ("id");

CREATE UNIQUE INDEX sys_dict_type_index1 ON public.sys_dict_type USING btree (tenant_id, dict_type);

CREATE INDEX idx_sys_logininfor_lt ON public.sys_logininfor USING btree (login_time);

CREATE INDEX idx_sys_logininfor_s ON public.sys_logininfor USING btree (status);

CREATE INDEX idx_sys_oper_log_bt ON public.sys_oper_log USING btree (business_type);

CREATE INDEX idx_sys_oper_log_ot ON public.sys_oper_log USING btree (oper_time);

CREATE INDEX idx_sys_oper_log_s ON public.sys_oper_log USING btree (status);
