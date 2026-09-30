package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzReportDisasterFeedbackBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterHandleVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzReportDisasterVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReportDisasterStatVo;

import java.util.Collection;
import java.util.List;

/**
 * 报灾管理Service接口
 *
 * @author kongweiguang
 * @date 2026-01-27
 */
public interface IDzReportDisasterService {

    /**
     * 查询报灾管理
     *
     * @param id 主键
     * @return 报灾管理
     */
    DzReportDisasterVo queryById(Long id);

    /**
     * 分页查询报灾管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 报灾管理分页列表
     */
    TableDataInfo<DzReportDisasterVo> queryPageList(DzReportDisasterBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的报灾管理列表
     *
     * @param bo 查询条件
     * @return 报灾管理列表
     */
    List<DzReportDisasterVo> queryList(DzReportDisasterBo bo);

    /**
     * 新增报灾管理
     *
     * @param bo 报灾管理
     * @return 是否新增成功
     */
    Boolean insertByBo(DzReportDisasterBo bo);

    /**
     * 修改报灾管理
     *
     * @param bo 报灾管理
     * @return 是否修改成功
     */
    Boolean updateByBo(DzReportDisasterBo bo);

    /**
     * 校验并批量删除报灾管理信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    DzReportDisasterHandleVo handle(Long id);

    ReportDisasterStatVo stat();

    void closeReport(Long id);

    List<DzReportDisasterVo> getAllHandling();

    void addFeedback(DzReportDisasterFeedbackBo bo);
}
