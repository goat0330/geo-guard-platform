/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorTypeConfigBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorTypeConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 监测类型配置Service接口
 **/
public interface IMonitorTypeConfigService {

    /**
     * 查询监测类型配置
     *
     * @param id 主键
     * @return 监测类型配置
     */
    MonitorTypeConfigVo queryById(String id);

    /**
     * 分页查询监测类型配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 监测类型配置分页列表
     */
    TableDataInfo<MonitorTypeConfigVo> queryPageList(MonitorTypeConfigBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的监测类型配置列表
     *
     * @param bo 查询条件
     * @return 监测类型配置列表
     */
    List<MonitorTypeConfigVo> queryList(MonitorTypeConfigBo bo);

    /**
     * 新增监测类型配置
     *
     * @param bo 监测类型配置
     * @return 是否新增成功
     */
    Boolean insertByBo(MonitorTypeConfigBo bo);

    /**
     * 修改监测类型配置
     *
     * @param bo 监测类型配置
     * @return 是否修改成功
     */
    Boolean updateByBo(MonitorTypeConfigBo bo);

    /**
     * 校验并批量删除监测类型配置信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
