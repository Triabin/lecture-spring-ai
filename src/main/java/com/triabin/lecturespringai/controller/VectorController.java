package com.triabin.lecturespringai.controller;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类描述：Vector向量数据库控制接口
 *
 * @author Triabin
 * @date 2025-05-26 17:33:03
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/vector")
public class VectorController {

    private static final Logger logger = LogManager.getLogger(VectorController.class);

    // 注入VectorStore
    final VectorStore store;

    /**
     * 方法描述：导入向量数据库数据接口
     *
     * @return {@link String}
     * @date 2025-05-26 17:34:58
     */
    @GetMapping("/write")
    public String write() {
        StringBuilder text = new StringBuilder();
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream("static/《我和僵尸有个约会2》原剧梳理.md");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (Exception e) {
            logger.error("导入向量数据库数据时读取数据文件异常", e);
        }
        List<Document> documents = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Pattern pattern = Pattern.compile("### (\\S+)");
        String[] lines = text.toString().split("\n");
        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("# ") || line.startsWith("## ")) continue;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find() && !sb.isEmpty()) {
                documents.add(new Document(sb.toString()));
                sb.delete(0, sb.length());
            }
            sb.append(!sb.isEmpty() ? "\n" : "").append(line);
        }

        store.write(documents);
        return "success";
    }
}
