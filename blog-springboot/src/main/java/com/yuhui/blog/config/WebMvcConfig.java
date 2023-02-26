package com.yuhui.blog.config;


import com.yuhui.blog.handler.PageableHandlerInterceptor;
import com.yuhui.blog.handler.WebSecurityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * web mvc配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public WebSecurityHandler getWebSecurityHandler() {
        return new WebSecurityHandler();
    }

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")// 允许跨域访问的路径
                .allowCredentials(true) // 是否发送cookie
                .allowedHeaders("*")// 允许头部设置
                .allowedOriginPatterns("*")// 允许跨域访问的源
                .allowedMethods("*");// 允许请求方法
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageableHandlerInterceptor());
        registry.addInterceptor(getWebSecurityHandler());
    }


}
