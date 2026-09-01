package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.entity.TestReport;
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
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final TestReportMapper reportMapper;

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

    @Override
    public TestReport upload(Long projectId, MultipartFile file) {
        // 只有被分配的FAE测试人员及管理员才能上传报告
        if (!projectService.canEditProgress(projectId)) {
            throw new BizException("只有接受测试任务的FAE测试人员或管理员才能上传报告");
        }
        try {
            String originalName = file.getOriginalFilename();
            String key = projectId + "/" + UUID.randomUUID() + "_" + originalName;
            minioClient.putObject(PutObjectArgs.builder()
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
        return reportMapper.selectList(new LambdaQueryWrapper<TestReport>()
                .eq(TestReport::getProjectId, projectId)
                .orderByDesc(TestReport::getVersion));
    }

    @Override
    public void delete(Long id) {
        TestReport report = reportMapper.selectById(id);
        if (report == null) return;
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
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
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
}
