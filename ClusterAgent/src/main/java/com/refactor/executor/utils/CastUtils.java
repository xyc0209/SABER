package com.refactor.executor.utils;

import java.math.BigDecimal;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-01 19:06
 */
public class CastUtils {

    public static BigDecimal convertCpuStringToBigDecimal(String cpuValue) {
        // 去掉最后一个字符
        String numericValue = cpuValue.substring(0, cpuValue.length() - 1);
        return new BigDecimal(numericValue);
    }

    // 方法：将内存字符串转换为字节的 BigDecimal
    public static BigDecimal convertMemoryStringToBytes(String memoryValue) {
        // 去掉单位
        String numericValue = memoryValue.substring(0, memoryValue.length() - 2);
        BigDecimal bigDecimalValue = new BigDecimal(numericValue);
        // 转换为字节 (1 KiB = 1024 Bytes)
        return bigDecimalValue.multiply(new BigDecimal(1024));
    }
}