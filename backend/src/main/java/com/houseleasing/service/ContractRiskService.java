package com.houseleasing.service;

import java.util.List;

public interface ContractRiskService {

    record RiskItem(String type, String description, String level) {}

    List<RiskItem> analyzeRisk(String contractText, java.math.BigDecimal monthlyRent, java.math.BigDecimal deposit);
}
