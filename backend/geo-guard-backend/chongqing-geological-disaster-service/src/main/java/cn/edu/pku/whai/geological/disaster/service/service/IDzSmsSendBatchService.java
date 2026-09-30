/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendBatchBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendBatchVo;

import java.util.Date;
import java.util.List;

/**
 * 短信发送批次汇总服务。
 */
public interface IDzSmsSendBatchService {

    /**
     * 新增短信发送批次汇总。
     *
     * @param bo 业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(DzSmsSendBatchBo bo);

    /**
     * 按业务对象查询批次列表。
     *
     * @param bizType 业务类型
     * @param bizId   业务对象ID
     * @return 批次列表
     */
    List<DzSmsSendBatchVo> queryByBiz(Integer bizType, Long bizId);

    /**
     * 判断指定时间窗口内是否已存在相同手机号与正文的成功批次明细记录。
     */
    boolean existsRecentByPhoneAndContent(String phone, String content, Date since);
}
