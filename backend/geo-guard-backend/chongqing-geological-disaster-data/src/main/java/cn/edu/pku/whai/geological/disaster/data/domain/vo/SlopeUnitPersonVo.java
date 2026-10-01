/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.hutool.core.date.DateUtil;
import lombok.Data;

import java.util.Date;

@Data
public class SlopeUnitPersonVo {

    private String slopeUnitId;
    private Long userId;
    private String userName;
    private String date = DateUtil.formatDate(new Date());
    private String phoneNumber;

}
