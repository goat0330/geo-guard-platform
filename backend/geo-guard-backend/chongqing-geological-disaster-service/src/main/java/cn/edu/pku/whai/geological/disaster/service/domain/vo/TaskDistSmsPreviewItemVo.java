/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskDistSmsPreviewItemVo {

    /**
     * 接收人用户ID
     */
    private Long userId;

    /**
     * 接收人用户名
     */
    private String nickName;

    /**
     * 本条短信关联的任务ID列表
     */
    private List<Long> taskIdList;

    /**
     * 短信内容
     */
    private String smsContent;
}
