/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
public class FileInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @Alias("fileid")
    private String fileId;

    /**
     * 关联ID
     */
    @Alias("refid")
    private String refId;

    /**
     * 文件编码
     */
    @Alias("filecode")
    private String fileCode;

    /**
     * 文件名
     */
    @Alias("filename")
    private String fileName;

    /**
     * 文件类型
     */
    @Alias("filetype")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @Alias("filesize")
    private Long fileSize;

    /**
     * 文件扩展名
     */
    @Alias("fileext")
    private String fileExt;

    /**
     * 文件路径
     */
    @Alias("filepath")
    private String filePath;

    /**
     * 关联类型
     */
    @Alias("reftype")
    private String refType;

    /**
     * 文件MD5值
     */
    @Alias("md5")
    private String md5;

    /**
     * 文件访问URL
     */
    @Alias("url")
    private String url;

    /**
     * 内容类型（MIME类型）
     */
    @Alias("contenttype")
    private String contentType;

    /**
     * 描述信息
     */
    @Alias("description")
    private String description;

    /**
     * 创建人ID
     */
    @Alias("cuserid")
    private String createUserId;

    /**
     * 创建人姓名
     */
    @Alias("cusername")
    private String createUserName;

    /**
     * 创建时间
     */
    @Alias("ctime")
    private String createTime;

    /**
     * 序号
     */
    @Alias("seqno")
    private String seqNo;

    /**
     * 是否启用
     */
    @Alias("enable")
    private String enable;

    /**
     * 状态
     */
    @Alias("state")
    private String state;
}