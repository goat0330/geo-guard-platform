/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.WarningDisposalRecordMonitorBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.WarningDisposalRecordMonitorVo;

import java.util.Collection;
import java.util.List;

/**
 * 预警处置记录监测Service接口
 **/
public interface IWarningDisposalRecordMonitorService {

    /**
     * 查询预警处置记录监测
     *
     * @param id 主键
     * @return 预警处置记录监测
     */
    WarningDisposalRecordMonitorVo queryById(String id);

    /**
     * 分页查询预警处置记录监测列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 预警处置记录监测分页列表
     */
    TableDataInfo<WarningDisposalRecordMonitorVo> queryPageList(WarningDisposalRecordMonitorBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的预警处置记录监测列表
     *
     * @param bo 查询条件
     * @return 预警处置记录监测列表
     */
    List<WarningDisposalRecordMonitorVo> queryList(WarningDisposalRecordMonitorBo bo);

    /**
     * 新增预警处置记录监测
     *
     * @param bo 预警处置记录监测
     * @return 是否新增成功
     */
    Boolean insertByBo(WarningDisposalRecordMonitorBo bo);

    /**
     * 修改预警处置记录监测
     *
     * @param bo 预警处置记录监测
     * @return 是否修改成功
     */
    Boolean updateByBo(WarningDisposalRecordMonitorBo bo);

    /**
     * 校验并批量删除预警处置记录监测信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
