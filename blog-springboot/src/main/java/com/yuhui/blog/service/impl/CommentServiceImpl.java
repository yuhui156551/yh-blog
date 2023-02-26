package com.yuhui.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.yuhui.blog.dao.ArticleDao;
import com.yuhui.blog.dao.TalkDao;
import com.yuhui.blog.dao.UserInfoDao;
import com.yuhui.blog.dto.*;
import com.yuhui.blog.entity.Comment;
import com.yuhui.blog.dao.CommentDao;
import com.yuhui.blog.service.BlogInfoService;
import com.yuhui.blog.service.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.service.RedisService;
import com.yuhui.blog.util.HTMLUtils;
import com.yuhui.blog.util.PageUtils;
import com.yuhui.blog.util.UserUtils;
import com.yuhui.blog.vo.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.yuhui.blog.constant.CommonConst.*;
import static com.yuhui.blog.constant.MQPrefixConst.EMAIL_EXCHANGE;
import static com.yuhui.blog.constant.RedisPrefixConst.COMMENT_LIKE_COUNT;
import static com.yuhui.blog.constant.RedisPrefixConst.COMMENT_USER_LIKE;
import static com.yuhui.blog.enums.CommentTypeEnum.*;

/**
 * 评论服务
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentDao, Comment> implements CommentService {
    @Autowired
    private CommentDao commentDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private TalkDao talkDao;
    @Autowired
    private RedisService redisService;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private BlogInfoService blogInfoService;

    /**
     * 网站网址
     */
    @Value("${website.url}")
    private String websiteUrl;

    @Override
    public PageResult<CommentDTO> listComments(CommentVO commentVO) {
        // 1、判断是否有评论，无则直接返回
        Integer commentCount = commentDao.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Objects.nonNull(commentVO.getTopicId()), Comment::getTopicId, commentVO.getTopicId())
                .eq(Comment::getType, commentVO.getType())
                .isNull(Comment::getParentId)// 父评论id为null
                .eq(Comment::getIsReview, TRUE));
        if (commentCount == 0) {
            return new PageResult<>();
        }
        // 2、分页查询所有评论数据
        List<CommentDTO> commentDTOList = commentDao.listComments(PageUtils.getLimitCurrent(), PageUtils.getSize(), commentVO);
        if (CollectionUtils.isEmpty(commentDTOList)) {
            return new PageResult<>();
        }
        // 2.1、查询redis的评论点赞数据
        Map<String, Object> likeCountMap = redisService.hGetAll(COMMENT_LIKE_COUNT);
        // 3、提取评论id集合
        List<Integer> commentIdList = commentDTOList.stream()
                .map(CommentDTO::getId)
                .collect(Collectors.toList());
        // 4、根据评论id集合查询所有回复数据
        List<ReplyDTO> replyDTOList = commentDao.listReplies(commentIdList);
        // 4.1、封装回复点赞量
        replyDTOList.forEach(item -> item.setLikeCount((Integer) likeCountMap.get(item.getId().toString())));
        // 4.2、根据评论id分组回复数据
        Map<Integer, List<ReplyDTO>> replyMap = replyDTOList.stream()
                .collect(Collectors.groupingBy(ReplyDTO::getParentId));
        // 4.3、根据评论id查询回复量
        Map<Integer, Integer> replyCountMap = commentDao.listReplyCountByCommentId(commentIdList)
                .stream().collect(Collectors.toMap(ReplyCountDTO::getCommentId, ReplyCountDTO::getReplyCount));
        // 5、封装评论数据
        commentDTOList.forEach(item -> {
            item.setLikeCount((Integer) likeCountMap.get(item.getId().toString()));
            item.setReplyDTOList(replyMap.get(item.getId()));
            item.setReplyCount(replyCountMap.get(item.getId()));
        });
        return new PageResult<>(commentDTOList, commentCount);
    }

    @Override
    public List<ReplyDTO> listRepliesByCommentId(Integer commentId) {
        // 转换页码查询评论下的回复
        List<ReplyDTO> replyDTOList = commentDao.listRepliesByCommentId(PageUtils.getLimitCurrent(), PageUtils.getSize(), commentId);
        // 查询redis的评论点赞数据
        Map<String, Object> likeCountMap = redisService.hGetAll(COMMENT_LIKE_COUNT);
        // 封装点赞数据
        replyDTOList.forEach(item -> item.setLikeCount((Integer) likeCountMap.get(item.getId().toString())));
        return replyDTOList;
    }

    @Override
    public void saveComment(CommentVO commentVO) {
        // 1、判断评论是否需要审核
        WebsiteConfigVO websiteConfig = blogInfoService.getWebsiteConfig();
        Integer isReview = websiteConfig.getIsCommentReview();
        // 2、过滤评论
        commentVO.setCommentContent(HTMLUtils.filter(commentVO.getCommentContent()));
        Comment comment = Comment.builder()
                .userId(UserUtils.getLoginUser().getUserInfoId())
                .replyUserId(commentVO.getReplyUserId())
                .topicId(commentVO.getTopicId())
                .commentContent(commentVO.getCommentContent())
                .parentId(commentVO.getParentId())
                .type(commentVO.getType())
                .isReview(isReview == TRUE ? FALSE : TRUE)// 如果评论需要审核，赋予false代表未审核状态
                .build();
        commentDao.insert(comment);
        // 3、判断是否开启邮箱通知,通知用户
        if (websiteConfig.getIsEmailNotice().equals(TRUE)) {
            CompletableFuture.runAsync(() -> notice(comment));
        }
    }

    @Override
    public void saveCommentLike(Integer commentId) {
        // 判断是否点赞
        String commentLikeKey = COMMENT_USER_LIKE + UserUtils.getLoginUser().getUserInfoId();
        if (redisService.sIsMember(commentLikeKey, commentId)) {
            // 点过赞则删除评论id
            redisService.sRemove(commentLikeKey, commentId);
            // 评论点赞量-1
            redisService.hDecr(COMMENT_LIKE_COUNT, commentId.toString(), 1L);
        } else {
            // 未点赞则增加评论id
            redisService.sAdd(commentLikeKey, commentId);
            // 评论点赞量+1
            redisService.hIncr(COMMENT_LIKE_COUNT, commentId.toString(), 1L);
        }
    }

    @Override
    public void updateCommentsReview(ReviewVO reviewVO) {
        List<Comment> commentList = reviewVO.getIdList().stream().map(item -> Comment.builder()
                        .id(item)
                        .isReview(reviewVO.getIsReview())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(commentList);
    }

    @Override
    public PageResult<CommentBackDTO> listCommentBackDTO(ConditionVO condition) {
        // 1、统计后台评论量
        Integer count = commentDao.countCommentDTO(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // 2、查询后台评论集合
        List<CommentBackDTO> commentBackDTOList = commentDao.listCommentBackDTO(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        return new PageResult<>(commentBackDTOList, count);
    }

    /**
     * 通知评论用户
     *
     * @param comment 评论信息
     */
    public void notice(Comment comment) {
        Integer userId = BLOGGER_ID;
        String topicId = Objects.nonNull(comment.getTopicId()) ? comment.getTopicId().toString() : "";
        // 1、获取被评论用户的id、或博主的id
        // 1.1、回复的是用户
        if (Objects.nonNull(comment.getReplyUserId())) {
            userId = comment.getReplyUserId();
        } else {
            // 1.2、回复的是文章、说说或友链（友链的博主id就是1）
            switch (Objects.requireNonNull(getCommentEnum(comment.getType()))) {
                case ARTICLE:
                    userId = articleDao.selectById(comment.getTopicId()).getUserId();
                    break;
                case TALK:
                    userId = talkDao.selectById(comment.getTopicId()).getUserId();
                    break;
                default:
                    break;
            }
        }
        // 2、获取回复用户邮箱号
        String email = userInfoDao.selectById(userId).getEmail();
        if (StringUtils.isNotBlank(email)) {
            // 3、准备消息
            EmailDTO emailDTO = new EmailDTO();
            // 3.1、评论不是审核状态
            if (comment.getIsReview().equals(TRUE)) {
                emailDTO.setEmail(email);
                emailDTO.setSubject("评论提醒");
                // 获取评论路径
                String url = websiteUrl + getCommentPath(comment.getType()) + topicId;// websiteUrl + "/articles/" + id
                emailDTO.setContent("您收到了一条新的回复，请前往 " + url + " 页面查看");
            } else {
                // 3.2、评论是审核状态
                String adminEmail = userInfoDao.selectById(BLOGGER_ID).getEmail();
                emailDTO.setEmail(adminEmail);
                emailDTO.setSubject("审核提醒");
                emailDTO.setContent("您收到了一条新的回复，请前往后台管理页面审核");
            }
            // 4、发送消息
            rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, "*", new Message(JSON.toJSONBytes(emailDTO), new MessageProperties()));
        }
    }

}
