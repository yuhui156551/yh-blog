package com.yuhui.blog.dao;

import com.yuhui.blog.dto.PhotoAlbumBackDTO;
import com.yuhui.blog.entity.PhotoAlbum;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuhui.blog.vo.ConditionVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * 相册映射器
 */
@Repository
public interface PhotoAlbumDao extends BaseMapper<PhotoAlbum> {

    /**
     * 查询后台相册列表
     *
     * @param current   页码
     * @param size      大小
     * @param condition 条件
     * @return {@link List < PhotoAlbumBackDTO >} 相册列表
     */
    List<PhotoAlbumBackDTO> listPhotoAlbumBacks(@Param("current") Long current, @Param("size") Long size, @Param("condition") ConditionVO condition);

}




