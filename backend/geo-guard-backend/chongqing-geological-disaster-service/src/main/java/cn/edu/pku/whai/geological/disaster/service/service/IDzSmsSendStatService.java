/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendStatVo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 短信发送单条记录服务。
 */

public interface IDzSmsSendStatService {

    /**
     * 查询短信发送单条记录。
     *
     * @param id 主键
     * @return 短信发送单条记录
     */
    DzSmsSendStatVo queryById(Long id);

    /**
     * 分页查询短信发送单条记录列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<DzSmsSendStatVo> queryPageList(DzSmsSendStatBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的短信发送单条记录列表。
     *
     * @param bo 查询条件
     * @return 列表
     */
    List<DzSmsSendStatVo> queryList(DzSmsSendStatBo bo);

    /**
     * 判断指定时间窗口内是否已存在相同手机号与正文的成功单条发送记录。
     */
    boolean existsRecentByPhoneAndContent(String phone, String content, Date since);

    /**
     * 新增短信发送单条记录。
     *
     * @param bo 业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(DzSmsSendStatBo bo);

    /**
     * 修改短信发送单条记录。
     *
     * @param bo 业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(DzSmsSendStatBo bo);

    /**
     * 批量删除短信发送单条记录。
     *
     * @param ids     主键集合
     * @param isValid 是否校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
