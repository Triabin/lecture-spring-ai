package com.triabin.lecturespringai.controller;

import com.triabin.lecturespringai.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream("static/《我和僵尸有个约会2》原剧梳理.md");
        if (is == null) {
            logger.error("导入向量数据库数据时未找到数据文件");
            return "error";
        }
        List<Document> documents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("# ") || line.startsWith("## ")) continue;
                if (line.startsWith("### ") && !sb.isEmpty()) {
                    documents.add(new Document(sb.toString()));
                    sb.delete(0, sb.length());
                }
                sb.append(!sb.isEmpty() ? "\n" : "").append(line);
            }
            if (!sb.isEmpty()) {
                documents.add(new Document(sb.toString()));
            }
        } catch (Exception e) {
            logger.error("导入向量数据库数据时读取数据文件异常", e);
        }
        if (documents.isEmpty()) {
            return "error";
        }
        store.write(documents);
        return "success";
    }

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
}
