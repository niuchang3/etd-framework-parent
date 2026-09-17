package org.etd.framework.common.utils.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * JSON 序列化与反序列化通用工具类。
 */
public class JsonUtils {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                    json == null || !StringUtils.hasText(json.getAsString()) ? null : LocalDate.parse(json.getAsString()))
            .create();

    public static String toGson(Object[] args) {
        return GSON.toJson(args);
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param object 待序列化对象
     * @return JSON 字符串
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        return GSON.toJson(object);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param json JSON 字符串
     * @param clazz 目标 Class 类型
     * @param <T> 目标对象类型
     * @return 反序列化对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json) || clazz == null) {
            return null;
        }
        return GSON.fromJson(json, clazz);
    }
}
