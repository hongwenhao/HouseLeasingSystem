package com.houseleasing.controller;

import com.houseleasing.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * @author HouseLeasingSystem开发团队
 * @description 处理图片文件的上传请求，将图片保存到服务器磁盘，
 *              返回可供前端访问的图片 URL，避免将图片二进制数据存入数据库。
 *
 *              上传的图片保存在 app.upload.dir 配置目录下，
 *              通过 /api/uploads/{filename} 路径对外提供访问。
 */
@Slf4j
@Tag(name = "FileUpload", description = "File upload endpoints")
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /**
     * 图片上传根目录，从 application.yml 的 app.upload.dir 读取，
     * 默认为相对路径 "uploads"（即应用运行目录下的 uploads 文件夹）
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * 上传图片接口
     *
     * <p>接收前端通过 multipart/form-data 提交的图片文件，
     * 执行格式校验和大小限制后保存到磁盘，返回可访问的图片 URL。</p>
     *
     * <p>支持的图片格式：jpg / jpeg / png / gif / webp</p>
     * <p>最大文件大小：10 MB（由 Spring multipart 配置限制）</p>
     *
     * @param file 前端上传的图片文件（表单字段名 "file"）
     * @return 上传成功后的图片访问 URL（如 /api/uploads/abc123.jpg）
     */
    @Operation(summary = "Upload image", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 获取原始文件名，校验合法性
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return Result.error("文件名不合法");
        }

        // 安全校验：拒绝包含路径分隔符的文件名，防止路径遍历攻击
        // 即使后续使用 UUID 重命名，也应在最早的时机拒绝可疑输入
        if (originalFilename.contains("/") || originalFilename.contains("\\")
                || originalFilename.contains("..")) {
            return Result.error("文件名包含非法字符");
        }

        // 提取文件扩展名并校验是否为允许的图片格式
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return Result.error("不支持的图片格式，仅允许 jpg/jpeg/png/gif/webp");
        }

        // 校验 MIME 类型：防止攻击者伪造扩展名上传非图片文件
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("文件类型不合法，仅允许上传图片");
        }

        // 使用 UUID 重命名文件，防止文件名冲突及路径遍历安全风险
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保上传目录存在，若不存在则自动创建（含多级父目录）
        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建上传目录失败：{}", dir.getAbsolutePath());
            return Result.error("服务器存储目录初始化失败，请稍后重试");
        }

        // 将上传的文件写入磁盘
        // 使用 Files.copy 代替 transferTo，避免 Servlet Part.write() 将相对路径
        // 解析到 Tomcat 工作目录（而非期望的上传目录）导致 FileNotFoundException
        File dest = new File(dir, newFilename);
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, dest.toPath());
            log.info("图片上传成功：{}", dest.getAbsolutePath());
        } catch (IOException e) {
            log.error("图片保存失败：{}", e.getMessage(), e);
            return Result.error("图片保存失败，请稍后重试");
        }

        // 返回图片访问 URL，格式：/api/uploads/{filename}
        // 该路径由 WebMvcConfig 中的静态资源处理器对外提供服务
        String imageUrl = "/api/uploads/" + newFilename;
        return Result.success(imageUrl);
    }
}
