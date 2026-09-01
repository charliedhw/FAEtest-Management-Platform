package com.sugon.testplatform.service;

import com.sugon.testplatform.entity.TestStage;
import java.util.List;
import java.util.Map;

public interface StageService {
    TestStage add(TestStage stage);
    void update(TestStage stage);
    void delete(Long id);
    List<TestStage> listByProject(Long projectId);
    Map<String, Object> projectProgress(Long projectId);
}
