/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserAdRegionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户行政区划关联Service接口
 */
public interface IDzUserAdRegionService {

    /**
     * 查询用户行政区划关联
     *
     * @param userId 用户ID
     * @return 用户行政区划关联
     */
    DzUserAdRegionVo queryByUserId(Long userId);

    /**
     * 查询用户全部行政区划关联
     *
     * @param userId 用户ID
     * @return 用户行政区划关联列表
     */
    List<DzUserAdRegionVo> queryListByUserId(Long userId);

    /**
     * 查询用户用于数据权限过滤的行政区划。
     * <p>当开启最高层级优先配置时，多个不同层级绑定仅返回最高层级；关闭时返回全部绑定。</p>
     *
     * @param userId 用户ID
     * @return 有效行政区划关联列表
     */
    List<DzUserAdRegionVo> queryEffectiveListByUserId(Long userId);

    /**
     * 分页查询用户行政区划关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 用户行政区划关联分页列表
     */
    TableDataInfo<DzUserAdRegionVo> queryPageList(DzUserAdRegionBo bo, PageQuery pageQuery);

    /**
     * 分页统计用户行政区划分布
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 用户行政区划分布统计分页列表
     */
    TableDataInfo<DzUserAdRegionStatVo> statRegionPage(DzUserAdRegionBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的用户行政区划关联列表
     *
     * @param bo 查询条件
     * @return 用户行政区划关联列表
     */
    List<DzUserAdRegionVo> queryList(DzUserAdRegionBo bo);

    /**
     * 新增用户行政区划关联
     *
     * @param bo 用户行政区划关联
     * @return 是否新增成功
     */
    Boolean insertByBo(DzUserAdRegionBo bo);

    /**
     * 修改用户行政区划关联
     *
     * @param bo 用户行政区划关联
     * @return 是否修改成功
     */
    Boolean updateByBo(DzUserAdRegionBo bo);

    /**
     * 删除用户行政区划关联
     *
     * @param ids     用户ID集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据行政区划名称、层级和角色获取全部候选用户。
     *
     * @param county  区县名称
     * @param street  街镇名称
     * @param village 村名称
     * @param level   行政区划层级
     * @param roleKey 角色标识
     * @return 用户姓名和电话号码列表
     */
    List<DzUserContactVo> resolveUsers(String county, String street, String village, Integer level, String roleKey);
}
