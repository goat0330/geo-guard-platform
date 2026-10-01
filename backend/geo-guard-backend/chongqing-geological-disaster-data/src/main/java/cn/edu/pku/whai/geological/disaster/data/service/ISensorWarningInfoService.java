/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SensorWarningInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SensorWarningInfoVo;

import java.util.Collection;
import java.util.List;

/**
 * 传感器预警Service接口
 **/
public interface ISensorWarningInfoService {

    /**
     * 查询传感器预警
     *
     * @param id 主键
     * @return 传感器预警
     */
    SensorWarningInfoVo queryById(String id);

    /**
     * 分页查询传感器预警列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 传感器预警分页列表
     */
    TableDataInfo<SensorWarningInfoVo> queryPageList(SensorWarningInfoBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的传感器预警列表
     *
     * @param bo 查询条件
     * @return 传感器预警列表
     */
    List<SensorWarningInfoVo> queryList(SensorWarningInfoBo bo);

    /**
     * 新增传感器预警
     *
     * @param bo 传感器预警
     * @return 是否新增成功
     */
    Boolean insertByBo(SensorWarningInfoBo bo);

    /**
     * 修改传感器预警
     *
     * @param bo 传感器预警
     * @return 是否修改成功
     */
    Boolean updateByBo(SensorWarningInfoBo bo);

    /**
     * 校验并批量删除传感器预警信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
