package com.example.dynamic.jpa.common.vo;


import com.example.dynamic.jpa.common.util.StringUtils;

import java.util.HashMap;

/**
 * 全局map转换接口,将下划线转换成驼峰
 */
public class ResultMap extends HashMap {
    @Override
    public Object put(Object key, Object value) {
        return super.put(StringUtils.underlineToHump(String.valueOf(key)), value);
    }
}
