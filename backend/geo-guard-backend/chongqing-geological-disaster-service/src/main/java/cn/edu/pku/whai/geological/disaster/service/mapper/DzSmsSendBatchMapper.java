/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendBatchVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/**
 * 短信发送批次汇总Mapper接口
 *
 * @author system
 * @date 2026-05-18
 */
public interface DzSmsSendBatchMapper extends BaseMapperPlus<DzSmsSendBatch, DzSmsSendBatchVo> {

    @Select("""
        SELECT COUNT(1) > 0
        FROM dz_sms_send_batch b
        INNER JOIN dz_sms_send_stat s ON b.batch_id = s.batch_id
        WHERE s.receiver_phone = #{phone}
          AND s.sms_content = #{content}
          AND s.send_status = 'SUCCESS'
          AND b.request_time >= #{since}
        """)
    boolean existsRecentByPhoneAndContent(@Param("phone") String phone,
                                          @Param("content") String content,
                                          @Param("since") Date since);
}
