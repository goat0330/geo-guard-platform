/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;

import lombok.Data;

@Data
public class FeedBacksBo {
    private String detailId;
    private Integer rating;
    private String content;
}
