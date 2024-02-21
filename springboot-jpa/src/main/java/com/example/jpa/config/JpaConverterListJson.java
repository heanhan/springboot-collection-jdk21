package com.example.jpa.config;

import com.alibaba.fastjson.JSON;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName JpaConverterListJson
 * @Description jpa list转换为test 相互转换工具类
 * @Author zhaojh
 */
public class JpaConverterListJson  implements AttributeConverter<Object, String> {

    @Override
    public String convertToDatabaseColumn(Object o) {
        if(ObjectUtils.isEmpty(o)){
            return null;
        }
        return JSON.toJSONString(o);
    }

    @Override
    public Object convertToEntityAttribute(String s) {
        if(StringUtils.hasText(s)){
            return JSON.parseArray(s);
        }
        return null;
    }
}

