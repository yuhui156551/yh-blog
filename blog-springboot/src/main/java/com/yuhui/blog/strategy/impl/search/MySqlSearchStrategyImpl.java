package com.yuhui.blog.strategy.impl.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.yuhui.blog.dao.ArticleDao;
import com.yuhui.blog.dto.ArticleSearchDTO;
import com.yuhui.blog.entity.Article;
import com.yuhui.blog.strategy.SearchStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yuhui.blog.constant.CommonConst.*;
import static com.yuhui.blog.enums.ArticleStatusEnum.PUBLIC;

/**
 * mysql搜索策略
 */
@Service("mySqlSearchStrategyImpl")
public class MySqlSearchStrategyImpl implements SearchStrategy {
    @Autowired
    private ArticleDao articleDao;

    @Override
    public List<ArticleSearchDTO> searchArticle(String keywords) {
        // 1、判空（即使搜索栏为空也会发送请求）
        if (StringUtils.isBlank(keywords)) {
            return new ArrayList<>();
        }
        // 2、搜索文章
        List<Article> articleList = articleDao.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus())
                .and(lqw -> lqw.like(Article::getArticleTitle, keywords)
                        .or()
                        .like(Article::getArticleContent, keywords)));
        // 3、每一个包含关键词的地方都进行高亮处理
        return articleList.stream().map(item -> {
            // 3.1、文章内容高亮
            // 获取关键词第一次出现的位置
            String articleContent = item.getArticleContent();
            int index = item.getArticleContent().indexOf(keywords);
            // 返回-1 说明关键词不在文章内容中
            if (index != -1) {
                /*// 获取关键词前面的文字
                int preIndex = index > 25 ? index - 25 : 0;
                String preText = item.getArticleContent().substring(preIndex, index);
                // 获取关键词到后面的文字
                int last = index + keywords.length();
                int postLength = item.getArticleContent().length() - last;
                int postIndex = postLength > 175 ? last + 175 : last + postLength;
                String postText = item.getArticleContent().substring(index, postIndex);
                // 文章内容高亮
                // 恭喜你成功运行博客，开启你的文章<span style='color:#f47466'>之</span>旅吧。
                articleContent = (preText + postText).replaceAll(keywords, PRE_TAG + keywords + POST_TAG);*/
                articleContent = articleContent.replaceAll(keywords, PRE_TAG + keywords + POST_TAG);
            }
            // 3.2、文章标题高亮
            // 有关键词的标题会替换成这种效果：<span style='color:#f47466'>测</span>试文章
            // 若没有关键词，则不影响
            String articleTitle = item.getArticleTitle().replaceAll(keywords, PRE_TAG + keywords + POST_TAG);
            return ArticleSearchDTO.builder()
                    .id(item.getId())
                    .articleTitle(articleTitle)
                    .articleContent(articleContent)
                    .build();
        }).collect(Collectors.toList());
    }

}
