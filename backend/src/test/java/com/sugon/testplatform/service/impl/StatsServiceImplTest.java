package com.sugon.testplatform.service.impl;

import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.mapper.TestProjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsServiceImplTest {
    @Test
    void countsCombinedTypesSeparatelyAndDeduplicatesWithinEachProject() {
        List<TestProject> projects = Arrays.asList(
                "AI、CPU、分布式存储、AI4S",
                "CPU、GV+AI软件",
                "AI、AI4S",
                "AI4S、AI",
                "[\"AI\", \"CPU\", \"AI\"]",
                " AI，国产IB / AI ",
                "GV+AI软件",
                "分布式存储",
                "", " 、，/ ", null
        ).stream().map(value -> {
            TestProject project = new TestProject();
            project.setTestType(value);
            return project;
        }).collect(Collectors.toList());
        TestProjectMapper mapper = mock(TestProjectMapper.class);
        when(mapper.selectList(any())).thenReturn(projects);
        com.sugon.testplatform.service.ProjectService projectService =
                mock(com.sugon.testplatform.service.ProjectService.class);
        StatsServiceImpl service = new StatsServiceImpl(mapper, projectService);
        Map<String, Object> expected = Map.of(
                "AI", 5L, "CPU", 3L, "分布式存储", 2L,
                "AI4S", 3L, "GV+AI软件", 2L, "国产IB", 1L);
        assertEquals(expected, service.testTypeDistribution());
        assertEquals(expected, service.dimensionStats("testType"));
        assertEquals(expected, service.dashboard().get("testType"));
    }
}
