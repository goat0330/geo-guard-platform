/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.aspect;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.service.annotation.SimulationEnabledRequired;
import cn.edu.pku.whai.geological.disaster.service.props.DizaiSimulationProps;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 统一拦截受模拟/联调总开关控制的接口。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class SimulationEnabledRequiredAspect {

    private final DizaiSimulationProps simulationProps;

    @Around("@annotation(simulationEnabledRequired)")
    public Object around(ProceedingJoinPoint joinPoint, SimulationEnabledRequired simulationEnabledRequired) throws Throwable {
        if (Boolean.TRUE.equals(simulationProps.getEnabled())) {
            return joinPoint.proceed();
        }
        String message = simulationEnabledRequired.message();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (R.class.isAssignableFrom(signature.getReturnType())) {
            return R.fail(message);
        }
        throw new ServiceException(message);
    }
}
