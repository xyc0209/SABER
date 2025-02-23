package com.refactor.context;

import com.github.javaparser.ast.expr.Expression;
import lombok.Data;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 字符串变量
 */
@Data
public class StringVariableContext {
    private Expression current; // 当前节点
    private String value; // 值
    private StringVariableContext prev; // 前一个节点

    public StringVariableContext(Expression current, String value, StringVariableContext prev) {
        this.current = current;
        this.value = value;
        this.prev = prev;
    }

    public StringVariableContext(Expression current, String value) {
        this.current = current;
        this.value = value;
    }

    public StringVariableContext(Expression current) {
        this.current = current;
    }

    public StringVariableContext() {}
}
