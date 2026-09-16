package com.sugon.testplatform.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleRequest {
    private Long appId;
    private LocalDate scheduleStartTime;
    private LocalDate scheduleEndTime;
    private String scheduleRemark;
}
