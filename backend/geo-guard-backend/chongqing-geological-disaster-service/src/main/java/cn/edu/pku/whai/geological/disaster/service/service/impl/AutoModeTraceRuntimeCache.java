/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeExecutionTrace;

import java.util.List;

interface AutoModeTraceRuntimeCache {

    void putRunning(DzAutoModeExecutionTrace trace);

    void deleteRunning(Long traceId);

    List<DzAutoModeExecutionTrace> listRunning();
}
