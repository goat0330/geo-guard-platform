/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.service;

import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.TaskReplyBo;

public interface ITaskReplyService {
    void handle(TaskReplyBo bo);
}
