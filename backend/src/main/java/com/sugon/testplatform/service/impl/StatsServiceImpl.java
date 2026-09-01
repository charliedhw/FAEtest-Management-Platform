package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.security.DataScopeHelper;
import com.sugon.testplatform.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final TestProjectMapper projectMapper;

    /**
     * 给统计查询追加数据权限条件
     */
    private <T> QueryWrapper<T> scoped(QueryWrapper<T> qw) {
        String scope = DataScopeHelper.projectScopeSql("");
        if (scope != null && !scope.isEmpty()) {
            qw.apply(scope.substring(4)); // 去掉前导 " AND"
        }
        return qw;
    }

    @Override
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", statusDistribution());
        result.put("region", regionDistribution());
        result.put("testType", testTypeDistribution());
        result.put("monthly", monthlyTrend());
        result.put("bid", bidStats());
        QueryWrapper<TestProject> cq = scoped(new QueryWrapper<>());
        long total = projectMapper.selectCount(cq);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> statusDistribution() {
        QueryWrapper<TestProject> qw = new QueryWrapper<>();
        qw.select("status", "count(*) as cnt").groupBy("status");
        scoped(qw);
        List<Map<String, Object>> list = projectMapper.selectMaps(qw);
        Map<String, Object> map = new HashMap<>();
        for (Map<String, Object> row : list) {
            map.put(String.valueOf(row.get("status")), row.get("cnt"));
        }
        return map;
    }

    @Override
    public Map<String, Object> regionDistribution() {
        QueryWrapper<TestProject> qw = new QueryWrapper<>();
        qw.select("region", "count(*) as cnt").isNotNull("region").groupBy("region");
        scoped(qw);
        List<Map<String, Object>> list = projectMaps(qw);
        Map<String, Object> map = new HashMap<>();
        for (Map<String, Object> row : list) {
            map.put(String.valueOf(row.get("region")), row.get("cnt"));
        }
        return map;
    }

    private List<Map<String, Object>> projectMaps(QueryWrapper<TestProject> qw) {
        return projectMapper.selectMaps(qw);
    }

    @Override
    public Map<String, Object> testTypeDistribution() {
        QueryWrapper<TestProject> qw = new QueryWrapper<TestProject>().select("test_type").isNotNull("test_type");
        scoped(qw);
        List<TestProject> projects = projectMapper.selectList(qw);
        Map<String, Long> counter = new HashMap<>();
        for (TestProject p : projects) {
            if (p.getTestType() == null) continue;
            String type = p.getTestType().replace("[", "").replace("]", "").replace("\"", "");
            for (String t : type.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    counter.merge(trimmed, 1L, Long::sum);
                }
            }
        }
        return new HashMap<>(counter);
    }

    @Override
    public Map<String, Object> monthlyTrend() {
        QueryWrapper<TestProject> qw = new QueryWrapper<>();
        qw.select("DATE_FORMAT(apply_time, '%Y-%m') as month", "count(*) as cnt")
          .isNotNull("apply_time")
          .groupBy("DATE_FORMAT(apply_time, '%Y-%m')")
          .orderByAsc("month");
        scoped(qw);
        List<Map<String, Object>> list = projectMapper.selectMaps(qw);
        Map<String, Object> map = new HashMap<>();
        for (Map<String, Object> row : list) {
            map.put(String.valueOf(row.get("month")), row.get("cnt"));
        }
        return map;
    }

    @Override
    public Map<String, Object> bidStats() {
        Map<String, Object> map = new HashMap<>();
        QueryWrapper<TestProject> qw = new QueryWrapper<>();
        qw.select("bid_status", "count(*) as cnt", "IFNULL(SUM(bid_amount),0) as amount")
          .isNotNull("bid_status").groupBy("bid_status");
        scoped(qw);
        List<Map<String, Object>> list = projectMapper.selectMaps(qw);
        BigDecimal totalAmount = BigDecimal.ZERO;
        long wonCount = 0;
        for (Map<String, Object> row : list) {
            String status = String.valueOf(row.get("bid_status"));
            map.put(status + "Count", row.get("cnt"));
            map.put(status + "Amount", row.get("amount"));
            if ("已中标".equals(status)) {
                wonCount = ((Number) row.get("cnt")).longValue();
                totalAmount = new BigDecimal(String.valueOf(row.get("amount")));
            }
        }
        map.put("wonCount", wonCount);
        map.put("wonAmount", totalAmount);
        return map;
    }

    @Override
    public Map<String, Object> dimensionStats(String dimension) {
        Map<String, Object> map = new HashMap<>();
        switch (dimension) {
            case "status" -> {
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("status as dim", "count(*) as cnt").groupBy("status");
                scoped(qw);
                for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
                    map.put(statusText(String.valueOf(row.get("dim"))), row.get("cnt"));
                }
            }
            case "region" -> {
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("region as dim", "count(*) as cnt").isNotNull("region").ne("region", "").groupBy("region");
                scoped(qw);
                for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
                    map.put(String.valueOf(row.get("dim")), row.get("cnt"));
                }
            }
            case "testType" -> {
                return testTypeDistribution();
            }
            case "deviceType" -> {
                // 设备类型: 按逗号/顿号拆分后单独统计
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("device_type").isNotNull("device_type").ne("device_type", "");
                scoped(qw);
                List<TestProject> devList = projectMapper.selectList(qw);
                Map<String, Long> devCounter = new HashMap<>();
                for (TestProject p : devList) {
                    if (p.getDeviceType() == null) continue;
                    String cleaned = p.getDeviceType().replace("[", "").replace("]", "").replace("\"", "");
                    for (String d : cleaned.split("[,，、/]")) {
                        String trimmed = d.trim();
                        if (!trimmed.isEmpty()) {
                            devCounter.merge(trimmed, 1L, Long::sum);
                        }
                    }
                }
                return new HashMap<>(devCounter);
            }
            case "period" -> {
                // 测试周期分段: <=7天 / 8-15天 / 16-30天 / 31-90天 / >90天
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("test_start_time", "test_end_time").isNotNull("test_start_time").isNotNull("test_end_time");
                scoped(qw);
                List<TestProject> list = projectMapper.selectList(qw);
                java.util.LinkedHashMap<String, Long> buckets = new java.util.LinkedHashMap<>();
                buckets.put("7天内", 0L); buckets.put("8-15天", 0L); buckets.put("16-30天", 0L);
                buckets.put("31-90天", 0L); buckets.put("90天以上", 0L);
                for (TestProject p : list) {
                    if (p.getTestStartTime() == null || p.getTestEndTime() == null) continue;
                    long days = java.time.temporal.ChronoUnit.DAYS.between(p.getTestStartTime(), p.getTestEndTime());
                    if (days <= 7) buckets.merge("7天内", 1L, Long::sum);
                    else if (days <= 15) buckets.merge("8-15天", 1L, Long::sum);
                    else if (days <= 30) buckets.merge("16-30天", 1L, Long::sum);
                    else if (days <= 90) buckets.merge("31-90天", 1L, Long::sum);
                    else buckets.merge("90天以上", 1L, Long::sum);
                }
                return new HashMap<>(buckets);
            }
            case "monthly" -> {
                return monthlyTrend();
            }
            case "sales" -> {
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("sales_name as dim", "count(*) as cnt").isNotNull("sales_name").ne("sales_name", "").groupBy("sales_name").orderByDesc("cnt").last("limit 20");
                scoped(qw);
                for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
                    map.put(String.valueOf(row.get("dim")), row.get("cnt"));
                }
            }
            case "presales" -> {
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("presales_name as dim", "count(*) as cnt").isNotNull("presales_name").ne("presales_name", "").groupBy("presales_name").orderByDesc("cnt").last("limit 20");
                scoped(qw);
                for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
                    map.put(String.valueOf(row.get("dim")), row.get("cnt"));
                }
            }
            case "tester" -> {
                // 测试人员工作量: tester_names是/分隔
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("tester_names").isNotNull("tester_names").ne("tester_names", "");
                scoped(qw);
                List<TestProject> list = projectMapper.selectList(qw);
                Map<String, Long> counter = new HashMap<>();
                for (TestProject p : list) {
                    if (p.getTesterNames() == null) continue;
                    for (String t : p.getTesterNames().split("[/,，、]")) {
                        String n = t.trim();
                        if (!n.isEmpty()) counter.merge(n, 1L, Long::sum);
                    }
                }
                return new HashMap<>(counter);
            }
            case "internalResource" -> {
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("is_internal_resource as dim", "count(*) as cnt").isNotNull("is_internal_resource").ne("is_internal_resource", "").groupBy("is_internal_resource");
                scoped(qw);
                for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
                    map.put(String.valueOf(row.get("dim")), row.get("cnt"));
                }
            }
            case "bidStatus" -> {
                QueryWrapper<TestProject> qw = new QueryWrapper<>();
                qw.select("bid_status as dim", "count(*) as cnt").isNotNull("bid_status").ne("bid_status", "").groupBy("bid_status");
                scoped(qw);
                for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
                    map.put(String.valueOf(row.get("dim")), row.get("cnt"));
                }
            }
            default -> {
                return statusDistribution();
            }
        }
        return map;
    }

    @Override
    public Map<String, Object> timeDimensionStats(String timeUnit) {
        // 按测试开始时间统计, 支持 month/quarter/halfYear/year
        String dateExpr;
        switch (timeUnit) {
            case "month" -> dateExpr = "DATE_FORMAT(test_start_time, '%Y-%m')";
            case "quarter" -> dateExpr = "CONCAT(YEAR(test_start_time), 'Q', QUARTER(test_start_time))";
            case "halfYear" -> dateExpr = "CONCAT(YEAR(test_start_time), IF(MONTH(test_start_time)<=6,'H1','H2'))";
            case "year" -> dateExpr = "YEAR(test_start_time)";
            default -> dateExpr = "DATE_FORMAT(test_start_time, '%Y-%m')";
        }
        QueryWrapper<TestProject> qw = new QueryWrapper<>();
        qw.select(dateExpr + " as dim", "count(*) as cnt")
          .isNotNull("test_start_time")
          .groupBy(dateExpr)
          .orderByAsc("dim");
        scoped(qw);
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : projectMapper.selectMaps(qw)) {
            map.put(String.valueOf(row.get("dim")), row.get("cnt"));
        }
        return map;
    }

    private String statusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "NOT_START" -> "未开始";
            case "IN_PROGRESS" -> "进行中";
            case "PAUSED" -> "暂停";
            case "COMPLETED" -> "已完成";
            case "CLOSED" -> "关闭";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }
}
