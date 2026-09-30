/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeMinuteSummary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Date;

/**
 * 自动模式分钟汇总 Mapper。
 */
public interface DzAutoModeMinuteSummaryMapper extends BaseMapperPlus<DzAutoModeMinuteSummary, DzAutoModeMinuteSummary> {

    @Insert("""
        INSERT INTO dz_auto_mode_minute_summary (
            id,
            window_start,
            action_type,
            action_name,
            execute_count,
            detail_json,
            create_date,
            update_date
        ) VALUES (
            #{summary.id},
            #{summary.windowStart},
            #{summary.actionType},
            #{summary.actionName},
            #{summary.executeCount},
            #{summary.detailJson,typeHandler=cn.edu.pku.whai.geological.disaster.data.typehandler.PgJacksonTypeHandler},
            #{summary.createDate},
            #{summary.updateDate}
        )
        ON CONFLICT (window_start, action_type)
        DO UPDATE SET
            action_name = EXCLUDED.action_name,
            execute_count = dz_auto_mode_minute_summary.execute_count + EXCLUDED.execute_count,
            detail_json = COALESCE(dz_auto_mode_minute_summary.detail_json, '{}'::jsonb)
                || COALESCE(EXCLUDED.detail_json, '{}'::jsonb),
            update_date = EXCLUDED.update_date
        """)
    int upsertIncrement(@Param("summary") DzAutoModeMinuteSummary summary);

    @Select("""
        <script>
        SELECT COALESCE(SUM(execute_count), 0)
        FROM dz_auto_mode_minute_summary
        WHERE window_start &gt;= #{startTime}
          AND window_start &lt;= #{endTime}
        <if test="actionTypes != null and actionTypes.size > 0">
          AND action_type IN
          <foreach collection="actionTypes" item="actionType" open="(" separator="," close=")">
            #{actionType}
          </foreach>
        </if>
        </script>
        """)
    Long sumExecuteCount(@Param("startTime") Date startTime,
                         @Param("endTime") Date endTime,
                         @Param("actionTypes") Collection<Integer> actionTypes);
}
