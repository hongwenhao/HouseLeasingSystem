package com.houseleasing.dto;

import lombok.Data;

@Data
public class ContractGenerateRequest {
    private Long orderId;
    private String additionalClauses;
}
