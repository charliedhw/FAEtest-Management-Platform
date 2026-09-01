package com.sugon.testplatform.service;

import java.util.Map;

public interface StatsService {
    Map<String, Object> dashboard();
    Map<String, Object> statusDistribution();
    Map<String, Object> regionDistribution();
    Map<String, Object> testTypeDistribution();
    Map<String, Object> monthlyTrend();
    Map<String, Object> bidStats();

    /**
     * 通用维度统计: 按指定维度分组计数
     * @param dimension status/region/testType/deviceType/period/sales/presales/tester/internalResource/bizType/bidStatus
     */
    Map<String, Object> dimensionStats(String dimension);

    /**
     * 按测试开始时间统计: month/quarter/halfYear/year
     */
    Map<String, Object> timeDimensionStats(String timeUnit);
}
