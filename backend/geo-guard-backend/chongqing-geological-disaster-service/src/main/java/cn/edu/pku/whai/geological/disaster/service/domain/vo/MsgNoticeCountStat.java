/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

@Data
public class MsgNoticeCountStat {
    private Integer totalCount;
    private Integer unreadCount;
    private Integer readCount;
}
