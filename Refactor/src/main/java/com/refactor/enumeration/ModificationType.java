package com.refactor.enumeration;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public enum ModificationType {
    MODULE_ADD,         // 模块添加
    DEPENDENCY_ADD,     // 依赖添加
    DEPENDENCY_REMOVE,  // 依赖删除
    CONFIG_UPDATE,      // 配置更新
    FILE_CREATE,        // 文件创建
    FILE_DELETE,        // 文件删除
    PARENT_UPDATE,      // 父POM更新
    CODE_CHANGE         // 代码变更
}
