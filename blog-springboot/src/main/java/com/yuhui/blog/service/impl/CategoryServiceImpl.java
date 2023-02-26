package com.yuhui.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.yuhui.blog.dao.ArticleDao;
import com.yuhui.blog.dto.CategoryBackDTO;
import com.yuhui.blog.dto.CategoryDTO;
import com.yuhui.blog.dto.CategoryOptionDTO;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.util.PageUtils;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.PageResult;
import com.yuhui.blog.entity.Article;
import com.yuhui.blog.entity.Category;
import com.yuhui.blog.dao.CategoryDao;
import com.yuhui.blog.exception.BizException;
import com.yuhui.blog.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.vo.CategoryVO;
import org.omg.CORBA.SystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


/**
 * 分类服务
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, Category> implements CategoryService {
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private ArticleDao articleDao;

    @Override
    public PageResult<CategoryDTO> listCategories() {
        return new PageResult<>(categoryDao.listCategoryDTO(), categoryDao.selectCount(null));
    }

    @Override
    public PageResult<CategoryBackDTO> listBackCategories(ConditionVO condition) {
        // 1、查询分类数量
        Integer count = categoryDao.selectCount(new LambdaQueryWrapper<Category>()
                .like(StringUtils.isNotBlank(condition.getKeywords()), Category::getCategoryName, condition.getKeywords()));
        if (count == 0) {
            return new PageResult<>();
        }
        // 2、分页查询分类列表
        List<CategoryBackDTO> categoryList = categoryDao.listCategoryBackDTO(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        return new PageResult<>(categoryList, count);
    }

    @Override
    public List<CategoryOptionDTO> listCategoriesBySearch(ConditionVO condition) {
        // 搜索分类
        List<Category> categoryList = categoryDao.selectList(new LambdaQueryWrapper<Category>()
                .like(StringUtils.isNotBlank(condition.getKeywords()), Category::getCategoryName, condition.getKeywords())
                .orderByDesc(Category::getId));
        return BeanCopyUtils.copyList(categoryList, CategoryOptionDTO.class);
    }

    @Override
    public void deleteCategory(List<Integer> categoryIdList) {
        // 查询分类id下是否有文章
        Integer count = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .in(Article::getCategoryId, categoryIdList));
        if (count > 0) {
            throw new BizException("删除失败，该分类下存在文章");
        }
        categoryDao.deleteBatchIds(categoryIdList);
    }

    @Override
    public void saveOrUpdateCategory(CategoryVO categoryVO) {
        /*LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        if (count(queryWrapper.eq(Category::getCategoryName, categoryVO.getCategoryName())) > 0) {
            throw new BizException("分类名已存在");
        }*/
        // 1、根据分类名查找分类id
        // 无论新增还是编辑，分类名重复，就能查到id值
        Category existCategory = categoryDao.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getCategoryName, categoryVO.getCategoryName()));
        // 2、重复的两种情况：
        // 2.1、新增：categoryVO里面id为null，分类名一样
        // 2.2、编辑：categoryVO里面id有值，分类名一样并且查到的id和自身id不一样，如果一样的话，就算作是没做任何修改直接保存
        if (Objects.nonNull(existCategory) && !existCategory.getId().equals(categoryVO.getId())) {// 187   189
            throw new BizException("分类名已存在");
        }
        // 不重复直接保存
        Category category = Category.builder()
                .id(categoryVO.getId())
                .categoryName(categoryVO.getCategoryName())
                .build();
        this.saveOrUpdate(category);
    }

}
