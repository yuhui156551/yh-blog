package com.yuhui.blog.service.impl;

import com.yuhui.blog.entity.ArticleTag;
import com.yuhui.blog.dao.ArticleTagDao;
import com.yuhui.blog.service.ArticleTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 文章标签服务
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagDao, ArticleTag> implements ArticleTagService {

}
