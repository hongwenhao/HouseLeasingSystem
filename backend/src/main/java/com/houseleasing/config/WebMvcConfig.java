package com.houseleasing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 扩展配置类
 *
 * @author hongwenhao
 * @description 注册静态资源处理器，将本地磁盘的上传图片目录
 *              映射到 /api/uploads/** URL，使前端可通过 HTTP 访问已上传的图片。
 *
 *              该配置与 FileUploadController 配合使用：
 *              - 图片上传写入：POST /api/upload/image（需登录）
 *              - 图片读取访问：GET /api/uploads/{filename}（公开访问）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 图片上传根目录，与 FileUploadController 使用同一配置项，保持路径一致
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * 注册静态资源处理器
     *
     * <p>将本地磁盘目录（uploadDir）映射为可供外部 HTTP 访问的路径（/api/uploads/**）。
     * Spring Security 中已对 GET /api/uploads/** 放行，无需认证即可访问图片。</p>
     *
     * <p>路径说明：
     * <ul>
     *   <li>URL 模式：/api/uploads/**（匹配所有图片文件请求）</li>
     *   <li>磁盘位置：file:{uploadDir}/（相对或绝对路径均支持）</li>
     * </ul>
     * </p>
     *
     * @param registry Spring MVC 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 规范化路径：确保以 "/" 结尾，以便 Spring 正确拼接文件名
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
