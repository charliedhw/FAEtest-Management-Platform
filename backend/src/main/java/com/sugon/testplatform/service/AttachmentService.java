package com.sugon.testplatform.service;

import com.sugon.testplatform.entity.TestAttachment;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AttachmentService {
    TestAttachment upload(Long appId, String fileType, MultipartFile file);
    List<TestAttachment> listByApp(Long appId);
    void delete(Long id);
    byte[] download(Long id);
    TestAttachment getById(Long id);
    // 申请删除时级联清理（内部调用，入口已鉴权）：删除MinIO对象与附件记录
    void deleteByApp(Long appId);
}
