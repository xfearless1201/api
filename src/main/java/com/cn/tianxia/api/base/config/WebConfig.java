package com.cn.tianxia.api.base.config;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.domain.txdata.v2.RefererUrlDao;
import com.cn.tianxia.api.po.BaseResponse;
import com.cn.tianxia.api.service.v2.TokenService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefererUrlDao refererUrlDao;

    @Value("${excludePaths}")
    private String excludePaths;
    
    /**
     * VIP通道
     */
    private static final String VIP_PATH="Notify,LoginMap,PSGame";

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        HandlerInterceptor handlerInterceptor = new HandlerInterceptor() {

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                response.setContentType("application/json;charset=UTF-8");
                String url = request.getRequestURI();
                List<String> vippaths = Arrays.asList(VIP_PATH.split(","));
                //过滤VIP通道
                for (String vippath : vippaths) {
                    if(url.contains(vippath)){
                        return true;
                    }
                }
                //判断用户是否登录
                Map<String, String> user = tokenService.getUserInfo(request);
                if (CollectionUtils.isEmpty(user)) {
                    //合法域名,判断域名是否为不需要登录即可访问
                    List<String> urls = Arrays.asList(excludePaths.split(","));
                    for (String vippath : urls) {
                        if(url.contains(vippath)){
                            //校验白名单
                            boolean hasAuthor = verifyUrl(request);
                            if(hasAuthor){
                                return true;
                            }else{
                                request.getSession().invalidate();
                                printMessage(response, BaseResponse.faild("faild", "非法域名,无权访问").toString());
                                return false;
                            }
                        }
                    }
                    request.getSession().invalidate();
                    printMessage(response, BaseResponse.faild("faild", "未登录,无权访问").toString());
                    return false;
                }
                return true;
            }

            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                    ModelAndView modelAndView) throws Exception {
                // TODO Auto-generated method stub
                HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                    Exception ex) throws Exception {
                // TODO Auto-generated method stub
                HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
            }
        };
        // 过滤路径
        registry.addInterceptor(handlerInterceptor).addPathPatterns("/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        converters.add(converter);
    }

    public void printMessage(HttpServletResponse response, String message) throws IOException {
        PrintWriter pw = response.getWriter();
        pw.print(message);
        pw.flush();
        pw.close();
    }

    private boolean verifyUrl(HttpServletRequest request) {
        // 验证域名白名单
        String refurl = request.getHeader("referer");
        if (StringUtils.isBlank(refurl)) {
            return false;
        }
        String domain = PatternUtils.getMatchDomain(refurl);
        if (null == domain) {
            return false;
        }
        // 验证来源域名是否属于代该代理平台
        int count = refererUrlDao.selectByReferUrl(domain);
        if (count>0) {
            return true;
        }
        return false;
    }
}
