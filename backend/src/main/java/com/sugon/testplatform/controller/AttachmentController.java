package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.TestAttachment;
import com.sugon.testplatform.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/attachment")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    public Result<TestAttachment> upload(@RequestParam Long appId, @RequestParam String fileType,
                                         @RequestParam MultipartFile file) {
        return Result.ok(attachmentService.upload(appId, fileType, file));
    }

    @GetMapping("/list")
    public Result<List<TestAttachment>> list(@RequestParam Long appId) {
        return Result.ok(attachmentService.listByApp(appId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return Result.ok();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        TestAttachment att = attachmentService.getById(id);
        byte[] data = attachmentService.download(id);
        String name = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
