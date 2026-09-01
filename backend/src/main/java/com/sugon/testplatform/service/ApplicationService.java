package com.sugon.testplatform.service;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.ApplicationSubmitRequest;
import com.sugon.testplatform.dto.ApprovalRequest;
import com.sugon.testplatform.dto.AssignRequest;
import com.sugon.testplatform.entity.TestApplication;

public interface ApplicationService {
    Long submit(ApplicationSubmitRequest req);
    void saveDraft(ApplicationSubmitRequest req);
    void approve(ApprovalRequest req);
    void assign(AssignRequest req);
    void withdraw(Long appId);
    PageResult<TestApplication> page(int pageNum, int pageSize, String status, String keyword, Long applicantId, boolean todoOnly);
    TestApplication detail(Long id);
    PageResult<TestApplication> todoList(Long userId, int pageNum, int pageSize);
}
