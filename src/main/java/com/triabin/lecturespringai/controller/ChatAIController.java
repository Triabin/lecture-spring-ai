package com.triabin.lecturespringai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

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

    private final OllamaChatModel ollamaChatModel;

    /**
     * 方法描述：聊天接口
     * @param message 输入内容
     * @return {@link String} 回复内容
     * @date 2025-05-27 00:07:54
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(name = "message") String message) {
        return chatClient.prompt()
                .user(message) // 传入输入内容
                .tools("askForLeave") // 调用自定义函数
                .call() // 调用底层模型
                .content(); // 获取返回结果
    }

    /**
     * 方法描述：聊天接口（带图片）
     * @param pic {@link MultipartFile} 图片文件
     * @param message {@link String} 输入内容
     * @return {@link String} 回复内容
     * @date 2025-05-27 14:18:05
     */
    @PostMapping("/chatWithPic")
    public String chatWithPic(@RequestParam(name = "pic") MultipartFile pic, @RequestParam(name = "message") String message) {
        if (pic == null || pic.isEmpty()) {
            return "无图不聊天哦！";
        }
        String mimeType = pic.getContentType();
        if (mimeType == null || !Arrays.asList("image/jpeg", "image/jpg", "image/png").contains(mimeType.toLowerCase())) {
            return "只支持jpg、png格式的图片！";
        }
        if (message.isEmpty()) {
            return "你想要我对这张图片说点啥？";
        }

        Message msg = new UserMessage(message, List.of(new Media(MimeTypeUtils.parseMimeType(mimeType), pic.getResource())));
        return ollamaChatModel.call(new Prompt(
                List.of(msg),
                ChatOptions.builder()
                        .model(OllamaModel.LLAVA.getName())
                        .build()
                ))
                .getResult()
                .getOutput()
                .getText();
    }
}
