package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.entity.TestApplication;
import com.sugon.testplatform.entity.TestAttachment;
import com.sugon.testplatform.mapper.TestApplicationMapper;
import com.sugon.testplatform.mapper.TestAttachmentMapper;
import com.sugon.testplatform.security.DataScopeHelper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.AttachmentService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AttachmentServiceImpl implements AttachmentService {
    private final TestAttachmentMapper attachmentMapper;
    private final TestApplicationMapper applicationMapper;

    public AttachmentServiceImpl(TestAttachmentMapper attachmentMapper, TestApplicationMapper applicationMapper) {
        this.attachmentMapper = attachmentMapper;
        this.applicationMapper = applicationMapper;
    }

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
            minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        } catch (Exception e) {
            log.warn("MinIO初始化失败: {}", e.getMessage());
        }
    }

    /** 是否能查看该申请（与申请数据范围一致） */
    private boolean canViewApp(TestApplication app) {
        if (DataScopeHelper.seeAll()) return true;
        Long uid = UserContext.getUserId();
        if (uid == null) return false;
        return uid.equals(app.getApplicantId()) || uid.equals(app.getPresalesId()) || uid.equals(app.getSalesId());
    }

    /** 是否能编辑（上传/删除）：仅申请人本人或管理员；提交后不可删（审批中/已立项） */
    private void checkEditable(TestApplication app) {
        Long uid = UserContext.requireUserId();
        boolean isAdmin = UserContext.getRoles().contains("ADMIN");
        if (!isAdmin && !uid.equals(app.getApplicantId())) {
            throw new BizException("只有申请人才能操作附件");
        }
    }

    @Override
    public TestAttachment upload(Long appId, String fileType, MultipartFile file) {
        TestApplication app = applicationMapper.selectById(appId);
        if (app == null) throw new BizException("申请单不存在");
        checkEditable(app);
        if (fileType == null || (!"METRIC".equals(fileType) && !"PLAN".equals(fileType))) {
            throw new BizException("附件类型必须为 METRIC(测试指标要求) 或 PLAN(测试方案)");
        }
        try {
            String originalName = file.getOriginalFilename();
            String key = "application/" + appId + "/" + UUID.randomUUID() + "_" + originalName;
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType()).build());
            TestAttachment att = new TestAttachment();
            att.setBizType("APPLICATION");
            att.setBizId(appId);
            att.setFileType(fileType);
            att.setFileName(originalName);
            att.setFileKey(key);
            att.setFileSize(file.getSize());
            att.setUploadBy(UserContext.getUserId());
            att.setUploadByName(UserContext.getRealName());
            attachmentMapper.insert(att);
            return att;
        } catch (Exception e) {
            log.error("上传申请附件失败", e);
            throw new BizException("上传附件失败: " + e.getMessage());
        }
    }

    @Override
    public List<TestAttachment> listByApp(Long appId) {
        TestApplication app = applicationMapper.selectById(appId);
        if (app == null) throw new BizException("申请单不存在");
        if (!canViewApp(app)) throw new BizException("无权查看该申请附件");
        return attachmentMapper.selectList(new LambdaQueryWrapper<TestAttachment>()
                .eq(TestAttachment::getBizType, "APPLICATION").eq(TestAttachment::getBizId, appId)
                .orderByAsc(TestAttachment::getCreateTime));
    }

    @Override
    public void delete(Long id) {
        TestAttachment att = attachmentMapper.selectById(id);
        if (att == null) return;
        TestApplication app = applicationMapper.selectById(att.getBizId());
        if (app != null) {
            checkEditable(app);
            // 提交后(非草稿/驳回)不允许删除
            if (!"DRAFT".equals(app.getStatus()) && !"REJECTED".equals(app.getStatus())) {
                throw new BizException("申请已提交，附件不能删除");
            }
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(att.getFileKey()).build());
        } catch (Exception e) {
            log.warn("删除MinIO对象失败: {}", e.getMessage());
        }
        attachmentMapper.deleteById(id);
    }

    @Override
    public byte[] download(Long id) {
        TestAttachment att = attachmentMapper.selectById(id);
        if (att == null) throw new BizException("附件不存在");
        TestApplication app = applicationMapper.selectById(att.getBizId());
        if (app == null || !canViewApp(app)) throw new BizException("无权下载该附件");
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(att.getFileKey()).build());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("下载失败: " + e.getMessage());
        }
    }

    @Override
    public TestAttachment getById(Long id) {
        return attachmentMapper.selectById(id);
    }
}
