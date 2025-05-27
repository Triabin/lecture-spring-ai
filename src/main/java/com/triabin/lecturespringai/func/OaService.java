package com.triabin.lecturespringai.func;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

/**
 * 类描述：演示function-calling的类，以OA为例
 *
 * @author Triabin
 * @date 2025-05-27 13:17:29
 */
public class OaService implements Function<OaService.Request, OaService.Response> {

    private static final Logger logger = LogManager.getLogger(OaService.class);

    @Override
    public OaService.Response apply(OaService.Request request) {
        logger.info("{}请假{}天", request.who, request.days);
        return new Response(request.days);
    }

    public record Request(String who, int days) {}

    public record Response(int days) {}
}
