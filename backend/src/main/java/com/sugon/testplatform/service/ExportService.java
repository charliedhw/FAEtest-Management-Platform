package com.sugon.testplatform.service;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ExportService {
    void exportProjects(HttpServletResponse response, Map<String, Object> params);
}
