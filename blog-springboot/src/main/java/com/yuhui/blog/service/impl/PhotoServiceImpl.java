package com.yuhui.blog.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuhui.blog.dao.PhotoDao;
import com.yuhui.blog.dto.PhotoBackDTO;
import com.yuhui.blog.dto.PhotoDTO;
import com.yuhui.blog.entity.Photo;
import com.yuhui.blog.entity.PhotoAlbum;
import com.yuhui.blog.exception.BizException;
import com.yuhui.blog.service.PhotoAlbumService;
import com.yuhui.blog.service.PhotoService;
import com.yuhui.blog.util.BeanCopyUtils;
import com.yuhui.blog.util.PageUtils;
import com.yuhui.blog.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yuhui.blog.constant.CommonConst.FALSE;
import static com.yuhui.blog.enums.PhotoAlbumStatusEnum.PUBLIC;

/**
 * 照片服务
 */
@Service
public class PhotoServiceImpl extends ServiceImpl<PhotoDao, Photo> implements PhotoService {
    @Autowired
    private PhotoDao photoDao;
    @Autowired
    private PhotoAlbumService photoAlbumService;

    @Override
    public PageResult<PhotoBackDTO> listPhotos(ConditionVO condition) {
        // 分页查询照片列表
        Page<Photo> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<Photo> photoPage = photoDao.selectPage(page, new LambdaQueryWrapper<Photo>()
                .eq(Objects.nonNull(condition.getAlbumId()), Photo::getAlbumId, condition.getAlbumId())
                .eq(Photo::getIsDelete, condition.getIsDelete())
                .orderByDesc(Photo::getId)
                .orderByDesc(Photo::getUpdateTime));
        List<PhotoBackDTO> photoList = BeanCopyUtils.copyList(photoPage.getRecords(), PhotoBackDTO.class);
        return new PageResult<>(photoList, (int) photoPage.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhoto(PhotoInfoVO photoInfoVO) {
        Photo photo = BeanCopyUtils.copyObject(photoInfoVO, Photo.class);
        photoDao.updateById(photo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void savePhotos(PhotoVO photoVO) {
        List<Photo> photoList = photoVO.getPhotoUrlList().stream().map(item -> Photo.builder()
                        .albumId(photoVO.getAlbumId())
                        .photoName(IdWorker.getIdStr())
                        .photoSrc(item)
                        .build())
                .collect(Collectors.toList());
        this.saveBatch(photoList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhotosAlbum(PhotoVO photoVO) {
        /*Photo photo = new Photo();
        List<Photo> photoList = photoVO.getPhotoIdList().stream()
                .map(item -> photo.setId(item).setAlbumId(photoVO.getAlbumId())).collect(Collectors.toList());*/
        List<Photo> photoList = photoVO.getPhotoIdList().stream().map(item -> Photo.builder()
                        .id(item)
                        .albumId(photoVO.getAlbumId())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(photoList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhotoDelete(DeleteVO deleteVO) {
        // 1、更新照片状态
        List<Photo> photoList = deleteVO.getIdList().stream().map(item -> Photo.builder()
                        .id(item)
                        .isDelete(deleteVO.getIsDelete())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(photoList);
        // 若是恢复照片，进入此方法
        if (deleteVO.getIsDelete().equals(FALSE)) {
            // 2、数据库查找照片id所对应的相册id
            List<PhotoAlbum> photoAlbumList = photoDao.selectList(new LambdaQueryWrapper<Photo>()
                            .select(Photo::getAlbumId)
                            .in(Photo::getId, deleteVO.getIdList())
                            .groupBy(Photo::getAlbumId))
                    .stream()
                    // 3、无论照片对应相册有没有被删除，都设置false
                    .map(item -> PhotoAlbum.builder()
                            .id(item.getAlbumId())
                            .isDelete(FALSE)
                            .build())
                    .collect(Collectors.toList());
            photoAlbumService.updateBatchById(photoAlbumList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePhotos(List<Integer> photoIdList) {
        photoDao.deleteBatchIds(photoIdList);
    }

    @Override
    public PhotoDTO listPhotosByAlbumId(Integer albumId) {
        // 1、查询相册信息
        PhotoAlbum photoAlbum = photoAlbumService.getOne(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getId, albumId)
                .eq(PhotoAlbum::getIsDelete, FALSE)
                .eq(PhotoAlbum::getStatus, PUBLIC.getStatus()));
        if (Objects.isNull(photoAlbum)) {
            throw new BizException("相册不存在");
        }
        // 2、分页查询照片列表
        Page<Photo> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<Photo> photoPage = photoDao.selectPage(page, new LambdaQueryWrapper<Photo>()
                .select(Photo::getPhotoSrc)// 为什么查单字段不查*，都是为了性能
                .eq(Photo::getAlbumId, albumId)
                .eq(Photo::getIsDelete, FALSE)
                .orderByDesc(Photo::getId));
        List<String> photoList = photoPage.getRecords().stream().map(Photo::getPhotoSrc).collect(Collectors.toList());
        return PhotoDTO.builder()
                .photoAlbumCover(photoAlbum.getAlbumCover())
                .photoAlbumName(photoAlbum.getAlbumName())
                .photoList(photoList)
                .build();
    }

}




