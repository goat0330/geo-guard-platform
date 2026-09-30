package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzMsgNotice;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzMsgNoticeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.MsgNoticeCountStat;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 业务消息通知Mapper接口
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
public interface DzMsgNoticeMapper extends BaseMapperPlus<DzMsgNotice, DzMsgNoticeVo> {

    @Select("""
            SELECT
                 COUNT(*) AS totalCount,
                 COUNT(*) FILTER (WHERE status = 0) AS unreadCount,
                 COUNT(*) FILTER (WHERE status = 1) AS readCount
            FROM
                 dz_msg_notice
            WHERE
                 user_id = #{userId}
            """)
    MsgNoticeCountStat countStatus(@Param("userId") Long userId);
}
