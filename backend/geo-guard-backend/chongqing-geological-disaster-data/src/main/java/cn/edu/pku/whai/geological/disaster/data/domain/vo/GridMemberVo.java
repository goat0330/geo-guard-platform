/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.GridMember;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 网格人员信息视图对象 v_grid_member
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = GridMember.class)
public class GridMemberVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
