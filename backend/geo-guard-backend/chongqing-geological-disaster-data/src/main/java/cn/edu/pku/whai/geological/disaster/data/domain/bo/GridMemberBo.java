/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.GridMember;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 网格人员信息业务对象 v_grid_member
 **/
@Data
@AutoMapper(target = GridMember.class, reverseConvertGenerate = false)
public class GridMemberBo {

    /**
     * 人员id
     */
    private String phonenumber;

    /**
     * 姓名
     */
    private String userName;

    /**
     * 编码
     */
    private String gridCode;


}
