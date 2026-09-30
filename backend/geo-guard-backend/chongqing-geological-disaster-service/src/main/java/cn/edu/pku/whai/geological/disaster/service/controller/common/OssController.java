/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.dto.OssDTO;
import org.dromara.common.core.service.OssService;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.vo.SysOssUploadVo;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.ApplyUrlBo;
import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * oss
 *
 * @author kongweiguang
 */
@RestController
@RequestMapping("/dizai/oss")
@RequiredArgsConstructor
public class OssController {

    private final ISysOssService sysOssService;
    private final OssService ossService;

    /**
     * 通过 OSS ID 批量申请私有文件访问地址，沿用系统 OSS 服务和统一过期策略。
     */
    @PostMapping("/apply-url")
    public R<List<String>> applyUrl(@RequestBody ApplyUrlBo bo) {
        List<OssDTO> ossDTOS = ossService.selectByIds(String.join(",", bo.getIds()));

        OssClient instance = OssFactory.instance();
        List<String> urls = ossDTOS.stream()
                .map(e -> instance.createPresignedGetUrl(e.getFileName(), Duration.ofMinutes(10)))
                .toList();

        return R.ok(urls);
    }

    /**
     * 通过 OSS ID 下载文件，交给系统 OSS 服务统一处理响应流和存储差异。
     */
    @Log(title = "OSS文件下载", businessType = BusinessType.EXPORT, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @SneakyThrows
    @PostMapping("/down-url/{id}")
    public void downFile(@PathVariable Long id, HttpServletResponse response) {
        sysOssService.download(id, response);
    }

    /**
     * 上传文件到 OSS，复用系统上传服务以保持元数据落库和存储实现一致。
     */
    @Log(title = "OSS文件", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysOssUploadVo> upload(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        SysOssVo oss = sysOssService.upload(file);
        SysOssUploadVo uploadVo = new SysOssUploadVo();
        uploadVo.setUrl(oss.getUrl());
        uploadVo.setFileName(oss.getOriginalName());
        uploadVo.setOssId(oss.getOssId().toString());
        return R.ok(uploadVo);
    }
}
