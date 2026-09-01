package org.etd.framework.common.utils.json;

import com.google.gson.Gson;

public class JsonUtils {

    public static String toGson(Object[] args){
        return new Gson().toJson(args);
    }
}
