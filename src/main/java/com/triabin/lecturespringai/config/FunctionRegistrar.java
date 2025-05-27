package com.triabin.lecturespringai.config;

import com.triabin.lecturespringai.func.OaService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 类描述：AI模型调用函数的注册器
 *
 * @author Triabin
 * @date 2025-05-27 13:24:42
 */
@Configuration
public class FunctionRegistrar {

    @Bean
    public ToolCallback askForLeaveCallback() {
        return FunctionToolCallback.builder("askForLeave", new OaService())
                .description("当有人请假时，返回请假天数")
                .inputType(OaService.Request.class)
                .build();
    }
}
