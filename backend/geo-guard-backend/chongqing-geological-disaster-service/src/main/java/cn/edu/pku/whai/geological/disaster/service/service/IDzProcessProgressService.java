/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DzDefProcessProgressVo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzProcessProgressBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzProcessProgressVo;

import java.util.Collection;
import java.util.List;

public interface IDzProcessProgressService {

    DzProcessProgressVo queryById(Long id);

    TableDataInfo<DzProcessProgressVo> queryPageList(DzProcessProgressBo bo, PageQuery pageQuery);

    List<DzProcessProgressVo> queryList(DzProcessProgressBo bo);

    Boolean insertByBo(DzProcessProgressBo bo);

    Boolean updateByBo(DzProcessProgressBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    List<DzDefProcessProgressVo> getDefInfo(Long defId);
}
