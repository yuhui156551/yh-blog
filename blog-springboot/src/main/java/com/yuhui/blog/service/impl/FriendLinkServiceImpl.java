package com.yuhui.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuhui.blog.dto.FriendLinkBackDTO;
import com.yuhui.blog.dto.FriendLinkDTO;
import com.yuhui.blog.util.PageUtils;
import com.yuhui.blog.vo.ConditionVO;
import com.yuhui.blog.vo.PageResult;
import com.yuhui.blog.entity.FriendLink;
import com.yuhui.blog.dao.FriendLinkDao;
import com.yuhui.blog.service.FriendLinkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.vo.FriendLinkVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 友情链接服务
 */
@Service
public class FriendLinkServiceImpl extends ServiceImpl<FriendLinkDao, FriendLink> implements FriendLinkService {
    @Autowired
    private FriendLinkDao friendLinkDao;

    @Override
    public List<FriendLinkDTO> listFriendLinks() {
        // 查询友链列表
        List<FriendLink> friendLinkList = friendLinkDao.selectList(null);
        return BeanCopyUtils.copyList(friendLinkList, FriendLinkDTO.class);
    }

    @Override
    public PageResult<FriendLinkBackDTO> listFriendLinkDTO(ConditionVO condition) {
        // 1、分页查询友链
        Page<FriendLink> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<FriendLink> linkPage = page(page, new LambdaQueryWrapper<FriendLink>().like(StringUtils.isNotBlank(condition.getKeywords()), FriendLink::getLinkName, condition.getKeywords()));
        // 2、转换dto
        return new PageResult<>(BeanCopyUtils.copyList(linkPage.getRecords(), FriendLinkBackDTO.class), (int) linkPage.getTotal());
        // 上下两种实现都是一样的效果
       /* Page<FriendLink> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<FriendLink> friendLinkPage = friendLinkDao.selectPage(page, new LambdaQueryWrapper<FriendLink>()
                .like(StringUtils.isNotBlank(condition.getKeywords()), FriendLink::getLinkName, condition.getKeywords()));
        // 转换DTO
        List<FriendLinkBackDTO> friendLinkBackDTOList = BeanCopyUtils.copyList(friendLinkPage.getRecords(), FriendLinkBackDTO.class);
        return new PageResult<>(friendLinkBackDTOList, (int) friendLinkPage.getTotal());*/
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO) {
        FriendLink friendLink = BeanCopyUtils.copyObject(friendLinkVO, FriendLink.class);
        this.saveOrUpdate(friendLink);
    }

}
