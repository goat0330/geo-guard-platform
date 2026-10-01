/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import cn.edu.pku.whai.geological.disaster.data.domain.po.DefRespRange;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DefRespRangeResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 单点防御响应方案范围（type = 1）
     */
    private DefRespRangeStatus single;

    /**
     * 区域防御响应方案范围（type = 2）
     */
    private DefRespRangeStatus region;

    /**
     * 防御响应方案范围（按是否已启动进行划分）
     */
    @Data
    public static class DefRespRangeStatus {

        /**
         * 已启动的方案范围
         * key 为方案 id，value 为范围列表
         */
        private Map<Long, List<DefRespRange>> started;

        /**
         * 未启动的方案范围
         * key 为方案 id，value 为范围列表
         */
        private Map<Long, List<DefRespRange>> unstarted;


    }


}
