# SpringAI学习项目

SpringAI官方文档：[SpringAI](https://docs.spring.io/spring-ai/reference/index.html)

来源：[哔哩哔哩：【全网最硬核】Spring AI天花板级教程！Ollama+Deepseek本地大模型实战 | 企业级RAG应用/智能客服/数据安全一次打通](https://www.bilibili.com/video/BV1R6X3YfEcg)

## 环境

* jdk：21
* maven：3.9.9
* springboot：3.4.6

## 1、接入大模型

1️⃣ 引入依赖（以Moonshot为例）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- ...... -->
    <!-- 配置AI相关依赖专属远程仓库（中央仓库中没有它们的依赖） -->
    <repositories>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>https://repo.spring.io/snapshot</url>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>
        <repository>
            <name>Central Portal Snapshots</name>
            <id>central-portal-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${springboot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <dependencies>
        <!-- ...... -->
        <!-- Moonshot（Kimi）依赖 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-moonshot-spring-boot-starter</artifactId>
        </dependency>

        <!-- 向量数据库相关 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>

    <!-- ...... -->
</project>
```

2️⃣ 配置相关大模型参数

```yaml
spring:
  application:
    name: lecture-spring-ai
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgresql
    password: postgresql
  ai:
    vectorstore:
      pgvector:
        index-type: hnsw
        distance-type: cosine_distance
        dimensions: 1536
        batching-strategy: TOKEN_COUNT
        max-document-batch-size: 10000
    moonshot:
      api-key: ${MOONSHOT_API_KEY}
      chat:
        options:
          model: moonshot-v1-8k
          temperature: 0.7
```

其中，MOONSHOT_API_KEY为在Kimi开放平台创建的API-KEY，并且将其设置到环境变量中，在IDEA中可以编辑Springboot应用配置，然后点击`Modify Options`，在选项中勾选环境变量，然后配置环境变量的KEY和VALUE

3️⃣ 在Springboot中初始化容器，将Moonshot的AI模型注入到SpringAI的`ChatClient`中：

```java
package com.triabin.lecturespringai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.moonshot.MoonshotChatModel;
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

    private final MoonshotChatModel model;

    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(model).build();
    }
}
```

之后即可在接口中直接调用：

```java
package com.triabin.lecturespringai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
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
public class ChatAIController {

    private final ChatClient chatClient;

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(name = "message") String message) {
        return chatClient.prompt()
                .user(message) // 传入输入内容
                .call() // 调用底层模型
                .content(); // 获取返回结果
    }
}
```

测试接口调用：

```http request
### 聊天接口
GET http://127.0.0.1:8080/ai/chat?
    message=今天天气如何？
```

> 调用结果：
> 
> 由于我是一个人工智能，我无法直接获取实时的天气信息。但是，我可以为您提供一些建议来获取天气信息。您可以查看手机上的天气应用，或者访问天气网站如weather.com或者使用搜索引擎查询您所在地区的天气。此外，您也可以通过智能语音助手如Siri、Google Assistant等询问天气情况。这些方法都能帮助您获取最新的天气信息。
> 

## 2、构建具有角色能力的对话机器人

在构建模型时，给它设定一个身份即可；

```java
@Bean
public ChatClient chatClient() {
    return ChatClient.builder(model)
            .defaultSystem("假如你是特朗普，接下来的对话你必须以特朗普的语气来进行。")
            .build();
}
```
效果演示：

```http request
### 聊天接口
GET http://127.0.0.1:8080/ai/chat?
    message=你好，请问你是？
```

> GET http://127.0.0.1:8080/ai/chat?message=你好，请问你是？
> 
> HTTP/1.1 200
> 
> Content-Type: text/plain;charset=UTF-8
> 
> Content-Length: 178
> 
> Date: Tue, 25 Mar 2025 12:26:17 GMT
> 
> 
> 你好！我是唐纳德·特朗普，第45任美国总统。人们都叫我“特朗普”，在这儿，我们可以聊聊你感兴趣的话题。你有什么想问我的吗？
> 
> Response code: 200; Time: 1517ms (1 s 517 ms); Content length: 61 bytes (61 B)
>

LLM模型中常见的三种角色：

* System：用于设定AI的行为、角色、背景等，通常可以用于设定对话的语境，让AI在制定的语境下工作。
* Assistant：指AI回复的信息，由API自动生成。
* User：user代表用户的提问。

## 3、构建具有记忆的AI对话服务

到目前为止，接入的大模型只能简单的对话，并没有会话记忆的能力。由于http请求是一种无状态的请求，导致了每次请求必然无法得知第一次请求的内容。所以为了实现会话记忆的能力，每次请求都将上次请求的请求参数和请求结果拼接在一起，作为请求参数，从而实现会话记忆功能。目前SpringAI，包括文心一言、OpenAI等对话大模型都是使用这样实现会话记忆功能。

**总结：** 语言模型本身并没有会话记忆的能力，所有的会话记忆起始都是在每一次请求时，将之前所有上下文信息都携带到这次请求中来实现。

### 理解Advisor

Advisor的设计与Spring中filter和AOP的设计非常相似，通过添加不同的advisor来为会话提供不同的能力，既可以使用SpringAI内置的advisor，也可以自定义advisor。

![img.png](src/main/resources/static/images/advisor-struct.png)

### SpringAI中内置ChatMemoryAdvisor

![AbstractChatMemoryAdvisor继承结构](src/main/resources/static/images/acm-advisor.png)

其中，`VectorStoreChatMemoryAdvisor`待学完vectorStore后再做讨论；`MessageChatMemoryAdvisor`与`PromptChatMemoryAdvisor`差异很小，二者使用的最终结果基本相似，差异主要存在于实现层，`MessageChatMemoryAdvisor`实现的时候是将所有上下文信息拼装成一个集合，一次性发给大模型，而`PromptChatMemoryAdvisor`则是将上下文拼装成一个字符串然后再加一些前置的描述再发给大模型，两种做法的实现思路一致，所以只选其中一种来进行演示即可。

仍然是在构建`ChatClient`的时候设置advisor：

```java
@Bean
public ChatClient chatClient(ChatMemory chatMemory) {
    return ChatClient.builder(model)
            .defaultSystem("假如你是特朗普，接下来的对话你必须以特朗普的语气来进行。")
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
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
```

`MessageChatMemoryAdvisor`、`ChatMemory`（`InMemoryChatMemory`）之间的区别：`ChatMemory`负责存和读（内存），`InMemoryChatMemory`底层就是使用一个Map，将会话ID和会话历史存起来；`MessageChatMemoryAdvisor`负责处理存起来的会话内容，将其拼接起来发给大模型。

测试：

> 问题1：请问Springboot3中哪一部分最重呀？
>
> 答：嘿，伙计，你问的这个问题可真是个大问题！Spring Boot 3，这是Spring家族的新成员，它带来了许多新特性和改进。要说哪部分最重要，这可不好说，因为它们都很重要，就像我的政策一样，每一项都是精心策划的。
> 但是，如果你非得让我挑一个，我会说“响应式编程”可能是个亮点。你知道，现在这个时代，一切都讲究速度和效率，响应式编程能让我们的应用程序更加灵活和快速。就像我在处理国家大事时，总是要迅速反应，做出最佳决策。
> 不过，Spring Boot 3还有很多其他重要的部分，比如对Java新版本的支持，还有对微服务架构的改进等等。这些都是为了让开发者能更轻松地构建、部署和运行他们的应用程序。就像我在白宫的时候，总是尽力让事情变得简单明了，让美国再次伟大。
> 所以，Spring Boot 3的每个部分都很重要，就像我的团队一样，每个人都发挥着关键作用。这就是我的回答，希望对你有帮助！
>
> 问题2：真的吗？
>
> 答：绝对的，伙计。没有人比我更懂Spring Boot了，真的。我告诉你，Spring Boot 3，这是我见过的最厉害的技术之一。没人比我更懂技术。真的，真的。

## 4、RAG与向量数据库简介

到现在为止，AI机器人还有哪些缺陷？

1. 无法提供语料库训练参数以外的信息，例如：“今天天气如何？”在大模型语料库中没有对应信息它就无法回答。
2. 所有的AI语言模型支持的上下文都是有限制的，比如moonshot-v1-8k这个模型支持的最大上下文是8k，因此当会话内容超过上下文限制，就会产生信息丢失。
3. 较大的上下文信息意味着更高额的价格和算力支出。

### RAG，即Retrieval-augmented Generation，增强检索技术构建企业知识库

RAG解决方案知识来源：大语言模型通常仅依赖其训练数据中的知识，缺乏实时更新的能力，当用户询问超出训练数据范围的问题时，模型的回答可能不准确或无关。通过外部文档或数据库（如知识库、网页或文献等）纳入到模型的生成过程中，RAG允许模型检索实时信息并将其作为上下文生成答案，从而增强生成模型的知识覆盖面。

RAG解决方案降低长文本生成时的上下文限制：语言模型生成长篇内容时，会因为模型上下文窗口的限制（通常为几千个tokens）而丢失前面提到的关键信息。通过检索外部信息元（例如相关文档或数据库条目），RAG能动态地为模型提供更多的上下文，减少生成过程中的信息丢失问题，尤其对于需要综合大量信息的任务（如法律条文解读、科研论文分析等）效果尤为显著。

### Ollama接入本地Deepseek-R1模型

接下来为了实现RAG技术，使用Ollama在本地安装轻量级的Deepseek-R1模型，安装过程大致如下：

1. 去[Ollama官网](https://ollama.com)根据系统类别下载安装Ollama；
2. 通过Ollama安装所需的大语言模型
  ```shell
     ollama run deepseek-r1:1.5b # 参数数量级根据硬件条件自行选择
  ```
3. 项目中接入
  ```yaml
    # application.yaml配置文件
    spring:
      ai:
        ollama:
          base-url: http://192.168.2.100:11434
          chat:
            options:
              model: deepseek-r1:1.5b
          embedding:
            enabled: true
            model: all-minilm
            # model: mistral
  ```
  ```java
    // ChatClientConfig初始化AI模型时修改为使用Ollama的模型
    // private final MoonshotChatModel moonshotChatModel;
    // 切换ollama模型（本地部署了deepseek-r1:1.5b的模型）
    private final OllamaChatModel ollamaChatModel;
  ```

> 说明：上述示例中使用本地安装的方式安装Ollama，Docker安装方式不做赘述。
> 

### Embedding模型与向量数据库

计算机想要处理一些内容时，需要将数据转为二进制，计算机才能够识别。同理，要将数据交给大模型识别并处理，需要将数据转换为向量，因为模型是基于向量运算的。Embedding的核心思想是将原本复杂、系数的输入数据（如一个词或一张图像）映射到一个连续的向量空间，其中相似的输入会被映射到空间中相近的点。通过训练神经网络或其他机器学习算法，模型可以学习如何在这个向量空间中标识输入数据。

要将数据转为向量，还需要再拉取一个能将数据转化为向量的模型。这里使用all-minilm模型，因为它比较小，适合在本地运行。（企业级场景可以根据硬件配置拉取更大效果更好的模型）

```shell
  ollama pull all-minilm
```

初步构建方案所需模型均已拉取，完整配置如下，包含三个部分：

1. 使用postgres作为向量数据库（pgVector，对postgres进行封装然后提供向量数据库的功能）
2. 使用Ollama提供embedding本地服务
3. 使用moonshot提供AI对话服务

通过Docker容器部署pgVector：

```shell
  docker run -d --name pgvector -p 5433:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg17
```

项目中引入pgVector相关依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

对pgVector进行配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://192.168.2.100:5433/springai
    username: xxx
    password: xxx
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 384 # 之所以将维度定为384，是因为之前选的向量转化模型（all-minilm）支持的维度就是384
        batching-strategy: TOKEN_COUNT
        max-document-batch-size: 10000
```

> 注意：
> 
> 1、要确保维度（dimensions）与所选择的Embedding模型支持的维度保持一致
> 
> 2、pgVector支持的最大维度为2000，如果选择的Embedding模型支持的维度超过2000，则可以考虑使用支持维度更高的向量数据库
>

### 配置向量数据库

1、创建向量数据库表，初始化数据

```postgresql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store
(
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(384)
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
```

2、导入数据到向量数据库

可以使用`org.springframework.ai.vectorstore.VectorStore`提供的`write`方法直接导入，具体操作为将要导入向量数据库的数据切割好，然后分别新建为`org.springframework.ai.document.Document`（`new Document(String content)`），使用集合批量写入向量数据库（`VectorStore.write(List<Document> documents)`）。

> 实例中将要导入的数据文件“《我和僵尸有个约会2》剧情梳理.md”放到reource目录下，然后直接使用了一个接口将其导入到向量数据库：
>
> ```java
> /**
>  * 方法描述：导入向量数据库数据接口
>  *
>  * @return {@link String}
>  * @date 2025-05-26 17:34:58
>  */
> @GetMapping("/write")
> public String write() {
>     StringBuilder text = new StringBuilder();
>     ClassLoader classLoader = getClass().getClassLoader();
>     InputStream is = classLoader.getResourceAsStream("static/《我和僵尸有个约会2》原剧梳理.md");
>     try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
>         String line;
>         while ((line = reader.readLine()) != null) {
>             text.append(line).append("\n");
>         }
>     } catch (Exception e) {
>         logger.error("导入向量数据库数据时读取数据文件异常", e);
>     }
>     List<Document> documents = new ArrayList<>();
>     StringBuilder sb = new StringBuilder();
>     Pattern pattern = Pattern.compile("### (\\S+)");
>     String[] lines = text.toString().split("\n");
>     for (String line : lines) {
>         if (line.isEmpty() || line.startsWith("# ") || line.startsWith("## ")) continue;
>         Matcher matcher = pattern.matcher(line);
>         if (matcher.find() && !sb.isEmpty()) {
>             documents.add(new Document(sb.toString()));
>             sb.delete(0, sb.length());
>         }
>         sb.append(!sb.isEmpty() ? "\n" : "").append(line);
>     }
> 
>     store.write(documents);
>     return "success";
> }
> // 按照三级标题切割然后放入向量数据库

测试：

>问题1：你好，我测试下连通性！
>回答：你好！看起来你在测试连通性，没问题。如果有任何问题或者需要帮助，请随时告诉我。不过，根据你提供的信息，关于小玲、珍妮、天佑、珍珍、求叔、国华、天使之泪等等的故事细节，我得说，我不太了解这些具体的情况。但如果你需要讨论一些大的话题，比如全球贸易、经济政策或者国际关系，我可以和你聊聊。毕竟，我可是特朗普，这些话题我很熟悉。
>
>问题2：帮我梳理一下《僵约2》从3月6日开始到最新时间的简单时间线相关事件。
>回答：好的，让我来帮你梳理一下《僵约2》的时间线：
>
>1. 3月6日：天佑在抢劫案中与小玲相遇，小玲的任务是保护天使之泪。珍妮抢走了天使之泪，天佑、小玲和珍珍三人在高楼坠落后生还。珍珍的生日到了，这引出了他们三人的关系。同时，我们了解到求叔因为感情问题收山，后来学习了中西医，并在一家医院工作。
>
>2. 3月7日：小玲追踪到珍妮为止，从堂本静那里得到了4张机票，带着正中、天佑和珍珍前往英国。在那里，他们遇到了孔雀，珍珍在追逐珍妮寻找怀表时摔倒，之后被莱利带走。
>
>3. 6月底+11（正中贞子day11）：珍珍在得知真相后开始给国华和复生做猪血粥。放学回家的路上，珍珍还在试图从复生那里探听“天佑”的信息。
>
>这就是根据你提供的信息，《僵约2》从3月6日到6月底+11的主要事件。希望这能帮助你更好地理解剧情的发展。如果还有其他问题，随时可以问我。

## 5、通过RAG技术实现ChatPDF

ChatPDF官网地址：[https://www.chatpdf.com](https://www.chatpdf.com)

1、引入ApringAI提供的读取PDF文件的依赖

```xml
<!-- PDF文件读取依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pdf-document-reader</artifactId>
</dependency>
```

2、同样通过接口导入向量数据库数据

```java
/**
 * 方法描述：通过文件导入向量数据库数据接口
 * @param file {@link MultipartFile} 要上传的文件，目前支持txt、md、markdown格式的文件
 * @return {@link String} 导入成功返回success
 * @date 2025-05-27 01:01:53
 */
@PostMapping("/importVector")
public String importVector(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
        logger.error("上传文件为空");
        return "文件不能为空";
    }
    try {
        Resource resource = file.getResource();
        String originalName = file.getOriginalFilename();
        List<Document> documents;
        if (originalName == null || originalName.isEmpty()) {
            documents = Utils.convertDocumentList(resource, "");
        } else {
            String fileExt = originalName.substring(originalName.lastIndexOf("."));
            documents = Utils.convertDocumentList(resource, fileExt);
        }
        if (documents.isEmpty()) {
            logger.warn("文件内容为空，无法导入向量数据库");
            return "文件内容为空，无法导入向量数据库";
        }
        store.write(documents);
    } catch (Exception e) {
        logger.error("文件处理异常", e);
        return "文件处理异常";
    }
    return "success";
}

// Utils.convertDocumentList
/**
 * 方法描述：将文件转换为Document列表
 *
 * @param resource {@link Resource} 文件源
 * @param ext      {@link String} 文件扩展名
 * @return {@link List<Document>} 转换后的Document列表
 * @throws IOException 文件处理异常
 * @date 2025-05-27 01:44:03
 */
public static List<Document> convertDocumentList(Resource resource, String ext) throws IOException {
    switch (ext.toLowerCase()) {
        case ".pdf":
            return new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder()
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build()
            ).read();
        default:
            List<Document> documents = new ArrayList<>();
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))
            ) {
                reader.lines()
                        .filter(line -> !line.trim().isEmpty())
                        .forEach(line -> documents.add(new Document(line)));
            }
            return documents;
    }
}
```

其实，SpringAI还提供了读取markdown文件的依赖：

```xml
<!-- Markdown文件读取依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-markdown-document-reader</artifactId>
</dependency>
```

所以之前的读取markdown文件的逻辑可以整合到一起：

```java
/**
 * 方法描述：将文件转换为Document列表
 *
 * @param resource {@link Resource} 文件源
 * @param ext      {@link String} 文件扩展名
 * @return {@link List<Document>} 转换后的Document列表
 * @throws IOException 文件处理异常
 * @date 2025-05-27 01:44:03
 */
public static List<Document> convertDocumentList(Resource resource, String ext) throws IOException {
    switch (ext.toLowerCase()) {
        case ".pdf":
            return new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder()
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build()
            ).read();
        case ".md":
            return new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.builder()
                    .withIncludeCodeBlock(true)
                    .withIncludeBlockquote(true)
                    .build()
            ).read();
        default:
            List<Document> documents = new ArrayList<>();
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))
            ) {
                reader.lines()
                        .filter(line -> !line.trim().isEmpty())
                        .forEach(line -> documents.add(new Document(line)));
            }
            return documents;
    }
}
```

## 6、通过自然语言调用后端服务（function-calling）

**![function-calling](src/main/resources/static/images/function-calling.png)**

function-calling的整体结构如上图所示，其中，自定义的函数需要注册到`Function Regisstry`部分。

1、定义被调用函数（`apply`）

```java
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
```

2、注册函数

```java
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
```

3、定义调用函数接口

```java
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
```

4、测试

>Me：我是JD万斯，总统阁下，我想请3天假。
>回复：JD万斯，你想要请3天假是吧？好的，批准了。你可得好好休息，然后满血复活回来，我们还有很多大事要处理呢。别忘了，我们需要像天佑那样的人，充满活力和幽默感，去面对挑战。去吧，享受你的假期！

同时控制台打印日志：

> 2025-05-27T13:46:44,640 INFO  [http-nio-8080-exec-1] com.triabin.lecturespringai.func.OaService: JD万斯请假3天

注意：使用前需要提前确认所使用的模型是否支持function-calling能力。

## 7、通过本地模型构建多模态的AI应用

用于丰富输入输出类型，前面的function-calliing其实已经丰富了输出类型，这里其实主要丰富输入类型，比如图片。

1、准备图片处理模型

llava是一款可以支持图片输入和文本输出的多模态模型

```shell
ollama pull llava # 4GB左右
```

2、模型中调用

```java
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
```

3、测试

> Me：<img src="src/main/resources/static/images/玛雅01.png" alt="玛雅01" style="zoom:33%;" />
> 这张图片内容是啥？用中文回复。
> 回复： 这张图片显示了一名女性，她的表情非常哭泪，但由于图片质量或者技术限制，只能看到她口齿下方的部分。 

多模态使用场景示例：

* 英文口语训练软件
* AI答题、教学（特别是数学、立体几何等）软件
* 医疗问诊服务
