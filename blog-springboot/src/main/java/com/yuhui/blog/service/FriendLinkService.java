package com.yuhui.blog.service;

import com.yuhui.blog.dto.FriendLinkBackDTO;
import com.yuhui.blog.dto.FriendLinkDTO;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.PageResult;
import com.yuhui.blog.entity.FriendLink;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yuhui.blog.vo.FriendLinkVO;

import java.util.List;

/**
 * 友链服务
 */
public interface FriendLinkService extends IService<FriendLink> {

    /**
     * 查看友链列表
     *
     * @return 友链列表
     */
    List<FriendLinkDTO> listFriendLinks();

    /**
     * 查看后台友链列表
     *
     * @param condition 条件
     * @return 友链列表
     */
    PageResult<FriendLinkBackDTO> listFriendLinkDTO(ConditionVO condition);

    /**
     * 保存或更新友链
     *
     * @param friendLinkVO 友链
     */
    void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO);

}
