package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListAddBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListAddVo;

import java.util.List;

/**
 * 任务上报信息 Service
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
public interface IDzTaskDistListAddService {

    /**
     * 按联合主键查询
     *
     * @param taskId 任务 id
     * @param userId 用户 id
     * @return 记录
     */
    DzTaskDistListAddVo queryByKeys(Long taskId, Long userId);

    /**
     * 分页查询
     */
    TableDataInfo<DzTaskDistListAddVo> queryPageList(DzTaskDistListAddBo bo, PageQuery pageQuery);

    /**
     * 列表查询
     */
    List<DzTaskDistListAddVo> queryList(DzTaskDistListAddBo bo);

    /**
     * 新增
     */
    Boolean insertByBo(DzTaskDistListAddBo bo);

    /**
     * 修改（按 taskId + userId）
     */
    Boolean updateByBo(DzTaskDistListAddBo bo);

    /**
     * 删除（按联合主键）
     *
     * @param taskId  任务 id
     * @param userId  用户 id
     * @param isValid 是否校验存在性
     */
    Boolean deleteWithValidByKeys(Long taskId, Long userId, Boolean isValid);
}
