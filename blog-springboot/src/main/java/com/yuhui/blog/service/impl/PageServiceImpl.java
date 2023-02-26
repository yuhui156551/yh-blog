package com.yuhui.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.dao.PageDao;
import com.yuhui.blog.entity.Page;
import com.yuhui.blog.service.PageService;
import com.yuhui.blog.service.RedisService;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Objects;

import static com.yuhui.blog.constant.RedisPrefixConst.PAGE_COVER;

/**
 * 页面服务
 */
@Service
public class PageServiceImpl extends ServiceImpl<PageDao, Page> implements PageService {
    @Autowired
    private RedisService redisService;
    @Autowired
    private PageDao pageDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdatePage(PageVO pageVO) {
        Page page = BeanCopyUtils.copyObject(pageVO, Page.class);
        this.saveOrUpdate(page);
        // 删除缓存
        redisService.del(PAGE_COVER);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePage(Integer pageId) {
        pageDao.deleteById(pageId);
        // 删除缓存
        redisService.del(PAGE_COVER);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<PageVO> listPages() {
        List<PageVO> pageVOList;
        // 删除或更新页面信息，都会将redis里的page信息删除，重新到mysql获取，保持前后端数据一致
        Object pageList = redisService.get(PAGE_COVER);
        if (Objects.nonNull(pageList)) {
            pageVOList = JSON.parseObject(pageList.toString(), List.class);
        } else {
            pageVOList = BeanCopyUtils.copyList(pageDao.selectList(null), PageVO.class);
            redisService.set(PAGE_COVER, JSON.toJSONString(pageVOList));
        }
        return pageVOList;
    }

}




