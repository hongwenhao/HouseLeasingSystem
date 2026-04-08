package com.houseleasing.service;

import java.util.List;

/**
 * 合同风险分析服务接口
 *
 * @author hongwenhao
 * @description 定义合同风险分析的业务操作，通过规则引擎对合同条款进行风险识别和评估
 */
public interface ContractRiskService { // 合同风控能力抽象：负责识别合同文本中的潜在风险

    /**
     * 合同风险条目记录类（Java 16+ Record 类型）
     *
     * @param type        风险类型描述
     * @param description 风险详细说明
     * @param level       风险等级：LOW（低）、MEDIUM（中）、HIGH（高）
     */
    record RiskItem(String type, String description, String level) {} // 一条风险结果：类型、描述、风险等级

    /**
     * 对合同文本进行风险分析
     * 检查押金比例、维修条款、提前解约条款、违约金条款、水电费约定等
     *
     * @param contractText 合同正文内容
     * @param monthlyRent  月租金（用于计算押金比例）
     * @param deposit      押金金额（用于计算押金比例）
     * @return 发现的风险条目列表
     */
    List<RiskItem> analyzeRisk(String contractText, java.math.BigDecimal monthlyRent, java.math.BigDecimal deposit); // 对合同内容进行规则化风险扫描
}
