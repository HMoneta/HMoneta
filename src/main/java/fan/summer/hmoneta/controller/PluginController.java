package fan.summer.hmoneta.controller;

import fan.summer.hmoneta.common.enums.exception.plugin.PluginExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.service.plugin.PluginService;
import fan.summer.hmoneta.util.ObjectUtil;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 类的详细说明
 *
 * @author summer
 * @version 1.00
 * @Date 2025/11/5
 */
@RestController
@RequestMapping("/hm/plugin")
@AllArgsConstructor
public class PluginController {
    private final PluginService pluginService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPlugin(@RequestParam("plugin") MultipartFile plugin) throws IOException {
        if(ObjectUtil.isEmpty(plugin)) {
            throw new HMException(PluginExceptionEnum.PLUGIN_FILE_EMPTY_ERROR);
        }
        if(!isZipFile(plugin)){
            throw new HMException(PluginExceptionEnum.PLUGIN_FILE_TYPE_ERROR);
        }
        createUploadDirectory();
        String safeFileName = generateSafeFileName(plugin.getOriginalFilename());
        Path filePath = Paths.get("plugins", safeFileName);
        pluginService.upLoadPlugin(plugin.getInputStream(), filePath);
        return ResponseEntity.ok("ZIP文件上传成功: " + safeFileName);
    }

    private boolean isZipFile(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();

        // 检查MIME类型和文件扩展名
        return ("application/zip".equals(contentType) ||
                "application/x-zip-compressed".equals(contentType) ||
                (originalFileName != null && originalFileName.toLowerCase().endsWith(".zip")));
    }

    private void createUploadDirectory() throws IOException {
        Path uploadPath = Paths.get("plugin");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    private String generateSafeFileName(String originalFileName) {
        // 移除路径信息并处理特殊字符
        String fileName = new File(originalFileName).getName();

        // 添加时间戳防止重名
        String timeStamp = String.valueOf(System.currentTimeMillis());
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex) + "_" + timeStamp + fileName.substring(dotIndex);
        } else {
            return fileName + "_" + timeStamp;
        }
    }
}
