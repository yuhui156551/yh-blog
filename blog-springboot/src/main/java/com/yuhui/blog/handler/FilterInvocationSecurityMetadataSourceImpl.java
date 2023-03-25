package com.yuhui.blog.handler;

import com.yuhui.blog.dao.RoleDao;
import com.yuhui.blog.dto.ResourceRoleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

/**
 * 接口拦截规则
 */
@Component
public class FilterInvocationSecurityMetadataSourceImpl implements FilterInvocationSecurityMetadataSource {

    /**
     * 资源角色列表
     */
    private static List<ResourceRoleDTO> resourceRoleList;

    @Autowired
    private RoleDao roleDao;

    /**
     * 加载资源角色信息
     *
     * @PostConstruct 是Java自带的注解，在方法上加该注解会在项目启动的时候执行该方法，也可以理解为在spring容器初始化的时候执行该方法。
     */
    @PostConstruct
    private void loadDataSource() {
        resourceRoleList = roleDao.listResourceRoles();
    }

    /**
     * 清空接口角色信息
     */
    public void clearDataSource() {
        resourceRoleList = null;
    }

    /**
     * 某角色访问某url时，对比数据库url，对应角色集合，此角色集合会进行保存，这样就知道访问此url需要哪些角色了
     */
    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        // object：当前的请求对象
        // 1、若之前清空过一次资源角色信息，重新加载
        if (CollectionUtils.isEmpty(resourceRoleList)) {
            this.loadDataSource();
        }
        FilterInvocation fi = (FilterInvocation) object;
        // 2、获取用户请求方式
        String method = fi.getRequest().getMethod();
        // 3、获取用户请求Url
        String url = fi.getRequest().getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        // 4、用户url和请求方式与数据库做对比
        for (ResourceRoleDTO resourceRoleDTO : resourceRoleList) {
            if (antPathMatcher.match(resourceRoleDTO.getUrl(), url) && resourceRoleDTO.getRequestMethod().equals(method)) {
                // 5、对比之后，把对应角色进行保存
                List<String> roleList = resourceRoleDTO.getRoleList();
                // 5.1、若无对应角色，创建名为disable的集合，意为可以访问允许匿名访问的url
                if (CollectionUtils.isEmpty(roleList)) {
                    return SecurityConfig.createList("disable");
                }
                // 5.2、若有对应角色，创建数组，保存相关角色
                return SecurityConfig.createList(roleList.toArray(new String[]{}));
            }
        }
        return null;
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return FilterInvocation.class.isAssignableFrom(aClass);
    }

}
