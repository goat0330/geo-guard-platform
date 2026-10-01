/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import lombok.Data;

import java.util.List;

/**
 * 监测预警分页响应类
 */
@Data
public class MonitorWarningRecordPageResp {
    /**
     * 总数
     */
    private Integer total;

    /**
     * 列表数据
     */
    private List<MonitorWarningRecordResp> list;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 当前页大小
     */
    private Integer size;

    /**
     * 起始行
     */
    private Integer startRow;

    /**
     * 结束行
     */
    private Integer endRow;

    /**
     * 总页数
     */
    private Integer pages;
}
