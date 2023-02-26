package com.yuhui.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章状态枚举
 */
@Getter
@AllArgsConstructor
public enum ArticleStatusEnum {
    /**
     * 公开
     */
    PUBLIC(1, "公开"),
    /**
     * 私密
     */
    SECRET(2, "私密"),
    /**
     * 草稿
     */
    DRAFT(3, "草稿");
    // 构造方法默认为private，枚举被设计成是单例模式，JVM为了保证每一个枚举类元素的唯一实例，是不会允许外部进行new的，
    // 所以会把构造函数设计成private，防止用户生成实例，破坏唯一性。
    /**
     * 状态
     */
    private final Integer status;

    /**
     * 描述
     */
    private final String desc;

}
