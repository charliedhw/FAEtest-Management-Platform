package com.sugon.testplatform.service;

import com.sugon.testplatform.entity.TestReport;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ReportService {
    TestReport upload(Long projectId, MultipartFile file);
    List<TestReport> listByProject(Long projectId);
    void delete(Long id);
    byte[] download(Long id);
    TestReport getById(Long id);
    // 项目删除时级联清理（内部调用，入口已鉴权）：删除MinIO对象与报告记录
    void deleteByProject(Long projectId);
}
