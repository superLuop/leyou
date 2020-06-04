package com.leyou.upload.service.impl;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.upload.config.UploadProperties;
import com.leyou.upload.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

    @Autowired
    private FastFileStorageClient storageClient;

    @Autowired
    private UploadProperties prop;

//    private static final Logger logger= LoggerFactory.getLogger(UploadServiceImpl.class);

    @Override
    public String uploadImage(MultipartFile file) {
        /**
         * 1.图片信息校验
         *      1)校验文件类型
         *      2)校验图片内容
         * 2.保存图片
         *      1)生成保存目录
         *      2)保存图片
         *      3)拼接图片地址
         */
        try {
            String type = file.getContentType();
            if (!prop.getSuffixes().contains(type)){
//                logger.info("上传文件失败，文件类型不匹配：{}", type);
                throw new LyException(ExceptionEnum.INVALID_FILE_FORMAT);
            }
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null){
//                logger.info("上传失败，文件内容不符合要求");
                throw new LyException(ExceptionEnum.INVALID_FILE_FORMAT);
            }

            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);
            return prop.getBaseUrl() + storePath.getFullPath();
        }catch (IOException e){
            log.error("上传失败!", e);
            throw new LyException(ExceptionEnum.UPLOAD_IMAGE_EXCEPTION);
        }
    }
}
