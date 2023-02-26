package com.yuhui.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuhui.blog.dto.MessageBackDTO;
import com.yuhui.blog.service.BlogInfoService;
import com.yuhui.blog.util.HTMLUtils;
import com.yuhui.blog.util.PageUtils;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.PageResult;
import com.yuhui.blog.vo.MessageVO;
import com.yuhui.blog.dto.MessageDTO;
import com.yuhui.blog.entity.Message;
import com.yuhui.blog.dao.MessageDao;
import com.yuhui.blog.service.MessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.util.IpUtils;
import com.yuhui.blog.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.yuhui.blog.constant.CommonConst.FALSE;
import static com.yuhui.blog.constant.CommonConst.TRUE;

/**
 * 留言服务
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageDao, Message> implements MessageService {
    @Autowired
    private MessageDao messageDao;
    @Resource
    private HttpServletRequest request;
    @Autowired
    private BlogInfoService blogInfoService;

    @Override
    public void saveMessage(MessageVO messageVO) {
        // 1、判断是否需要审核
        Integer isReview = blogInfoService.getWebsiteConfig().getIsMessageReview();
        // 2、获取用户ip
        String ipAddress = IpUtils.getIpAddress(request);
        String ipSource = IpUtils.getIpSource(ipAddress);
        // 3、保存message
        Message message = BeanCopyUtils.copyObject(messageVO, Message.class);
        message.setMessageContent(HTMLUtils.filter(message.getMessageContent()));
        message.setIpAddress(ipAddress);
        message.setIsReview(isReview == TRUE ? FALSE : TRUE);// 需要审核->设置为未审核状态
        message.setIpSource(ipSource);
        messageDao.insert(message);
    }

    @Override
    public List<MessageDTO> listMessages() {
        // 查询留言列表
        List<Message> messageList = messageDao.selectList(new LambdaQueryWrapper<Message>()
                .select(Message::getId, Message::getNickname, Message::getAvatar, Message::getMessageContent, Message::getTime)
                .eq(Message::getIsReview, TRUE));
        return BeanCopyUtils.copyList(messageList, MessageDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateMessagesReview(ReviewVO reviewVO) {
        // 修改留言审核状态（updateBatchById：id + isReview）
        List<Message> messageList = reviewVO.getIdList().stream().map(item -> Message.builder()
                        .id(item)
                        .isReview(reviewVO.getIsReview())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(messageList);
    }

    @Override
    public PageResult<MessageBackDTO> listMessageBackDTO(ConditionVO condition) {
        // 1、分页查询留言列表
        Page<Message> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<Message> messagePage = messageDao.selectPage(page, new LambdaQueryWrapper<Message>()
                .like(StringUtils.isNotBlank(condition.getKeywords()), Message::getNickname, condition.getKeywords())
                .eq(Objects.nonNull(condition.getIsReview()), Message::getIsReview, condition.getIsReview())
                .orderByDesc(Message::getId));
        // 2、转换DTO
        List<MessageBackDTO> messageBackDTOList = BeanCopyUtils.copyList(messagePage.getRecords(), MessageBackDTO.class);
        return new PageResult<>(messageBackDTOList, (int) messagePage.getTotal());
    }

}
