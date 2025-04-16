package com.triabin.lecturespringai.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 类描述：API日志打印切面
 *
 * @author Triabin
 * @date 2025-03-25 18:29:29
 */
@Component
@Aspect
@Slf4j
public class ApiLogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) && within(com.triabin.lecturespringai.controller..*)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();

        // 1. 请求参数记录（智能截断）
        try {
            Object[] args = joinPoint.getArgs();
            String params = JSON.toJSONString(args,
                    JSONWriter.Feature.IgnoreNonFieldGetter,
                    JSONWriter.Feature.WriteBigDecimalAsPlain);

            if (params.length() > MAX_PARAM_LENGTH) {
                params = params.substring(0, MAX_PARAM_LENGTH) + "...[TRUNCATED]";
            }

            log.info("[REQUEST] traceId={} | method={} | params={}",
                    traceId, method.getName(), params);
        } catch (JSONException e) {
            log.warn("[PARAM_SERIALIZE_ERROR] {}", e.getMessage());
        }

        // 2. 方法执行与耗时统计
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            MDC.put("costTime", String.valueOf(costTime));
        }

        // 3. 响应结果处理（异步记录）
        CompletableFuture.runAsync(() -> {
            try {
                String response = JSON.toJSONString(result,
                        JSONWriter.Feature.PrettyFormat,
                        JSONWriter.Feature.WriteNulls);

                log.info("[RESPONSE] traceId={} | cost={}ms | result={}",
                        traceId, MDC.get("costTime"), response);
            } catch (Exception e) {
                log.error("[RESULT_SERIALIZE_ERROR]", e);
            } finally {
                MDC.remove("costTime");
            }
        });

        return result;
    }

    @AfterThrowing(pointcut = "logPointCut()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        log.error("[EXCEPTION] method={} | errorType={} | message={} | stack={}",
                method.getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex.getStackTrace());
    }
}
