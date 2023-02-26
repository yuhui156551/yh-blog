package com.yuhui.blog.strategy.impl.upload;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.yuhui.blog.config.OssConfigProperties;
import com.yuhui.blog.strategy.impl.upload.AbstractUploadStrategyImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * oss上传策略实现
 */
@Service("ossUploadStrategyImpl")
public class OssUploadStrategyImpl extends AbstractUploadStrategyImpl {
    @Autowired
    private OssConfigProperties ossConfigProperties;

    @Override
    public Boolean exists(String filePath) {
        // 判断文件是否存在。如果返回值为true，则文件存在，否则存储空间或者文件不存在。
        return getOssClient().doesObjectExist(ossConfigProperties.getBucketName(), filePath);
    }

    @Override
    public void upload(String path, String fileName, InputStream inputStream) {
        // 通过流式上传的方式将文件上传到OSS
        getOssClient().putObject(ossConfigProperties.getBucketName(), path + fileName, inputStream);
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return ossConfigProperties.getUrl() + filePath;
    }

    /**
     * 获取ossClient
     *
     * @return {@link OSS} ossClient
     */
    private OSS getOssClient() {
        return new OSSClientBuilder().build(
                ossConfigProperties.getEndpoint(),
                ossConfigProperties.getAccessKeyId(),
                ossConfigProperties.getAccessKeySecret());
    }

}
