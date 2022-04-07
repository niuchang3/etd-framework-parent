package org.etd.framework.starter.web.interceptor.extend;

import org.etd.framework.starter.web.interceptor.CustomInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
public class CustomDefaultInterceptor extends CustomInterceptor {

    @Override
    protected void beforeHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

    }

    @Override
    protected void afterHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

    }

    /**
     * 获取拦截器路径
     *
     * @return
     */
    @Override
    public List<String> getInterceptorsPath() {
        List<String> list = new ArrayList<>();
        list.add("/**");
        return list;
    }
}
