package com.sugon.testplatform.service;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.Resource;
import com.sugon.testplatform.entity.ResourceLoan;
import java.util.List;
import java.util.Map;

public interface ResourceService {
    PageResult<Resource> pageResource(int pageNum, int pageSize, String type, String status, String keyword);
    void saveResource(Resource resource);
    void deleteResource(Long id);
    void online(Long id);
    void offline(Long id);
    PageResult<Resource> pageOnlineAsset(int pageNum, int pageSize, String type, String keyword);
    ResourceLoan borrow(ResourceLoan loan);
    void returnResource(Long loanId);
    int recycleByProject(Long projectId);
    PageResult<ResourceLoan> pageLoan(int pageNum, int pageSize, Map<String, Object> params);
    List<ResourceLoan> overdueList();
    Map<String, Object> resourceStats();
}
