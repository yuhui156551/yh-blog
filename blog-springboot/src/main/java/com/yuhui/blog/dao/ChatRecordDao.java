package com.yuhui.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuhui.blog.entity.ChatRecord;
import org.springframework.stereotype.Repository;

/**
 * 聊天记录
 */
@Repository
public interface ChatRecordDao extends BaseMapper<ChatRecord> {
}
