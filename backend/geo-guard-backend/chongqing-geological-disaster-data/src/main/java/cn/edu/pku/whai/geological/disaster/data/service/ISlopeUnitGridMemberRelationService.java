/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitGridMemberRelationBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;

import java.util.Collection;
import java.util.List;

/**
 * 斜坡单元网格员信息关联Service接口
 */
public interface ISlopeUnitGridMemberRelationService {

    /**
     * 查询斜坡单元网格员信息关联
     *
     * @param id 主键
     * @return 斜坡单元网格员信息关联
     */
    SlopeUnitGridMemberRelationVo queryById(Long id);

    /**
     * 根据斜坡单元ID查询关联人员信息
     *
     * @param unitId 斜坡单元ID
     * @return 斜坡单元网格员信息关联
     */
    SlopeUnitGridMemberRelationVo queryByUnitId(String unitId);

    /**
     * 根据斜坡单元ID集合批量查询关联人员信息
     *
     * @param unitIds 斜坡单元ID集合
     * @return 斜坡单元网格员信息关联列表
     */
    List<SlopeUnitGridMemberRelationVo> queryByUnitIds(Collection<String> unitIds);

    /**
     * 分页查询斜坡单元网格员信息关联列表
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 斜坡单元网格员信息关联分页列表
     */
    TableDataInfo<SlopeUnitGridMemberRelationVo> queryPageList(SlopeUnitGridMemberRelationBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的斜坡单元网格员信息关联列表
     *
     * @param bo 查询条件
     * @return 斜坡单元网格员信息关联列表
     */
    List<SlopeUnitGridMemberRelationVo> queryList(SlopeUnitGridMemberRelationBo bo);

    /**
     * 新增斜坡单元网格员信息关联
     *
     * @param bo 业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(SlopeUnitGridMemberRelationBo bo);

    /**
     * 修改斜坡单元网格员信息关联
     *
     * @param bo 业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(SlopeUnitGridMemberRelationBo bo);

    /**
     * 校验并批量删除斜坡单元网格员信息关联
     *
     * @param ids 主键集合
     * @param isValid 是否校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
