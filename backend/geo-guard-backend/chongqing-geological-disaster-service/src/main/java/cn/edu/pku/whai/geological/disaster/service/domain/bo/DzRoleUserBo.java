/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class DzRoleUserBo {
    private String nickName;
    private List<String> roleKeys;
}
