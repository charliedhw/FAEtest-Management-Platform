package com.sugon.testplatform.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProgressRequest {
    private Long projectId;
    private Long stageId;
    private LocalDate progressDate;
    private String content;
}
