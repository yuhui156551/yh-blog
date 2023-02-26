package com.yuhui.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.dao.ResourceDao;
import com.yuhui.blog.dao.RoleResourceDao;
import com.yuhui.blog.dto.ResourceDTO;
import com.yuhui.blog.dto.LabelOptionDTO;
import com.yuhui.blog.entity.Resource;
import com.yuhui.blog.entity.RoleResource;
import com.yuhui.blog.exception.BizException;
import com.yuhui.blog.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.yuhui.blog.service.ResourceService;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.ResourceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.yuhui.blog.constant.CommonConst.FALSE;

/**
 * 资源服务
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceDao, Resource> implements ResourceService {
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private RoleResourceDao roleResourceDao;
    @Autowired
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;


    @Override
    public void saveOrUpdateResource(ResourceVO resourceVO) {
        // 1、更新资源信息
        Resource resource = BeanCopyUtils.copyObject(resourceVO, Resource.class);
        this.saveOrUpdate(resource);
        // 2、重新加载角色资源信息
        filterInvocationSecurityMetadataSource.clearDataSource();
    }

    @Override
    public void deleteResource(Integer resourceId) {
        // 1、查询是否有角色关联
        Integer count = roleResourceDao.selectCount(new LambdaQueryWrapper<RoleResource>()
                .eq(RoleResource::getResourceId, resourceId));
        if (count > 0) {
            throw new BizException("该资源下存在角色");
        }
        // 2、如果是删除模块，那么同时删除子资源
        List<Integer> resourceIdList = resourceDao.selectList(new LambdaQueryWrapper<Resource>()
                        .select(Resource::getId).
                        eq(Resource::getParentId, resourceId))
                .stream()
                .map(Resource::getId)
                .collect(Collectors.toList());
        resourceIdList.add(resourceId);
        resourceDao.deleteBatchIds(resourceIdList);
    }

    @Override
    public List<ResourceDTO> listResources(ConditionVO conditionVO) {
        // 1、查询资源列表
        List<Resource> resourceList = resourceDao.selectList(new LambdaQueryWrapper<Resource>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), Resource::getResourceName, conditionVO.getKeywords()));
        // 2、获取所有模块
        List<Resource> parentList = listResourceModule(resourceList);
        // 3、获取模块下的所有资源，并根据父id进行分组
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resourceList);
        // 4、绑定模块下的所有接口
        List<ResourceDTO> resourceDTOList = parentList.stream().map(item -> {
            ResourceDTO resourceDTO = BeanCopyUtils.copyObject(item, ResourceDTO.class);
            List<ResourceDTO> childrenList = BeanCopyUtils.copyList(childrenMap.get(item.getId()), ResourceDTO.class);
            resourceDTO.setChildren(childrenList);
            childrenMap.remove(item.getId());
            return resourceDTO;
        }).collect(Collectors.toList());
        // 5、若还有资源未取出则拼接
        if (CollectionUtils.isNotEmpty(childrenMap)) {
            List<Resource> childrenList = new ArrayList<>();
//            childrenMap.values().forEach(value -> childrenList.addAll(value));
            // void forEach(Consumer<? super T> action);
            // 该方法接收一个 Consumer 接口函数，会将每一个流元素交给该函数进行处理。
            childrenMap.values().forEach(childrenList::addAll);
            List<ResourceDTO> childrenDTOList = childrenList.stream()
                    .map(item -> BeanCopyUtils.copyObject(item, ResourceDTO.class))
                    .collect(Collectors.toList());
            resourceDTOList.addAll(childrenDTOList);
        }
        return resourceDTOList;
    }

    @Override
    public List<LabelOptionDTO> listResourceOption() {
        // 1、查询资源列表
        List<Resource> resourceList = resourceDao.selectList(new LambdaQueryWrapper<Resource>()
                .select(Resource::getId, Resource::getResourceName, Resource::getParentId)
                .eq(Resource::getIsAnonymous, FALSE));
        // 2、获取所有模块
        List<Resource> parentList = listResourceModule(resourceList);
        // 3、根据父id分组获取模块下的资源
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resourceList);
        // 4、组装模块、资源数据
        return parentList.stream().map(item -> {
            // 不能用下面这种方式处理，因为对象转换，没有赋值给 label，导致前端没有显示名称
           /* LabelOptionDTO labelOptionDTO = BeanCopyUtils.copyObject(item, LabelOptionDTO.class);
            List<LabelOptionDTO> childrenList = BeanCopyUtils.copyList(childrenMap.get(item.getId()), LabelOptionDTO.class);
            labelOptionDTO.setChildren(childrenList);
            childrenMap.remove(item.getId());
            return labelOptionDTO;*/
            List<LabelOptionDTO> list = new ArrayList<>();
            List<Resource> children = childrenMap.get(item.getId());
            // 4.1、如果子资源不为空，则转换成 LabelOptionDTO 对象并保存到 list
            if (CollectionUtils.isNotEmpty(children)) {
                list = children.stream()
                        .map(resource -> LabelOptionDTO.builder()
                                .id(resource.getId())
                                .label(resource.getResourceName())
                                .build())
                        .collect(Collectors.toList());
            }
            // 4.2、子资源为空，list为空
            return LabelOptionDTO.builder()
                    .id(item.getId())
                    .label(item.getResourceName())
                    .children(list)
                    .build();
        }).collect(Collectors.toList());
        /*if (CollectionUtils.isNotEmpty(childrenMap)) {
            List<Resource> childrenList = new ArrayList<>();
            childrenMap.values().forEach(childrenList::addAll);
            List<LabelOptionDTO> childrenDTOList = childrenList.stream()
                    .map(item -> BeanCopyUtils.copyObject(item, LabelOptionDTO.class))
                    .collect(Collectors.toList());
            labelOptionDTOList.addAll(childrenDTOList);
        }
        return labelOptionDTOList;*/
    }

    /**
     * 获取模块下的所有资源
     *
     * @param resourceList 资源列表
     * @return 模块资源
     */
    private Map<Integer, List<Resource>> listResourceChildren(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.nonNull(item.getParentId()))
                .collect(Collectors.groupingBy(Resource::getParentId));
    }

    /**
     * 获取所有资源模块
     *
     * @param resourceList 资源列表
     * @return 资源模块列表
     */
    private List<Resource> listResourceModule(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.isNull(item.getParentId()))
                .collect(Collectors.toList());
    }

}
