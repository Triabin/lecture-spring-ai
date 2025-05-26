package com.triabin.lecturespringai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.moonshot.MoonshotChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 类描述：初始化AI模型
 *
 * @author Triabin
 * @date 2025-03-25 17:39:03
 */
@RequiredArgsConstructor
@Configuration
public class ChatClientConfig {

    private final MoonshotChatModel moonshotChatModel;

    // 切换ollama模型（本地部署了deepseek-r1:1.5b的模型）
    private final OllamaChatModel ollamaChatModel;

    // 引入向量数据库
    private final VectorStore vectorStore;

    @Bean
    public ChatClient chatClient(ChatMemory chatMemory) {
        return ChatClient.builder(moonshotChatModel)
                .defaultSystem("假如你是特朗普，接下来的对话你必须以特朗普的语气来进行。")
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory), // 应用内存上下文功能
                        new QuestionAnswerAdvisor(vectorStore) // 使用向量数据库
                )
                .build();
    }

    /**
     * 方法描述：初始化ChatMemory
     *
     * @return {@link ChatMemory}
     * @date 2025-03-25 21:18:13
     */
    @Bean
    public ChatMemory chatMemory() {
        // SpringAI官方提供InMemoryChatMemory类进行初始化
        return new InMemoryChatMemory();
    }
}
