package com.example.minio.controller;

import com.example.minio.dto.FileInfo;
import com.example.minio.utils.MinioUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/file")
public class FileController {

    @Resource
    MinioUtil minioUtil;

    /**
     * 上传一个文件
     * @param uploadfile
     * @param bucket
     * @param objectName
     * @return
     */
    @PostMapping(value = "/uploadfile")
    public ResultBody fileupload(@RequestParam MultipartFile uploadfile, @RequestParam String bucket,
                                 @RequestParam(required = false) String objectName) {
        try {
            minioUtil.createBucket(bucket);
            if (objectName != null) {
                minioUtil.uploadFile(uploadfile.getInputStream(), bucket, objectName + "/" + uploadfile.getOriginalFilename());
            } else {
                minioUtil.uploadFile(uploadfile.getInputStream(), bucket, uploadfile.getOriginalFilename());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultBody.error("上传文件出错，请稍后再试");
        }
        return ResultBody.success();
    }

    /**
     * 列出所有的桶
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/listBuckets")
    public ResultBody listBuckets() throws Exception {
        List<String> strings = minioUtil.listBuckets();
        return ResultBody.success(strings);
    }

    /**
     * 递归列出一个桶中的所有文件和目录
     * @param bucket
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/listFiles")
    public ResultBody listFiles(@RequestParam(value = "bucket") String bucket) throws Exception {
        List<FileInfo> fileInfos = minioUtil.listFiles(bucket);
        return ResultBody.success(fileInfos);
    }

    /**
     * 下载一个文件
     * @param bucket
     * @param filename
     * @param response
     * @throws Exception
     */
    @GetMapping(value = "/downloadFile")
    public void downloadFile(@RequestParam String bucket, @RequestParam String filename,
                             HttpServletResponse response) throws Exception {
        InputStream inputStream = minioUtil.download(bucket, filename);
        try{
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
            byte[] bytes = new byte[1024];
            int len;
            while ((len = inputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, len);
            }
            outputStream.close();
        } catch (Exception e) {
            log.error("file download from minio exception, file name: {}", filename,  e);
        }

    }


    /**
     * 删除一个文件
     * @param bucket
     * @param objectName
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/deleteFile")
    public ResultBody deleteFile(@RequestParam String bucket, @RequestParam String objectName) throws Exception {
        minioUtil.deleteObject(bucket, objectName);
        return ResultBody.success();
    }

    /**
     * 删除一个桶
     * @param bucket
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/deleteBucket")
    public ResultBody deleteBucket(@RequestParam String bucket) throws Exception {
        minioUtil.deleteBucket(bucket);
        return ResultBody.success();
    }


    /**
     * 复制一个文件
     * @param sourceBucket
     * @param sourceObject
     * @param targetBucket
     * @param targetObject
     * @return
     * @throws Exception
     */
    @GetMapping("/copyObject")
    public ResultBody copyObject(@RequestParam String sourceBucket, @RequestParam String sourceObject, @RequestParam String targetBucket, @RequestParam String targetObject) throws Exception {
        minioUtil.copyObject(sourceBucket, sourceObject, targetBucket, targetObject);
        return ResultBody.success();
    }

    /**
     * 获取文件信息
     * @param bucket
     * @param objectName
     * @return
     * @throws Exception
     */
    @GetMapping("/getObjectInfo")
    public ResultBody getObjectInfo(@RequestParam String bucket, @RequestParam String objectName) throws Exception {
        String objectInfo = minioUtil.getObjectInfo(bucket, objectName);
        return ResultBody.success(objectInfo);
    }

    /**
     * 获取一个连接以供下载
     * @param bucket
     * @param objectName
     * @param expires
     * @return
     * @throws Exception
     */
    @GetMapping("/getPresignedObjectUrl")
    public ResultBody getPresignedObjectUrl(@RequestParam String bucket, @RequestParam String objectName, @RequestParam Integer expires) throws Exception {
        String presignedObjectUrl = minioUtil.getPresignedObjectUrl(bucket, objectName, expires);
        return ResultBody.success(presignedObjectUrl);
    }

    /**
     * 获取minio中所有的文件
     * @return
     * @throws Exception
     */
    @GetMapping("/listAllFile")
    public ResultBody listAllFile() throws Exception {
        List<FileInfo> fileInfos = minioUtil.listAllFile();
        return ResultBody.success(fileInfos);
    }


}
