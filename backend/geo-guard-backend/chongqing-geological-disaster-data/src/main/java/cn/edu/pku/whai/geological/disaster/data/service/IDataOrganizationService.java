/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSydwDto;

import java.util.List;

/**
 * 组织机构信息Service接口
 *
 * @author system
 * @date 2024
 */
public interface IDataOrganizationService {

    /**
     * 批量保存组织机构数据
     *
     * @param dataList 组织机构数据列表
     */
    void saveBatch(List<YbssSydwDto> dataList);
}
