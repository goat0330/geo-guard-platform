-- Independent local storage for the legacy monitoring view contracts.
-- No private source schema, credentials, accounts or business rows are imported.
-- Coordinates are CGCS2000 (4490); slope polygons are WGS84 (4326).
-- Ordinary views reflect newly imported records immediately.

CREATE TABLE IF NOT EXISTS public.data_grid_member (
    phonenumber text PRIMARY KEY,
    user_name text,
    grid_code text
);

CREATE OR REPLACE VIEW public.v_grid_member AS
SELECT * FROM public.data_grid_member;

CREATE TABLE IF NOT EXISTS public.data_disaster_prevention_plan (
    id text PRIMARY KEY,
    basic_info_id text,
    version_sn text,
    monitoring_cycle text,
    monitoring_responsible_person text,
    monitoring_responsible_person_phone text,
    mass_monitor_person text,
    mass_monitor_person_phone text,
    alarm_method text,
    alarm_model text,
    alarm_person text,
    alarm_person_phone text,
    disaster_avoidance_location text,
    personnel_evacuation_route text,
    prevention_suggestions text,
    created_by text,
    created_time timestamp without time zone,
    updated_by text,
    updated_time timestamp without time zone
);

CREATE OR REPLACE VIEW public.v_disaster_prevention_plan AS
SELECT m.*, h.longitude, h.latitude, ST_SetSRID(ST_MakePoint((h.longitude)::double precision, (h.latitude)::double precision), 4490) AS geom, su.id AS slope_unit_id, su.name AS slope_unit_name, su.pilot_area_1, su.pilot_area_2, su.province, su.city, su.county, su.street, su.village, su.province_code, su.city_code, su.county_code, su.street_code, su.village_code
FROM public.data_disaster_prevention_plan m
LEFT JOIN public.data_hazard_point h ON h.id = m.basic_info_id
LEFT JOIN public.data_slope_unit su ON ST_Contains(su.geom, ST_Transform(ST_SetSRID(ST_MakePoint((h.longitude)::double precision, (h.latitude)::double precision), 4490), 4326));

CREATE TABLE IF NOT EXISTS public.data_monitor_point (
    id text PRIMARY KEY,
    monitor_code text,
    basic_info_id text,
    monitor_name text,
    administrative_region_code text,
    location_desc text,
    altitude double precision,
    latitude double precision,
    longitude double precision,
    disaster_type_code text,
    construction_unit text,
    responsible_department text,
    operation_maintenance_unit text,
    patrol_responsible_person text,
    patrol_responsible_person_phone text,
    area_patrol_responsible_person text,
    area_patrol_responsible_person_phone text,
    professional_monitor text,
    professional_monitor_phone text,
    mass_prevention_person text,
    mass_prevention_phone text,
    responsible_person text,
    responsible_person_phone text,
    patrol_monitor text,
    patrol_monitor_phone text,
    monitoring_point_om_person text,
    monitoring_point_om_person_phone text,
    is_warning_model_set bigint,
    warning_frequency double precision,
    bound_device_ids text,
    form_filler text,
    form_fill_unit_id text,
    form_fill_date timestamp without time zone,
    construction_contractor text,
    equipment_parameters text,
    is_canceled bigint,
    year bigint,
    longitude_degree numeric,
    longitude_minute numeric,
    longitude_second numeric,
    latitude_degree numeric,
    latitude_minute numeric,
    latitude_second numeric,
    national_standard_code text,
    batch_sync_push_time timestamp without time zone,
    tag_ids text,
    management_unit_type text,
    created_by text,
    created_time timestamp without time zone,
    updated_by text,
    updated_time timestamp without time zone
);

CREATE OR REPLACE VIEW public.v_monitor_point AS
SELECT m.*, ST_SetSRID(ST_MakePoint((m.longitude)::double precision, (m.latitude)::double precision), 4490) AS geom, su.id AS slope_unit_id, su.name AS slope_unit_name, su.pilot_area_1, su.pilot_area_2, su.province, su.city, su.county, su.street, su.village, su.province_code, su.city_code, su.county_code, su.street_code, su.village_code
FROM public.data_monitor_point m
LEFT JOIN public.data_slope_unit su ON ST_Contains(su.geom, ST_Transform(ST_SetSRID(ST_MakePoint((m.longitude)::double precision, (m.latitude)::double precision), 4490), 4326));

CREATE TABLE IF NOT EXISTS public.data_monitor_device (
    id text PRIMARY KEY,
    device_name text,
    device_status bigint,
    client_id text,
    device_secret text,
    device_serial_number text,
    device_login_password text,
    encrypted_password text,
    device_login_username text,
    global_unique_id text,
    device_parameters text,
    device_business_code text,
    monitoring_point_id text,
    access_protocol text,
    communication_method text,
    device_type text,
    device_model text,
    longitude numeric,
    latitude numeric,
    device_install_address text,
    iot_card_number text,
    monitoring_type text,
    is_enabled text,
    device_enable_time timestamp without time zone,
    device_last_online_time timestamp without time zone,
    created_by text,
    created_time timestamp without time zone,
    updated_by text,
    updated_time timestamp without time zone,
    device_category bigint,
    is_sync_to_screen bigint,
    device_latest_online_time timestamp without time zone,
    batch_sync_device_code text,
    gateway_device_sn text
);

CREATE OR REPLACE VIEW public.v_monitor_device AS
SELECT m.*, p.disaster_type_code AS disaster_type, p.operation_maintenance_unit, ST_SetSRID(ST_MakePoint((m.longitude)::double precision, (m.latitude)::double precision), 4490) AS geom, su.id AS slope_unit_id, su.name AS slope_unit_name, su.pilot_area_1, su.pilot_area_2, su.province, su.city, su.county, su.street, su.village, su.province_code, su.city_code, su.county_code, su.street_code, su.village_code
FROM public.data_monitor_device m
LEFT JOIN public.data_monitor_point p ON p.id = m.monitoring_point_id
LEFT JOIN public.data_slope_unit su ON ST_Contains(su.geom, ST_Transform(ST_SetSRID(ST_MakePoint((m.longitude)::double precision, (m.latitude)::double precision), 4490), 4326));

CREATE INDEX IF NOT EXISTS idx_data_monitor_device_monitoring_point_id ON public.data_monitor_device (monitoring_point_id);

CREATE TABLE IF NOT EXISTS public.data_monitor_type_config (
    id text PRIMARY KEY,
    parent_id text,
    monitoring_content text,
    monitoring_method text,
    monitoring_type_code text,
    parameter_name text,
    parameter_type text,
    extended_attributes text,
    sort_number bigint,
    description text,
    created_by text,
    created_time timestamp without time zone,
    updated_by text,
    updated_time timestamp without time zone,
    monitoring_content_code text,
    remark text,
    monitoring_type_level bigint,
    field_description text,
    parameter_max_value numeric,
    parameter_min_value numeric,
    warning_threshold_value numeric,
    measurement_unit text,
    parameter_default_value text,
    supplementary_content text,
    field_length text
);

CREATE OR REPLACE VIEW public.v_monitor_type_config AS
SELECT * FROM public.data_monitor_type_config;

CREATE TABLE IF NOT EXISTS public.data_organization_info (
    id text PRIMARY KEY,
    organization_full_name text,
    parent_organization_id text,
    organization_short_name text,
    administrative_region_id text,
    organization_code text,
    sort_number bigint,
    organization_level bigint,
    contact_phone text,
    contact_person text,
    email_address text,
    detailed_address text,
    logo_image_path text,
    fax_number text,
    created_by text,
    created_time timestamp without time zone,
    updated_by text,
    updated_time timestamp without time zone,
    remark text,
    external_organization_id text
);

CREATE OR REPLACE VIEW public.v_organization_info AS
SELECT m.*, r.province, r.city, r.county, r.street, r.village
FROM public.data_organization_info m
LEFT JOIN public.data_ad_region r ON rpad(r.id::text, 12, '0') = m.administrative_region_id;

CREATE TABLE IF NOT EXISTS public.data_sensor_basic_info (
    id text PRIMARY KEY,
    sensor_code text,
    device_id text,
    monitoring_point_id text,
    client_id text,
    monitoring_method_code text,
    monitoring_type_code text,
    sensor_name text,
    sensor_parameter_value text,
    resolution text,
    sensitivity text,
    equipment_parameters text,
    storage_date timestamp without time zone,
    communication_method text,
    monitoring_system_icon text,
    measurement_unit text,
    install_latitude numeric,
    install_longitude numeric,
    install_altitude text,
    sensor_model text,
    installation_unit text,
    installation_time timestamp without time zone,
    data_precision text,
    collection_frequency text,
    upload_frequency text,
    device_report_frequency text,
    alarm_report_frequency text,
    installation_location text,
    threshold_value text,
    measurement_min_value text,
    measurement_max_value text,
    monitoring_model text,
    latest_collection_time timestamp without time zone,
    created_by text,
    created_time timestamp without time zone,
    updated_by text,
    updated_time timestamp without time zone,
    is_enabled text,
    measurement_start_value numeric,
    measurement_end_value numeric,
    is_sync_to_screen bigint,
    batch_sync_sensor_code text
);

CREATE OR REPLACE VIEW public.v_sensor_basic_info AS
SELECT m.*, ST_SetSRID(ST_MakePoint((d.longitude)::double precision, (d.latitude)::double precision), 4490) AS geom, su.id AS slope_unit_id, su.name AS slope_unit_name, su.pilot_area_1, su.pilot_area_2, su.province, su.city, su.county, su.street, su.village, su.province_code, su.city_code, su.county_code, su.street_code, su.village_code
FROM public.data_sensor_basic_info m
LEFT JOIN public.data_monitor_device d ON d.id = m.device_id
LEFT JOIN public.data_slope_unit su ON ST_Contains(su.geom, ST_Transform(ST_SetSRID(ST_MakePoint((d.longitude)::double precision, (d.latitude)::double precision), 4490), 4326));

CREATE INDEX IF NOT EXISTS idx_data_sensor_basic_info_device_id ON public.data_sensor_basic_info (device_id);

CREATE TABLE IF NOT EXISTS public.data_sensor_warning_info (
    id text PRIMARY KEY,
    sensor_id text,
    monitoring_point_warning_id text,
    device_id text,
    monitoring_point_id text,
    warning_occur_time timestamp without time zone,
    warning_level_code text,
    warning_description text
);

CREATE OR REPLACE VIEW public.v_sensor_warning_info AS
SELECT m.*, ST_SetSRID(ST_MakePoint((d.longitude)::double precision, (d.latitude)::double precision), 4490) AS geom, su.id AS slope_unit_id, su.name AS slope_unit_name, su.pilot_area_1, su.pilot_area_2, su.province, su.city, su.county, su.street, su.village, su.province_code, su.city_code, su.county_code, su.street_code, su.village_code
FROM public.data_sensor_warning_info m
LEFT JOIN public.data_monitor_device d ON d.id = m.device_id
LEFT JOIN public.data_slope_unit su ON ST_Contains(su.geom, ST_Transform(ST_SetSRID(ST_MakePoint((d.longitude)::double precision, (d.latitude)::double precision), 4490), 4326));

CREATE INDEX IF NOT EXISTS idx_data_sensor_warning_info_device_id ON public.data_sensor_warning_info (device_id);

CREATE TABLE IF NOT EXISTS public.data_warning_disposal_record_monitor (
    id text PRIMARY KEY,
    monitoring_point_warning_id text,
    warning_level text,
    warning_occur_time timestamp without time zone,
    disposal_type_code text,
    backup_warning_pic_count bigint,
    disposal_comments text,
    is_disposal_completed bigint,
    is_disposal_effective bigint,
    is_warning_closed bigint,
    disposal_person text,
    disposal_time timestamp without time zone
);

CREATE OR REPLACE VIEW public.v_warning_disposal_record_monitor AS
SELECT m.*, ST_SetSRID(ST_MakePoint((d.longitude)::double precision, (d.latitude)::double precision), 4490) AS geom, su.id AS slope_unit_id, su.name AS slope_unit_name, su.pilot_area_1, su.pilot_area_2, su.province, su.city, su.county, su.street, su.village, su.province_code, su.city_code, su.county_code, su.street_code, su.village_code
FROM public.data_warning_disposal_record_monitor m
LEFT JOIN public.data_sensor_warning_info w ON w.monitoring_point_warning_id = m.monitoring_point_warning_id
LEFT JOIN public.data_monitor_device d ON d.id = w.device_id
LEFT JOIN public.data_slope_unit su ON ST_Contains(su.geom, ST_Transform(ST_SetSRID(ST_MakePoint((d.longitude)::double precision, (d.latitude)::double precision), 4490), 4326));

CREATE INDEX IF NOT EXISTS idx_warning_disposal_monitor_warning ON public.data_warning_disposal_record_monitor (monitoring_point_warning_id);

CREATE OR REPLACE VIEW public.v_house_slope AS
 SELECT h.id AS house_id,
    h.house_unit_id,
    h.building_code,
    h.building_name,
    h.province_code AS house_province_code,
    h.city_code AS house_city_code,
    h.county_code AS house_county_code,
    h.street_code AS house_street_code,
    h.community_code AS house_community_code,
    h.village_code AS house_village_code,
    h.grid_code,
    h.street_address_code,
    h.door_plate_number,
    h.community_name,
    h.building_number,
    h.unit_number,
    h.floor_number,
    h.room_number,
    h.longitude,
    h.latitude,
    h.create_time,
    h.update_time,
    h.geometry AS house_geom,
    su.id AS slope_unit_id,
    su.name AS slope_unit_name,
    su.pilot_area_1,
    su.pilot_area_2,
    su.urban_area,
    su.province AS slope_unit_province,
    su.city AS slope_unit_city,
    su.county AS slope_unit_county,
    su.street AS slope_unit_street,
    su.village AS slope_unit_village,
    su.community AS slope_unit_community,
    su.center AS slope_unit_center,
    su.area AS slope_unit_area,
    su.province_code AS slope_unit_province_code,
    su.city_code AS slope_unit_city_code,
    su.county_code AS slope_unit_county_code,
    su.street_code AS slope_unit_street_code,
    su.village_code AS slope_unit_village_code,
    su.geom AS slope_unit_geom
   FROM data_house h
     LEFT JOIN data_slope_unit su ON st_contains(su.geom, st_transform(st_setsrid(st_makepoint(h.longitude::double precision, h.latitude::double precision), 4490), 4326))
  ORDER BY h.id;

CREATE OR REPLACE VIEW public.v_person_slope AS
 SELECT p.id AS person_id,
    p.name,
    p.phone_number,
    p.gender,
    p.birthday,
    p.id_type,
    p.id_number,
    p.residence_address,
    p.household_address,
    p.residence_unified_social_credit_code,
    p.residence_unified_address_name,
    p.house_unit_id,
    p.building_code,
    p.province_code AS person_province_code,
    p.city_code AS person_city_code,
    p.county_code AS person_county_code,
    p.street_code AS person_street_code,
    p.community_code AS person_community_code,
    p.village_code AS person_village_code,
    p.grid_code,
    p.grid_name,
    p.community_address_code,
    p.building_number,
    p.unit_number,
    p.floor_number,
    p.house_number,
    round(p.longitude, 10)::numeric(22,10) AS longitude,
    round(p.latitude, 10)::numeric(22,10) AS latitude,
    p.create_time,
    p.update_time,
        CASE
            WHEN p.longitude IS NOT NULL AND p.latitude IS NOT NULL THEN st_point(round(p.longitude, 10)::numeric(22,10)::double precision, round(p.latitude, 10)::numeric(22,10)::double precision, 4490)
            ELSE NULL::geometry
        END AS person_geom,
    su.id AS slope_unit_id,
    su.name AS slope_unit_name,
    su.pilot_area_1,
    su.pilot_area_2,
    su.urban_area,
    su.province AS slope_unit_province,
    su.city AS slope_unit_city,
    su.county AS slope_unit_county,
    su.street AS slope_unit_street,
    su.village AS slope_unit_village,
    su.community AS slope_unit_community,
    su.center AS slope_unit_center,
    su.area AS slope_unit_area,
    su.province_code AS slope_unit_province_code,
    su.city_code AS slope_unit_city_code,
    su.county_code AS slope_unit_county_code,
    su.street_code AS slope_unit_street_code,
    su.village_code AS slope_unit_village_code,
    su.geom AS slope_unit_geom
   FROM data_person p
     LEFT JOIN data_slope_unit su ON p.longitude IS NOT NULL AND p.latitude IS NOT NULL AND st_contains(su.geom, st_transform(st_point(round(p.longitude, 10)::numeric(22,10)::double precision, round(p.latitude, 10)::numeric(22,10)::double precision, 4490), 4326))
  ORDER BY p.id;

CREATE INDEX IF NOT EXISTS idx_data_monitor_point_coordinates ON public.data_monitor_point (longitude, latitude);
