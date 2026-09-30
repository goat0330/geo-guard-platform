/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 自动模式AI托管运行记录 dz_auto_mode_ai_hosting_record。
 */
@Data
@TableName("dz_auto_mode_ai_hosting_record")
public class DzAutoModeAiHostingRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String recordName;

    private Date openedAt;

    private Date closedAt;

    private Integer durationMinutes;

    private String durationText;

    private Long openUserId;

    private String openUserName;

    private String openUserRole;

    private Long closeUserId;

    private String closeUserName;

    private String closeUserRole;

    private Date createDate;

    private Date updateDate;
}
