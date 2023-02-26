package com.yuhui.blog.service.impl;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.yuhui.blog.dto.UniqueViewDTO;
import com.yuhui.blog.entity.UniqueView;
import com.yuhui.blog.dao.UniqueViewDao;
import com.yuhui.blog.service.RedisService;
import com.yuhui.blog.service.UniqueViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.yuhui.blog.constant.RedisPrefixConst.UNIQUE_VISITOR;
import static com.yuhui.blog.constant.RedisPrefixConst.VISITOR_AREA;
import static com.yuhui.blog.enums.ZoneEnum.SHANGHAI;


/**
 * 访问量统计服务
 */
@Service
public class UniqueViewServiceImpl extends ServiceImpl<UniqueViewDao, UniqueView> implements UniqueViewService {
    @Autowired
    private RedisService redisService;
    @Autowired
    private UniqueViewDao uniqueViewDao;

    @Override
    public List<UniqueViewDTO> listUniqueViews() {
        DateTime startTime = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -7));
        DateTime endTime = DateUtil.endOfDay(new Date());
        // 获取七天访问量
        return uniqueViewDao.listUniqueViews(startTime, endTime);
    }

    @Scheduled(cron = " 0 0 0 * * ?", zone = "Asia/Shanghai")// 每天0点执行一次
    public void saveUniqueView() {
        // 获取每天用户量
        Long count = redisService.sSize(UNIQUE_VISITOR);// 0
        // 获取昨天日期，插入数据
        UniqueView uniqueView = UniqueView.builder()
                .createTime(LocalDateTimeUtil.offset(LocalDateTime.now(ZoneId.of(SHANGHAI.getZone())), -1, ChronoUnit.DAYS))
                // ofNullable() 可以支持null值，of()不支持null值
                .viewsCount(Optional.of(count.intValue()).orElse(0))
                .build();
        uniqueViewDao.insert(uniqueView);
    }

    @Scheduled(cron = " 0 1 0 * * ?", zone = "Asia/Shanghai")// 每天1点执行一次，zone表示执行时间时区
    public void clear() {
        // 清空redis访客记录
        redisService.del(UNIQUE_VISITOR);
        // 清空redis游客区域统计
        redisService.del(VISITOR_AREA);
    }

    /*public static void main(String[] args) {
        Long count = 0L;
        System.out.println(Optional.of(count.intValue()).orElse(0));
    }*/

}
