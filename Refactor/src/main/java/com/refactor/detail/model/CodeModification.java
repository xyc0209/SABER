package com.refactor.detail.model;

import com.refactor.enumeration.ModificationType;
import com.refactor.utils.FileFactory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Data
public class CodeModification {
    private String filePath;         // 修改文件路径
    private ModificationType type;   // 修改类型
    private String oldValue;         // 修改前内容（JSON格式）
    private String newValue;         // 修改后内容（JSON格式）
    private String description;      // 修改描述
    private LocalDateTime timestamp;// 时间戳

    public CodeModification(String filePath, ModificationType type, String oldValue, String newValue, String description) {
        if (FileFactory.isFileExists(filePath)) {
            this.filePath = filePath;
        } else {
            this.filePath = null;
        }
        this.type = type;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }
}
