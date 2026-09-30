/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.AlgorithmReceiveBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ReceiveHandleProcessVo;

public interface IReceiveService {
    Integer risk(AlgorithmReceiveBo bo);

    ReceiveHandleProcessVo queryHandleProcess(Long handleId);

}
