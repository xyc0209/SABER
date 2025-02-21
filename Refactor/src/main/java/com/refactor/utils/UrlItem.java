package com.refactor.utils;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-09 19:30
 */
@Data
public class UrlItem {
    public String url1;
    public Map<String, String> url2;

    public UrlItem(){
        this.url2 = new HashMap<>();
    }
}