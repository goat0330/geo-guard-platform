/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.service.impl;


import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.TaskReplyBo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.ITaskReplyService;
import cn.edu.pku.whai.geological.disaster.service.mcp.cache.McpToolsRedisCache;
import org.springframework.stereotype.Service;

@Service
public class TaskReplyServiceImpl implements ITaskReplyService {

    @Override
    public void handle(TaskReplyBo bo) {

        String reply = bo.getReply();

        boolean b = McpToolsRedisCache.hasTaskKey(bo.getTaskId());
        if (b) {
            McpToolsRedisCache.setTaskKey(bo.getTaskId(), reply);
        }
    }
}
