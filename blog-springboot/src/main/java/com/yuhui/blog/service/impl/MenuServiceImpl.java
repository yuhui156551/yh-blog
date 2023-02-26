package com.yuhui.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.dao.MenuDao;
import com.yuhui.blog.dao.RoleMenuDao;
import com.yuhui.blog.dto.MenuDTO;
import com.yuhui.blog.dto.LabelOptionDTO;
import com.yuhui.blog.dto.UserMenuDTO;
import com.yuhui.blog.entity.Menu;
import com.yuhui.blog.entity.RoleMenu;
import com.yuhui.blog.exception.BizException;
import com.yuhui.blog.service.MenuService;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.util.UserUtils;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.MenuVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.yuhui.blog.constant.CommonConst.*;
import static com.yuhui.blog.constant.CommonConst.COMPONENT;

/**
 * 菜单服务
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuDao, Menu> implements MenuService {
    @Autowired
    private MenuDao menuDao;
    @Autowired
    private RoleMenuDao roleMenuDao;

    /**
     * 查看菜单列表
     *
     * @param conditionVO 条件
     * @return 菜单列表
     */
    @Override
    public List<MenuDTO> listMenus(ConditionVO conditionVO) {
        // 1、查询菜单数据
        List<Menu> menuList = menuDao.selectList(new LambdaQueryWrapper<Menu>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), Menu::getName, conditionVO.getKeywords()));
        // 2、获取目录列表
        List<Menu> catalogList = listCatalog(menuList);
        // 3、获取目录下的子菜单，根据父id分组，map：key为父id，value为子菜单
        Map<Integer, List<Menu>> childrenMap = getMenuMap(menuList);
        // 4、组装目录、子菜单数据
        List<MenuDTO> menuDTOList = catalogList.stream().map(item -> {
            MenuDTO menuDTO = BeanCopyUtils.copyObject(item, MenuDTO.class);
            // 4.1、获取目录下的菜单排序，放入目录
            List<MenuDTO> list = BeanCopyUtils.copyList(childrenMap.get(item.getId()), MenuDTO.class).stream()
                    .sorted(Comparator.comparing(MenuDTO::getOrderNum))
                    .collect(Collectors.toList());
            menuDTO.setChildren(list);
            // 4.2、移除已存放的子菜单
            childrenMap.remove(item.getId());
            return menuDTO;
            // 4.3、目录根据orderNum进行排序
            // 对象集合以类属性一升序排序,list.stream().sorted(Comparator.comparing(类::属性一));
        }).sorted(Comparator.comparing(MenuDTO::getOrderNum)).collect(Collectors.toList());
        // 5、若还有菜单未取出则拼接（一定是有父id，但是父id对应不上目录id）
        if (CollectionUtils.isNotEmpty(childrenMap)) {
            List<Menu> childrenList = new ArrayList<>();
//            childrenMap.values().forEach(item -> childrenList.addAll(item));
            childrenMap.values().forEach(childrenList::addAll);
            List<MenuDTO> childrenDTOList = childrenList.stream()
                    .map(item -> BeanCopyUtils.copyObject(item, MenuDTO.class))
                    .sorted(Comparator.comparing(MenuDTO::getOrderNum))
                    .collect(Collectors.toList());
            menuDTOList.addAll(childrenDTOList);
        }
        return menuDTOList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateMenu(MenuVO menuVO) {
        // 两种情况：（这里不用关心，menuVO里有parentId字段，有或无都直接保存到数据库）
        // 1、新增菜单，没有父id
        // 2、菜单下新增子菜单，有父id
        Menu menu = BeanCopyUtils.copyObject(menuVO, Menu.class);
        this.saveOrUpdate(menu);
    }

    @Override
    public void deleteMenu(Integer menuId) {
        // 1、查询是否有角色关联
        Integer count = roleMenuDao.selectCount(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getMenuId, menuId));
        if (count > 0) {
            throw new BizException("菜单下有角色关联");
        }
        // 2、查询子菜单
        List<Integer> menuIdList = menuDao.selectList(new LambdaQueryWrapper<Menu>()
                        .select(Menu::getId)
                        .eq(Menu::getParentId, menuId))
                .stream()
                .map(Menu::getId)
                .collect(Collectors.toList());
        // 3、如果有子菜单，目录和子菜单一起删除
        menuIdList.add(menuId);
        menuDao.deleteBatchIds(menuIdList);
    }

    @Override
    public List<LabelOptionDTO> listMenuOptions() {
        // 1、查询所有菜单数据
        List<Menu> menuList = menuDao.selectList(new LambdaQueryWrapper<Menu>()
                .select(Menu::getId, Menu::getName, Menu::getParentId, Menu::getOrderNum));
        // 2、获取目录列表
        List<Menu> catalogList = listCatalog(menuList);
        // 3、获取目录下的子菜单
        Map<Integer, List<Menu>> childrenMap = getMenuMap(menuList);
        // 4、组装目录菜单数据
        return catalogList.stream().map(item -> {
            // 获取目录下的菜单排序
            List<LabelOptionDTO> list = new ArrayList<>();
            List<Menu> children = childrenMap.get(item.getId());
            // 此目录含有子菜单
            if (CollectionUtils.isNotEmpty(children)) {
                list = children.stream()
                        .sorted(Comparator.comparing(Menu::getOrderNum))
                        .map(menu -> LabelOptionDTO.builder()
                                .id(menu.getId())
                                .label(menu.getName())
                                .build())
                        .collect(Collectors.toList());
            }
            return LabelOptionDTO.builder()
                    .id(item.getId())
                    .label(item.getName())
                    .children(list)// 存放子菜单或null
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserMenuDTO> listUserMenus() {
        // 查询用户菜单信息
        List<Menu> menuList = menuDao.listMenusByUserInfoId(UserUtils.getLoginUser().getUserInfoId());
        // 获取目录列表
        List<Menu> catalogList = listCatalog(menuList);
        // 获取目录下的子菜单
        Map<Integer, List<Menu>> childrenMap = getMenuMap(menuList);
        // 转换前端菜单格式
        return convertUserMenuList(catalogList, childrenMap);
    }

    /**
     * 获取目录列表
     *
     * @param menuList 菜单列表
     * @return 目录列表
     */
    private List<Menu> listCatalog(List<Menu> menuList) {
        return menuList.stream()
                .filter(item -> Objects.isNull(item.getParentId()))
                .sorted(Comparator.comparing(Menu::getOrderNum))
                .collect(Collectors.toList());
    }

    /**
     * 获取目录下菜单列表
     *
     * @param menuList 菜单列表
     * @return 目录下的菜单列表
     */
    private Map<Integer, List<Menu>> getMenuMap(List<Menu> menuList) {
        return menuList.stream()
                .filter(item -> Objects.nonNull(item.getParentId()))
                .collect(Collectors.groupingBy(Menu::getParentId));
    }

    /**
     * 转换用户菜单格式
     *
     * @param catalogList 目录
     * @param childrenMap 子菜单
     */
    private List<UserMenuDTO> convertUserMenuList(List<Menu> catalogList, Map<Integer, List<Menu>> childrenMap) {
        return catalogList.stream().map(item -> {
            UserMenuDTO userMenuDTO = new UserMenuDTO();// 目录
            List<UserMenuDTO> list = new ArrayList<>();// 子菜单
            // 1、获取目录下的子菜单
            List<Menu> children = childrenMap.get(item.getId());
            if (CollectionUtils.isNotEmpty(children)) {
                // 1.1、多级菜单处理
                userMenuDTO = BeanCopyUtils.copyObject(item, UserMenuDTO.class);
                list = children.stream()
                        .sorted(Comparator.comparing(Menu::getOrderNum))
                        .map(menu -> {
                            UserMenuDTO dto = BeanCopyUtils.copyObject(menu, UserMenuDTO.class);
                            dto.setHidden(menu.getIsHidden().equals(TRUE));// Integer->Boolean
                            return dto;
                        })
                        .collect(Collectors.toList());
            } else {
                // 1.2、一级菜单处理
                userMenuDTO.setPath(item.getPath());
                userMenuDTO.setComponent(COMPONENT);
                list.add(UserMenuDTO.builder()
                        .path("")
                        .name(item.getName())
                        .icon(item.getIcon())
                        .component(item.getComponent())
                        .build());
            }
            // 2、组装目录子菜单
            userMenuDTO.setHidden(item.getIsHidden().equals(TRUE));
            userMenuDTO.setChildren(list);
            return userMenuDTO;
        }).collect(Collectors.toList());
    }

}
