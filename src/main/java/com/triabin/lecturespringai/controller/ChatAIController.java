package com.triabin.lecturespringai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类描述：聊天接口控制类
 *
 * @author Triabin
 * @date 2025-03-25 17:35:34
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatAIController {

    private final ChatClient chatClient;

    @GetMapping("/chat")
    public String chat(@RequestParam(name = "message") String message) {
        return chatClient.prompt()
                .user(message) // 传入输入内容
                .call() // 调用底层模型
                .content(); // 获取返回结果
    }
}
