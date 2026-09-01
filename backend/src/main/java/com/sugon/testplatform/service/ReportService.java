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
}
