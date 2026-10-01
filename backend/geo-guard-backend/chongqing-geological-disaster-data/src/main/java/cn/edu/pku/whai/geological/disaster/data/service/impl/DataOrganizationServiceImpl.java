/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSydwDto;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Organization;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataOrganizationMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataOrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 组织机构信息Service业务层处理
 *
 * @author system
 * @date 2024
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataOrganizationServiceImpl implements IDataOrganizationService {

    private final DataOrganizationMapper baseMapper;

    /**
     * 批量保存组织机构数据
     *
     * @param dataList 组织机构数据列表
     */
    @Override
    public void saveBatch(List<YbssSydwDto> dataList) {
        try {
            if (dataList == null || dataList.isEmpty()) {
                log.warn("组织机构数据列表为空，跳过保存");
                return;
            }

            List<Organization> poList = dataList.stream()
                                                .map(dto -> MapstructUtils.convert(dto, Organization.class))
                                                .toList();

            baseMapper.insertOrUpdateBatch(poList);
            log.info("成功保存 {} 条组织机构数据", poList.size());
        } catch (Exception e) {
            log.error("批量保存组织机构数据失败", e);
            throw new ServiceException("批量保存组织机构数据失败：" + e.getMessage());
        }
    }
}
