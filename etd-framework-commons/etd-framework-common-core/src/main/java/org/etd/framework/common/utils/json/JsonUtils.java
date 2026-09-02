package org.etd.framework.common.utils.json;

import com.google.gson.Gson;

/**
 * JSON 序列化与反序列化通用工具类。
 */
public class JsonUtils {

    public static String toGson(Object[] args){
        return new Gson().toJson(args);
    }
}
