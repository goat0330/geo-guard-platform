/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 点位命中的房屋关联信息。
 *
 * @author system
 * @date 2026-06-06
 */
@Data
public class HousePointMatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房屋ID
     */
    private String houseId;

    /**
     * 户室唯一ID
     */
    private String houseUnitId;

    /**
     * 房屋建筑代码（3010码）
     */
    private String buildingCode;

    /**
     * 建筑物名称
     */
    private String buildingName;
}
