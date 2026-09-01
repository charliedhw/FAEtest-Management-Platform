package com.sugon.testplatform.service;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.TestProject;
import java.util.Map;

public interface ProjectService {
    PageResult<TestProject> page(int pageNum, int pageSize, Map<String, Object> params);
    TestProject detail(Long id);
    void update(TestProject project);
    void updateStatus(Long id, String status);
    void updateBid(Long id, String bidStatus, java.math.BigDecimal bidAmount);
    void delete(Long id);
    void checkStartTestPermission(Long id);
    void checkSetKeyPermission();
    boolean canEditProgress(Long projectId);
}
