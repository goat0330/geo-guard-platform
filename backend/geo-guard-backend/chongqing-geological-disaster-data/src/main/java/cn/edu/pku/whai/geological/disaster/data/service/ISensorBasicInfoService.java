/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SensorBasicInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SensorBasicInfoVo;

import java.util.Collection;
import java.util.List;

/**
 * 传感器基础信息Service接口
 **/
public interface ISensorBasicInfoService {

    /**
     * 查询传感器基础信息
     *
     * @param id 主键
     * @return 传感器基础信息
     */
    SensorBasicInfoVo queryById(String id);

    /**
     * 分页查询传感器基础信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 传感器基础信息分页列表
     */
    TableDataInfo<SensorBasicInfoVo> queryPageList(SensorBasicInfoBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的传感器基础信息列表
     *
     * @param bo 查询条件
     * @return 传感器基础信息列表
     */
    List<SensorBasicInfoVo> queryList(SensorBasicInfoBo bo);

    /**
     * 新增传感器基础信息
     *
     * @param bo 传感器基础信息
     * @return 是否新增成功
     */
    Boolean insertByBo(SensorBasicInfoBo bo);

    /**
     * 修改传感器基础信息
     *
     * @param bo 传感器基础信息
     * @return 是否修改成功
     */
    Boolean updateByBo(SensorBasicInfoBo bo);

    /**
     * 校验并批量删除传感器基础信息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
