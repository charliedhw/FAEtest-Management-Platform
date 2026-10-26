package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.TestReportView;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.entity.TestReport;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.mapper.TestReportMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ProjectService;
import com.sugon.testplatform.service.ReportService;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final TestReportMapper reportMapper;
    private final TestProjectMapper projectMapper;

    @Lazy
    @Autowired
    private ProjectService projectService;

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            log.warn("MinIO初始化失败(可能未启动): {}", e.getMessage());
        }
    }

    private MinioClient requireClient() {
        if (minioClient == null) throw new BizException("文件存储服务不可用，请联系管理员");
        return minioClient;
    }

    private boolean canSeeAllReports() {
        return UserContext.hasAnyRole("APPROVER", "LEADER", "TESTER", "ADMIN");
    }

    private void checkReportCenterAccess() {
        if (!canSeeAllReports()) {
            throw new BizException("仅售前审批组、分管领导和FAE测试工程师可查看测试报告模块");
        }
    }

    @Override
    public PageResult<TestReportView> page(int pageNum, int pageSize, Map<String, Object> params) {
        checkReportCenterAccess();
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BizException("分页参数无效，每页条数应为1至200");
        }

        LambdaQueryWrapper<TestProject> projectQuery = buildProjectQuery(params);
        projectQuery.apply("test_project.id = test_report.project_id");
        // 报告中心授权角色（含FAE测试工程师）均可查看全部测试报告
        if (!canSeeAllReports()) {
            projectService.applyDataScope(projectQuery);
        }

        LambdaQueryWrapper<TestReport> reportQuery = new LambdaQueryWrapper<>();
        reportQuery.exists("SELECT 1 FROM test_project WHERE " + projectQuery.getSqlSegment());
        // 所有筛选值都在项目子查询中绑定，合并参数后仍使用预编译占位符
        reportQuery.getParamNameValuePairs().putAll(projectQuery.getParamNameValuePairs());
        reportQuery.orderByDesc(TestReport::getCreateTime).orderByDesc(TestReport::getId);
        Page<TestReport> result = reportMapper.selectPage(new Page<>(pageNum, pageSize), reportQuery);
        if (result.getRecords().isEmpty()) {
            return new PageResult<>(result.getTotal(), List.of());
        }

        List<Long> projectIds = result.getRecords().stream()
                .map(TestReport::getProjectId).distinct().toList();
        Map<Long, TestProject> projects = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TestProject::getId, Function.identity()));
        List<TestReportView> records = result.getRecords().stream()
                .map(report -> toView(report, projects.get(report.getProjectId())))
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    @Override
    public void batchDownload(List<Long> ids, HttpServletResponse response) {
        checkReportCenterAccess();
        if (ids == null) throw new BizException("请选择需要下载的报告");
        List<Long> reportIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (reportIds.isEmpty()) throw new BizException("请选择需要下载的报告");
        if (reportIds.size() > 100) throw new BizException("单次最多批量下载100份报告");

        Map<Long, TestReport> reportMap = reportMapper.selectBatchIds(reportIds).stream()
                .collect(Collectors.toMap(TestReport::getId, Function.identity()));
        List<TestReport> reports = reportIds.stream().map(reportMap::get).filter(Objects::nonNull).toList();
        if (reports.size() != reportIds.size()) {
            throw new BizException("部分报告不存在或已被删除，请刷新后重试");
        }
        List<Long> projectIds = reports.stream().map(TestReport::getProjectId).distinct().toList();
        if (!canSeeAllReports()) {
            for (Long projectId : projectIds) {
                if (!projectService.canView(projectId)) {
                    throw new BizException("所选报告中包含无权下载的项目报告");
                }
            }
        }
        Map<Long, TestProject> projects = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TestProject::getId, Function.identity()));

        MinioClient client = requireClient();
        for (TestReport report : reports) {
            try {
                client.statObject(StatObjectArgs.builder()
                        .bucket(bucket).object(report.getFileKey()).build());
            } catch (Exception e) {
                throw new BizException("报告文件不存在或无法读取：" + report.getFileName());
            }
        }

        String zipName = "test_reports_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + zipName + "\"");
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            for (TestReport report : reports) {
                String entryName = zipEntryName(report, projects.get(report.getProjectId()));
                zip.putNextEntry(new ZipEntry(entryName));
                try (InputStream in = client.getObject(GetObjectArgs.builder()
                        .bucket(bucket).object(report.getFileKey()).build())) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        } catch (Exception e) {
            log.error("批量下载报告失败", e);
            throw new BizException("批量下载失败：" + e.getMessage());
        }
    }

    private String zipEntryName(TestReport report, TestProject project) {
        String projectNo = project != null && StringUtils.hasText(project.getProjectNo())
                ? project.getProjectNo() : "project_" + report.getProjectId();
        String fileName = StringUtils.hasText(report.getFileName())
                ? report.getFileName() : "report_" + report.getId();
        int version = report.getVersion() == null ? 1 : report.getVersion();
        return sanitizeZipPart(projectNo) + "/V" + version + "_" + report.getId() + "_" + sanitizeZipPart(fileName);
    }

    private String sanitizeZipPart(String value) {
        String cleaned = value.trim().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (!StringUtils.hasText(cleaned) || ".".equals(cleaned) || "..".equals(cleaned)) return "unnamed";
        return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
    }

    private LambdaQueryWrapper<TestProject> buildProjectQuery(Map<String, Object> params) {
        LambdaQueryWrapper<TestProject> query = new LambdaQueryWrapper<>();
        if (params == null) return query;

        Object testerId = params.get("testerId");
        if (testerId != null && StringUtils.hasText(testerId.toString())) {
            String id = testerId.toString();
            query.apply("(tester_ids = {0} OR tester_ids LIKE CONCAT({0},',%') "
                    + "OR tester_ids LIKE CONCAT('%,',{0}) OR tester_ids LIKE CONCAT('%,',{0},',%'))", id);
        }
        Object testType = params.get("testType");
        if (testType != null && StringUtils.hasText(testType.toString())) {
            applyMultiValueFilter(query, "test_type", testType.toString(), true, true);
        }
        Object deviceType = params.get("deviceType");
        if (deviceType != null && StringUtils.hasText(deviceType.toString())) {
            applyMultiValueFilter(query, "device_type", deviceType.toString(), false, false);
        }
        Object keyword = params.get("keyword");
        if (keyword != null && StringUtils.hasText(keyword.toString())) {
            query.and(w -> w.like(TestProject::getProjectName, keyword)
                    .or().like(TestProject::getCustomerName, keyword)
                    .or().like(TestProject::getProjectNo, keyword)
                    .or().like(TestProject::getSpmNo, keyword));
        }
        return query;
    }

    private void applyMultiValueFilter(LambdaQueryWrapper<TestProject> query, String column,
                                       String value, boolean splitSlash, boolean removeSpaces) {
        String expression = "COALESCE(" + column + ",'')";
        expression = sqlReplace(expression, "[", "");
        expression = sqlReplace(expression, "]", "");
        expression = sqlReplace(expression, "\"", "");
        expression = sqlReplace(expression, "，", ",");
        expression = sqlReplace(expression, "、", ",");
        if (splitSlash) expression = sqlReplace(expression, "/", ",");
        if (removeSpaces) expression = sqlReplace(expression, " ", "");
        query.apply("FIND_IN_SET({0}, " + expression + ")", value);
    }

    private String sqlReplace(String expression, String search, String replacement) {
        return "REPLACE(" + expression + ",'" + search + "','" + replacement + "')";
    }

    private TestReportView toView(TestReport report, TestProject project) {
        TestReportView view = new TestReportView();
        view.setId(report.getId());
        view.setProjectId(report.getProjectId());
        view.setFileName(report.getFileName());
        view.setFileSize(report.getFileSize());
        view.setVersion(report.getVersion());
        view.setUploadBy(report.getUploadBy());
        view.setUploadByName(report.getUploadByName());
        view.setCreateTime(report.getCreateTime());
        if (project != null) {
            view.setProjectNo(project.getProjectNo());
            view.setProjectName(project.getProjectName());
            view.setCustomerName(project.getCustomerName());
            view.setTesterNames(project.getTesterNames());
            view.setTestType(project.getTestType());
            view.setDeviceType(project.getDeviceType());
            view.setProjectStatus(project.getStatus());
        }
        return view;
    }

    @Override
    public TestReport upload(Long projectId, MultipartFile file) {
        // 只有被分配的FAE测试人员及管理员才能上传报告
        if (!projectService.canEditProgress(projectId)) {
            throw new BizException("只有接受测试任务的FAE测试人员或管理员才能上传报告");
        }
        try {
            String originalName = file.getOriginalFilename();
            String key = projectId + "/" + UUID.randomUUID() + "_" + originalName;
            requireClient().putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // version
            Long count = reportMapper.selectCount(new LambdaQueryWrapper<TestReport>()
                    .eq(TestReport::getProjectId, projectId));
            TestReport report = new TestReport();
            report.setProjectId(projectId);
            report.setFileName(originalName);
            report.setFileKey(key);
            report.setFileSize(file.getSize());
            report.setVersion(count.intValue() + 1);
            report.setUploadBy(UserContext.getUserId());
            report.setUploadByName(UserContext.getRealName());
            reportMapper.insert(report);
            return report;
        } catch (Exception e) {
            log.error("上传报告失败", e);
            throw new BizException("上传报告失败: " + e.getMessage());
        }
    }

    @Override
    public List<TestReport> listByProject(Long projectId) {
        if (!projectService.canView(projectId)) {
            throw new BizException("无权查看该项目的报告");
        }
        return reportMapper.selectList(new LambdaQueryWrapper<TestReport>()
                .eq(TestReport::getProjectId, projectId)
                .orderByDesc(TestReport::getVersion));
    }

    @Override
    public void delete(Long id) {
        TestReport report = reportMapper.selectById(id);
        if (report == null) return;
        if (!projectService.canEditProgress(report.getProjectId())) {
            throw new BizException("无权删除该报告");
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(report.getFileKey()).build());
        } catch (Exception e) {
            log.warn("删除MinIO对象失败: {}", e.getMessage());
        }
        reportMapper.deleteById(id);
    }

    @Override
    public byte[] download(Long id) {
        TestReport report = reportMapper.selectById(id);
        if (report == null) throw new BizException("报告不存在");
        if (!canSeeAllReports() && !projectService.canView(report.getProjectId())) {
            throw new BizException("无权下载该报告");
        }
        try (InputStream in = requireClient().getObject(GetObjectArgs.builder()
                .bucket(bucket).object(report.getFileKey()).build());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("下载失败: " + e.getMessage());
        }
    }

    @Override
    public TestReport getById(Long id) {
        return reportMapper.selectById(id);
    }

    @Override
    public void deleteByProject(Long projectId) {
        List<TestReport> reports = reportMapper.selectList(
                new LambdaQueryWrapper<TestReport>().eq(TestReport::getProjectId, projectId));
        for (TestReport r : reports) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket).object(r.getFileKey()).build());
            } catch (Exception e) {
                log.warn("级联删除MinIO对象失败 key={}: {}", r.getFileKey(), e.getMessage());
            }
            reportMapper.deleteById(r.getId());
        }
    }
}
