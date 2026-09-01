package com.sugon.testplatform.service;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.ProgressRequest;
import com.sugon.testplatform.entity.TestProgress;
import java.util.List;
import java.util.Map;

public interface ProgressService {
    void add(ProgressRequest req);
    void update(TestProgress progress);
    void delete(Long id);
    List<TestProgress> listByProject(Long projectId);
    PageResult<Map<String, Object>> projectSummary(int pageNum, int pageSize, String keyword, String date);
}
