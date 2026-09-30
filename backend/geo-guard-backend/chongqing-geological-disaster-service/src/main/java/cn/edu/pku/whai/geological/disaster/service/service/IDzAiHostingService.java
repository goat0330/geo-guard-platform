/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordSessionVo;

/**
 * AI 托管记录展示服务。
 */
public interface IDzAiHostingService {

    /**
     * 查询 AI 托管概览。
     *
     * @return AI 托管运行状态、统计摘要和展示记录
     */
    AiHostingOverviewVo getOverview(Long recordId);

    /**
     * 分页查询 AI 托管开启记录。
     *
     * @param pageQuery 分页参数
     * @param limit 每条托管记录内返回的历史时间窗口组数，默认 50，最大 200
     * @return AI 托管开启记录分页数据
     */
    TableDataInfo<AiHostingRecordSessionVo> listRecords(PageQuery pageQuery, Integer limit);

}
