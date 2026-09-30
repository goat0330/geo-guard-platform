package cn.edu.pku.whai.geological.disaster.service.dify.mapper;


import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetail;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对话历史记录详情Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
public interface AiChatHistoryDetailMapper extends BaseMapperPlus<AiChatHistoryDetail, AiChatHistoryDetailVo> {

    @Select("""
            SELECT id,
                   user_id,
                   history_id,
                   query,
                   answer,
                   message_metadata,
                   create_date
            FROM (SELECT *,
                         ROW_NUMBER() OVER (PARTITION BY history_id ORDER BY create_date DESC) as rn
                  FROM ai_chat_history_detail
                    ${ew.customSqlSegment}
                  ) t
            WHERE t.rn = 1;
            """)
    List<AiChatHistoryDetailVo> selectVoLastByHistoryIds(@Param("ew") LambdaQueryWrapper<AiChatHistoryDetail> lqw1);
}
