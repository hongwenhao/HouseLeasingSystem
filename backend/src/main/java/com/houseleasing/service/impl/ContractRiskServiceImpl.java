package com.houseleasing.service.impl;

import com.houseleasing.service.ContractRiskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ContractRiskServiceImpl implements ContractRiskService {

    @Override
    public List<RiskItem> analyzeRisk(String contractText, BigDecimal monthlyRent, BigDecimal deposit) {
        List<RiskItem> risks = new ArrayList<>();
        if (contractText == null) return risks;

        // 1. Check if deposit exceeds two months rent
        if (monthlyRent != null && deposit != null) {
            if (deposit.compareTo(monthlyRent.multiply(new BigDecimal("2"))) > 0) {
                risks.add(new RiskItem("押金过高", "押金超过两个月租金，请注意资金安全", "HIGH"));
            }
        }

        // 2. Check for maintenance responsibility clause
        if (!contractText.contains("维修") && !contractText.contains("修缮")) {
            risks.add(new RiskItem("条款缺失", "合同未约定维修责任，建议补充相关条款", "MEDIUM"));
        }

        // 3. Check for early termination clause
        if (!contractText.contains("提前解约") && !contractText.contains("提前终止")) {
            risks.add(new RiskItem("条款缺失", "合同未约定提前解约条款，建议明确违约处理方式", "MEDIUM"));
        }

        // 4. Check penalty clauses (look for penalty percentage or amount)
        if (contractText.contains("违约金") && monthlyRent != null) {
            // Simple heuristic: if "3个月" or "三个月" appears near "违约金", flag it
            if (contractText.contains("违约金") &&
                    (contractText.contains("4个月") || contractText.contains("5个月") ||
                     contractText.contains("四个月") || contractText.contains("五个月"))) {
                risks.add(new RiskItem("违约金过高", "违约金条款可能超过3个月租金，请仔细核查", "HIGH"));
            }
        }

        // 5. Check if water/electricity fee standards are mentioned
        if (!contractText.contains("水费") && !contractText.contains("电费")) {
            risks.add(new RiskItem("费用未约定", "合同未约定水电费标准，建议明确收费标准", "LOW"));
        }

        return risks;
    }
}
