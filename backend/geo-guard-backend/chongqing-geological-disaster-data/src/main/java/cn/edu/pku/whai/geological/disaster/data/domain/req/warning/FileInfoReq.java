/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req.warning;


import lombok.Data;


/**
 * 根据预警数据id获取文件查询参数
 */
@Data
public class FileInfoReq {
    /**
     * 预警数据id
     */
    private String id;

    /**
     * 文件类型（fxyjjg_slt-预警图，fxyjjg_report-预警报告）
     */
    private String refType = "fxyjjg_slt";
}
