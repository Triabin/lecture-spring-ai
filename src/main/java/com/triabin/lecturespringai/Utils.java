package com.triabin.lecturespringai;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 类描述：工具类
 *
 * @author Triabin
 * @date 2025-05-27 01:06:50
 */
public class Utils {

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
}
