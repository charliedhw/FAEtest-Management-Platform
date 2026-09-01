package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.Resource;
import com.sugon.testplatform.entity.ResourceLoan;
import com.sugon.testplatform.mapper.ResourceLoanMapper;
import com.sugon.testplatform.mapper.ResourceMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.DictService;
import com.sugon.testplatform.service.NotifyService;
import com.sugon.testplatform.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private final ResourceMapper resourceMapper;
    private final ResourceLoanMapper loanMapper;
    private final DictService dictService;
    private final NotifyService notifyService;

    @Override
    public PageResult<Resource> pageResource(int pageNum, int pageSize, String type, String status, String keyword) {
        Page<Resource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Resource> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) qw.eq(Resource::getResourceType, type);
        if (StringUtils.hasText(status)) qw.eq(Resource::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Resource::getResourceName, keyword)
                    .or().like(Resource::getResourceCode, keyword));
        }
        qw.orderByDesc(Resource::getCreateTime);
        Page<Resource> result = resourceMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public void saveResource(Resource resource) {
        if (resource.getId() == null) {
            if (!StringUtils.hasText(resource.getStatus())) resource.setStatus("IDLE");
            resourceMapper.insert(resource);
        } else {
            resourceMapper.updateById(resource);
        }
    }

    @Override
    public void deleteResource(Long id) {
        resourceMapper.deleteById(id);
    }

    @Override
    public void online(Long id) {
        Resource r = new Resource();
        r.setId(id);
        r.setOnlineStatus("ONLINE");
        resourceMapper.updateById(r);
    }

    @Override
    public void offline(Long id) {
        Resource r = new Resource();
        r.setId(id);
        r.setOnlineStatus("OFFLINE");
        resourceMapper.updateById(r);
    }

    @Override
    public PageResult<Resource> pageOnlineAsset(int pageNum, int pageSize, String type, String keyword) {
        // 资产中心: 只返回已上线资产
        Page<Resource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Resource> qw = new LambdaQueryWrapper<>();
        qw.eq(Resource::getOnlineStatus, "ONLINE");
        if (StringUtils.hasText(type)) qw.eq(Resource::getResourceType, type);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Resource::getResourceName, keyword)
                    .or().like(Resource::getResourceCode, keyword));
        }
        qw.orderByDesc(Resource::getCreateTime);
        Page<Resource> result = resourceMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public ResourceLoan borrow(ResourceLoan loan) {
        Resource resource = resourceMapper.selectById(loan.getResourceId());
        if (resource == null) throw new BizException("资源不存在");
        if (!"IDLE".equals(resource.getStatus())) throw new BizException("资源当前不可用");
        // 借出必须关联测试项目
        if (loan.getProjectId() == null) {
            throw new BizException("资产借出必须关联测试项目");
        }

        loan.setBorrowerId(UserContext.getUserId());
        loan.setBorrowerName(UserContext.getRealName());
        loan.setLoanTime(LocalDateTime.now());
        loan.setStatus("BORROWED");
        if (loan.getExpectReturnTime() != null) {
            loan.setLoanDays((int) ChronoUnit.DAYS.between(loan.getLoanTime(), loan.getExpectReturnTime()));
        }
        loanMapper.insert(loan);

        // 更新资源状态
        Resource update = new Resource();
        update.setId(resource.getId());
        update.setStatus("IN_USE");
        resourceMapper.updateById(update);
        return loan;
    }

    @Override
    @Transactional
    public void returnResource(Long loanId) {
        ResourceLoan loan = loanMapper.selectById(loanId);
        if (loan == null) throw new BizException("借用记录不存在");
        loan.setActualReturnTime(LocalDateTime.now());
        loan.setStatus("RETURNED");
        loanMapper.updateById(loan);

        Resource update = new Resource();
        update.setId(loan.getResourceId());
        update.setStatus("IDLE");
        resourceMapper.updateById(update);
    }

    @Override
    @Transactional
    public int recycleByProject(Long projectId) {
        // 回收该项目名下所有未归还(BORROWED/OVERDUE)的借用资源
        List<ResourceLoan> loans = loanMapper.selectList(new LambdaQueryWrapper<ResourceLoan>()
                .eq(ResourceLoan::getProjectId, projectId)
                .in(ResourceLoan::getStatus, "BORROWED", "OVERDUE"));
        int count = 0;
        for (ResourceLoan loan : loans) {
            loan.setActualReturnTime(LocalDateTime.now());
            loan.setStatus("RETURNED");
            loan.setRemark((loan.getRemark() == null ? "" : loan.getRemark() + " ") + "[项目完成自动回收]");
            loanMapper.updateById(loan);

            Resource update = new Resource();
            update.setId(loan.getResourceId());
            update.setStatus("IDLE");
            resourceMapper.updateById(update);

            // 通知借用人
            try {
                notifyService.send(loan.getBorrowerId(), "资源已自动回收",
                        "您借用的资源已随项目完成自动回收，资源ID:" + loan.getResourceId(),
                        "SYSTEM", loan.getId(), "/resource/loan");
            } catch (Exception ignored) {
            }
            count++;
        }
        return count;
    }

    @Override
    public PageResult<ResourceLoan> pageLoan(int pageNum, int pageSize, Map<String, Object> params) {
        Page<ResourceLoan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ResourceLoan> qw = new LambdaQueryWrapper<>();
        if (params != null) {
            Object status = params.get("status");
            if (status != null && StringUtils.hasText(status.toString())) {
                qw.eq(ResourceLoan::getStatus, status);
            }
            Object borrowerId = params.get("borrowerId");
            if (borrowerId != null) qw.eq(ResourceLoan::getBorrowerId, borrowerId);
        }
        qw.orderByDesc(ResourceLoan::getCreateTime);
        Page<ResourceLoan> result = loanMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public List<ResourceLoan> overdueList() {
        int warnDays = Integer.parseInt(dictService.getConfig("loan.warn.days", "30"));
        LocalDateTime threshold = LocalDateTime.now().minusDays(warnDays);
        return loanMapper.selectList(new LambdaQueryWrapper<ResourceLoan>()
                .eq(ResourceLoan::getStatus, "BORROWED")
                .lt(ResourceLoan::getLoanTime, threshold));
    }

    @Override
    public Map<String, Object> resourceStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = resourceMapper.selectCount(null);
        long idle = resourceMapper.selectCount(new LambdaQueryWrapper<Resource>().eq(Resource::getStatus, "IDLE"));
        long inUse = resourceMapper.selectCount(new LambdaQueryWrapper<Resource>().eq(Resource::getStatus, "IN_USE"));
        long borrowed = loanMapper.selectCount(new LambdaQueryWrapper<ResourceLoan>().eq(ResourceLoan::getStatus, "BORROWED"));
        stats.put("total", total);
        stats.put("idle", idle);
        stats.put("inUse", inUse);
        stats.put("borrowed", borrowed);
        stats.put("utilization", total == 0 ? 0 : Math.round(inUse * 100.0 / total));
        return stats;
    }
}
