package com.example.ddd.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 序列化工具。
 *
 * <p><b>为什么整个项目共享一个 ObjectMapper？</b>
 * ObjectMapper 是<b>线程安全</b>但创建成本高，共享单例可以：
 * <ul>
 *   <li>保证 MQ 消息、Feign 调用、Controller 响应使用同一套序列化规则。</li>
 *   <li>统一日期格式（{@code yyyy-MM-dd HH:mm:ss}）。</li>
 *   <li>避免各处 new ObjectMapper() 造成的性能损耗。</li>
 * </ul>
 *
 * @author ddd-learning
 */
public final class JsonUtils {

    /** 全局唯一 ObjectMapper */
    public static final ObjectMapper MAPPER = createMapper();

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private JsonUtils() {
        // 工具类禁止实例化
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 忽略未知字段，兼容跨服务演进
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // null 值不序列化，减小 payload
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 关闭 "日期序列化为时间戳"
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 注册 Java 8 时间类型支持
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        mapper.registerModule(javaTimeModule);

        return mapper;
    }

    /** 对象转 JSON 字符串 */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败: " + obj.getClass().getName(), e);
        }
    }

    /** JSON 字符串转对象 */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 反序列化失败: " + clazz.getName() + ", json=" + json, e);
        }
    }

    /** JSON 字符串转泛型对象（例如 {@code List<OrderDTO>}） */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 反序列化失败: " + typeRef.getType(), e);
        }
    }
}
