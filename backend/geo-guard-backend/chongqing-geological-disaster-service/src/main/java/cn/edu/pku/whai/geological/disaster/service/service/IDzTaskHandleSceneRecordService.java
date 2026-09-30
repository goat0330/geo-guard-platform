package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleSceneRecordBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;

import java.util.Collection;
import java.util.List;

/**
 * 处置管理现场记录Service接口
 *
 * @author kongweiguang
 * @date 2026-01-30
 */
public interface IDzTaskHandleSceneRecordService {

    /**
     * 查询处置管理现场记录
     *
     * @param id 主键
     * @return 处置管理现场记录
     */
    DzTaskHandleSceneRecordVo queryById(Long id);

    /**
     * 分页查询处置管理现场记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 处置管理现场记录分页列表
     */
    TableDataInfo<DzTaskHandleSceneRecordVo> queryPageList(DzTaskHandleSceneRecordBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的处置管理现场记录列表
     *
     * @param bo 查询条件
     * @return 处置管理现场记录列表
     */
    List<DzTaskHandleSceneRecordVo> queryList(DzTaskHandleSceneRecordBo bo);

    /**
     * 新增处置管理现场记录
     *
     * @param bo 处置管理现场记录
     * @return 是否新增成功
     */
    Boolean insertByBo(DzTaskHandleSceneRecordBo bo);

    /**
     * 修改处置管理现场记录
     *
     * @param bo 处置管理现场记录
     * @return 是否修改成功
     */
    Boolean updateByBo(DzTaskHandleSceneRecordBo bo);

    /**
     * 校验并批量删除处置管理现场记录信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    DzTaskHandleSceneRecordVo getByHandleId(Long handleId);

    void insertByReq(DzTaskHandleSceneRecordReq req);

}
