/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl.helper;

import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;

public record GeoAdviceAlarmMatch(DataAlarm alarm, Integer geoAdviceLevel) {}
