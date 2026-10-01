/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 网格人员信息对象 v_grid_member
 **/
@Data
@TableName("v_grid_member")
public class GridMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 人员id
     */
    @TableId(value = "phonenumber")
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
