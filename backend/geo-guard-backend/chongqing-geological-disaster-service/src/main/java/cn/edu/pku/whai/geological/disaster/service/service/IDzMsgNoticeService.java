/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzMsgNoticeBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzMsgNoticeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.MsgNoticeCountStat;

import java.util.Collection;
import java.util.List;

/**
 * 业务消息通知Service接口
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
public interface IDzMsgNoticeService {

    /**
     * 查询业务消息通知
     *
     * @param id 主键
     * @return 业务消息通知
     */
    DzMsgNoticeVo queryById(Long id);

    /**
     * 分页查询业务消息通知列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 业务消息通知分页列表
     */
    TableDataInfo<DzMsgNoticeVo> queryPageList(DzMsgNoticeBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的业务消息通知列表
     *
     * @param bo 查询条件
     * @return 业务消息通知列表
     */
    List<DzMsgNoticeVo> queryList(DzMsgNoticeBo bo);

    /**
     * 新增业务消息通知
     *
     * @param bo 业务消息通知
     * @return 是否新增成功
     */
    Boolean insertByBo(DzMsgNoticeBo bo);

    /**
     * 修改业务消息通知
     *
     * @param bo 业务消息通知
     * @return 是否修改成功
     */
    Boolean updateByBo(DzMsgNoticeBo bo);

    /**
     * 校验并批量删除业务消息通知信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    void handle(DzMsgNoticeBo bo);

    MsgNoticeCountStat countStatus(Long userId);

}
