-- Test fixtures only. Every inserted record is rolled back; no demo business data is retained.
BEGIN;
INSERT INTO public.data_slope_unit (id, name, pilot_area_1, province, geom)
VALUES ('__local_schema_test__', 'Schema test polygon', 1, 'Schema test region',
        ST_GeomFromText('POLYGON((106 29,107 29,107 30,106 30,106 29))', 4326));
INSERT INTO public.data_person (id, name, phone_number, longitude, latitude)
VALUES ('__local_schema_test__', 'Schema test person', 'TEST-NOT-A-PHONE', 106.5, 29.5);
INSERT INTO public.data_house (id, building_code, building_name, longitude, latitude)
VALUES ('__local_schema_test__', '__local_schema_test__', 'Schema test house', 106.5, 29.5);
INSERT INTO public.data_hazard_point (id, longitude, latitude)
VALUES ('__local_schema_test__', 106.5, 29.5);
INSERT INTO public.data_grid_member (phonenumber, user_name, grid_code)
VALUES ('__local_schema_test__', 'Schema test member', '__local_schema_test__');
INSERT INTO public.data_disaster_prevention_plan (id, basic_info_id)
VALUES ('__local_schema_test__', '__local_schema_test__');
INSERT INTO public.data_monitor_point (id, monitor_name, longitude, latitude, disaster_type_code, operation_maintenance_unit)
VALUES ('__local_schema_test__', 'Schema test monitor', 106.5, 29.5, 'TEST', 'Schema test operator');
INSERT INTO public.data_monitor_device (id, monitoring_point_id, longitude, latitude, monitoring_type)
VALUES ('__local_schema_test__', '__local_schema_test__', 106.5, 29.5, 'L1_GP');
INSERT INTO public.data_monitor_type_config (id, monitoring_type_code)
VALUES ('__local_schema_test__', 'L1_GP');
INSERT INTO public.data_organization_info (id, organization_full_name)
VALUES ('__local_schema_test__', 'Schema test organization');
INSERT INTO public.data_sensor_basic_info (id, device_id)
VALUES ('__local_schema_test__', '__local_schema_test__');
INSERT INTO public.data_sensor_warning_info (id, device_id, monitoring_point_warning_id)
VALUES ('__local_schema_test__', '__local_schema_test__', '__local_schema_test__');
INSERT INTO public.data_warning_disposal_record_monitor (id, monitoring_point_warning_id)
VALUES ('__local_schema_test__', '__local_schema_test__');
DO $$
DECLARE
    relation_name text;
    key_name text;
    record_count integer;
BEGIN
    FOREACH relation_name IN ARRAY ARRAY[
        'v_person_slope', 'v_house_slope', 'v_grid_member', 'v_disaster_prevention_plan',
        'v_monitor_point', 'v_monitor_device', 'v_monitor_type_config', 'v_organization_info',
        'v_sensor_basic_info', 'v_sensor_warning_info', 'v_warning_disposal_record_monitor'
    ] LOOP
        key_name := CASE relation_name WHEN 'v_person_slope' THEN 'person_id'
                    WHEN 'v_house_slope' THEN 'house_id' WHEN 'v_grid_member' THEN 'phonenumber' ELSE 'id' END;
        EXECUTE format('SELECT count(*) FROM public.%I WHERE %I = $1', relation_name, key_name)
            INTO record_count USING '__local_schema_test__';
        IF record_count <> 1 THEN RAISE EXCEPTION '% did not expose its imported record', relation_name; END IF;
        IF relation_name NOT IN ('v_grid_member', 'v_monitor_type_config', 'v_organization_info') THEN
            EXECUTE format('SELECT count(*) FROM public.%I WHERE %I = $1 AND slope_unit_id = $1 AND pilot_area_1 = 1', relation_name, key_name)
                INTO record_count USING '__local_schema_test__';
            IF record_count <> 1 THEN RAISE EXCEPTION '% failed 4490 to 4326 spatial association', relation_name; END IF;
        END IF;
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM public.v_monitor_device WHERE id = '__local_schema_test__'
                   AND disaster_type = 'TEST' AND operation_maintenance_unit = 'Schema test operator') THEN
        RAISE EXCEPTION 'Device lost monitoring-point fields';
    END IF;
    UPDATE public.data_monitor_point SET monitor_name = 'Updated schema test monitor' WHERE id = '__local_schema_test__';
    IF NOT EXISTS (SELECT 1 FROM public.v_monitor_point WHERE id = '__local_schema_test__'
                   AND monitor_name = 'Updated schema test monitor') THEN
        RAISE EXCEPTION 'View requires a refresh instead of reflecting current storage';
    END IF;
    INSERT INTO public.data_monitor_point (id, monitor_name) VALUES ('__local_schema_null_test__', 'No coordinates');
    IF NOT EXISTS (SELECT 1 FROM public.v_monitor_point WHERE id = '__local_schema_null_test__' AND geom IS NULL) THEN
        RAISE EXCEPTION 'Record with missing coordinates was dropped or failed';
    END IF;
    RAISE NOTICE 'PASS: 11 live views, spatial associations, device fields, updates and null coordinates';
END $$;
ROLLBACK;
