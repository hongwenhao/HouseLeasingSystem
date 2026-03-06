package com.houseleasing.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderCreateRequest {
    private Long houseId;
    private String orderType;
    private LocalDateTime appointmentTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remark;
}
