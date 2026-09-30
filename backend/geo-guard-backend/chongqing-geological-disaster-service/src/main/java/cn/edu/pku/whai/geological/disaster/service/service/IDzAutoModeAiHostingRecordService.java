/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordSessionVo;

import java.util.Date;

/**
 * 自动模式AI托管运行记录服务。
 */
public interface IDzAutoModeAiHostingRecordService {

    /**
     * 自动模式开启时创建托管记录。
     *
     * @param openedAt 开启时间
     * @return 记录视图
     */
    AiHostingRecordSessionVo createOnOpen(Date openedAt);

    /**
     * 自动模式关闭时关闭最近一条运行中托管记录。
     *
     * @param closedAt 关闭时间
     */
    void closeLatest(Date closedAt);

    /**
     * 查询托管记录。
     *
     * @param id 主键
     * @return 记录视图
     */
    AiHostingRecordSessionVo queryById(Long id);

    /**
     * 查询最新一条托管记录。
     *
     * @return 最新托管记录；无记录时返回 null
     */
    AiHostingRecordSessionVo queryLatest();

    /**
     * 分页查询托管记录。
     *
     * @param pageQuery 分页参数
     * @return 分页记录
     */
    TableDataInfo<AiHostingRecordSessionVo> queryPageList(PageQuery pageQuery);
}
