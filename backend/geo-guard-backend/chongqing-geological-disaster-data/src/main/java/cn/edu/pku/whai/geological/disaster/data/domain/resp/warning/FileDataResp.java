/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import lombok.Data;
import okhttp3.MediaType;

@Data
public class FileDataResp {

    /**
     * MIME 类型
     */
    private MediaType mediaType;

    /**
     * 文件流字节
     */
    private byte[] fileBytes;

    public FileDataResp() {
    }

    public FileDataResp(byte[] fileBytes, MediaType mediaType) {
        this.fileBytes = fileBytes;
        this.mediaType = mediaType;
    }
}
