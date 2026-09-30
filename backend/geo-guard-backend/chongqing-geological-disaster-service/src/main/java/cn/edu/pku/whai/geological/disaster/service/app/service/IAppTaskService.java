/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.service;

import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionRemindReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionSubmitReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionTaskReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.resp.PushTaskResp;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListHistoryVo;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface IAppTaskService {
    void submitTask(InspectionSubmitReq req);

    List<String> uploadImages(Long taskId, MultipartFile[] files);

    /**
     * 根据ossId列表获取照片预览URL
     *
     * @param ossIds ossId列表，多个用逗号分隔或单独传递
     * @return 预览URL列表，与ossIds顺序一致
     */
    List<String> getPhotoPreviewUrls(List<String> ossIds);

    PushTaskResp pushTask(List<InspectionTaskReq> req);

    PushTaskResp updateTask(List<InspectionTaskReq> req);

    void remindTask(InspectionRemindReq req);

    Long getInvalidSubmitCount(LocalDate date);

    List<DzTaskDistListHistoryVo> feedbacks(Long taskId);
}
