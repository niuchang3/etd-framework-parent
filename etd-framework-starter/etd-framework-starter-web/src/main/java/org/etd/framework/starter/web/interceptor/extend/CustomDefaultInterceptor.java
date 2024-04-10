package org.etd.framework.starter.web.interceptor.extend;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.web.interceptor.CustomInterceptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;

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
        SecurityContext context = SecurityContextHolder.getContext();
        if(ObjectUtils.isEmpty(context.getAuthentication()) || !ObjectUtils.isEmpty(context.getAuthentication().getDetails())){
            return;
        }
        if( context.getAuthentication().getDetails() instanceof UserDetails){
            UserDetails details = (UserDetails) context.getAuthentication().getDetails();
            RequestContext.setUser(details);
        }


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
