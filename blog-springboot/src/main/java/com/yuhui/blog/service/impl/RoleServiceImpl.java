package com.yuhui.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.constant.CommonConst;
import com.yuhui.blog.dao.RoleDao;
import com.yuhui.blog.dao.UserRoleDao;
import com.yuhui.blog.util.PageUtils;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.PageResult;
import com.yuhui.blog.dto.RoleDTO;
import com.yuhui.blog.dto.UserRoleDTO;
import com.yuhui.blog.entity.Role;
import com.yuhui.blog.entity.RoleMenu;
import com.yuhui.blog.entity.RoleResource;
import com.yuhui.blog.entity.UserRole;
import com.yuhui.blog.exception.BizException;
import com.yuhui.blog.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.yuhui.blog.service.RoleMenuService;
import com.yuhui.blog.service.RoleResourceService;
import com.yuhui.blog.service.RoleService;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.vo.RoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色服务
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleDao, Role> implements RoleService {
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private RoleResourceService roleResourceService;
    @Autowired
    private RoleMenuService roleMenuService;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;

    @Override
    public List<UserRoleDTO> listUserRoles() {
        // 查询角色列表
        List<Role> roleList = roleDao.selectList(new LambdaQueryWrapper<Role>()
                .select(Role::getId, Role::getRoleName));
        return BeanCopyUtils.copyList(roleList, UserRoleDTO.class);
    }

    @Override
    public PageResult<RoleDTO> listRoles(ConditionVO conditionVO) {
        // 查询角色列表（包含 资源id列表 和 菜单id列表）
        List<RoleDTO> roleDTOList = roleDao.listRoles(PageUtils.getLimitCurrent(), PageUtils.getSize(), conditionVO);
        // 查询总量
        Integer count = roleDao.selectCount(new LambdaQueryWrapper<Role>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), Role::getRoleName, conditionVO.getKeywords()));
        return new PageResult<>(roleDTOList, count);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateRole(RoleVO roleVO) {
        // 1、判断角色名重复
        Role existRole = roleDao.selectOne(new LambdaQueryWrapper<Role>()
                .select(Role::getId)
                .eq(Role::getRoleName, roleVO.getRoleName()));
        if (Objects.nonNull(existRole) && !existRole.getId().equals(roleVO.getId())) {
            throw new BizException("角色名已存在");
        }
        // 2、保存或更新角色信息
        Role role = Role.builder()
                .id(roleVO.getId())
                .roleName(roleVO.getRoleName())
                .roleLabel(roleVO.getRoleLabel())
                .isDisable(CommonConst.FALSE)
                .build();
        // 如果传递的角色id为空，此处保存到数据库之后，会有id
        this.saveOrUpdate(role);
        // 3、更新角色资源关系（只在编辑资源时生效，新增角色的时候没有资源选择，只有菜单选择）
        if (Objects.nonNull(roleVO.getResourceIdList())) {
            // 3.1、编辑角色，先把此角色下的所有资源删除
            if (Objects.nonNull(roleVO.getId())) {
                roleResourceService.remove(new LambdaQueryWrapper<RoleResource>().eq(RoleResource::getRoleId, roleVO.getId()));
            }
            // 3.2、新增资源
            List<RoleResource> roleResourceList = roleVO.getResourceIdList().stream()
                    .map(resourceId -> RoleResource.builder()
                            .roleId(role.getId())
                            .resourceId(resourceId)
                            .build())
                    .collect(Collectors.toList());
            roleResourceService.saveBatch(roleResourceList);
            // 3.3、重新加载角色资源信息
            filterInvocationSecurityMetadataSource.clearDataSource();
        }
        // 4、更新角色菜单关系
        if(Objects.nonNull(roleVO.getMenuIdList())){
            // 4.1、角色id不为空，说明是编辑菜单信息，把用户的菜单资源删除，后续当做新增角色处理
            if(Objects.nonNull(roleVO.getId())){
                roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleVO.getId()));
            }
            // 4.2、新增菜单
            List<RoleMenu> roleMenuList = roleVO.getMenuIdList().stream()
                    .map(menuId -> RoleMenu.builder()
                            .roleId(role.getId())
                            .menuId(menuId)
                            .build())
                    .collect(Collectors.toList());
            roleMenuService.saveBatch(roleMenuList);
        }
    }

    @Override
    public void deleteRoles(List<Integer> roleIdList) {
        // 判断角色下是否有用户
        Integer count = userRoleDao.selectCount(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getRoleId, roleIdList));
        if (count > 0) {
            throw new BizException("该角色下存在用户");
        }
        roleDao.deleteBatchIds(roleIdList);
    }

}
