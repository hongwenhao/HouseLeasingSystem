package com.houseleasing.service.impl;

import com.houseleasing.service.ContractRiskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同风险分析服务实现类
 *
 * @author hongwenhao
 * @description 基于规则引擎实现合同文本的风险识别，通过关键词匹配和数值计算
 *              检测押金比例、维修条款、解约条款、违约金和费用约定等风险点
 */
@Slf4j
@Service // 声明为 Spring 服务组件
public class ContractRiskServiceImpl implements ContractRiskService { // 合同风控规则的具体实现

    /**
     * 对合同文本进行多维度风险分析
     * 依次检查五类风险：押金比例、维修条款、提前解约、违约金额度、水电费约定
     *
     * @param contractText 合同正文文本
     * @param monthlyRent  月租金（用于计算押金倍数）
     * @param deposit      押金金额
     * @return 发现的风险条目列表
     */
    @Override
    public List<RiskItem> analyzeRisk(String contractText, BigDecimal monthlyRent, BigDecimal deposit) { // 执行合同文本风险扫描并返回风险清单
        List<RiskItem> risks = new ArrayList<>(); // 准备风险结果容器，后续命中规则就往这里追加
        // 合同正文为空时不做规则匹配，直接返回空风险列表：
        // - 避免空指针影响合同主流程；
        // - 风险分析属于“增值能力”，不应阻断合同生成/读取流程。
        if (contractText == null) return risks; // 没有合同文本就无法做关键词匹配，直接返回空结果

        // 风险一：检查押金是否超过两个月租金
        // 业务依据：本系统将“押金 > 2个月租金”视作资金占用风险，按 HIGH 提示用户重点核查。
        if (monthlyRent != null && deposit != null) { // 只有月租和押金都存在时，才有比较意义
            if (deposit.compareTo(monthlyRent.multiply(new BigDecimal("2"))) > 0) { // 计算“押金是否超过2个月租金”
                risks.add(new RiskItem("押金过高", "押金超过两个月租金，请注意资金安全", "HIGH")); // 命中高风险并给出提醒文案
            }
        }

        // 风险二：检查是否包含维修责任条款
        if (!contractText.contains("维修") && !contractText.contains("修缮")) { // 两个常见维修关键词都没出现，判定维修责任条款缺失
            risks.add(new RiskItem("条款缺失", "合同未约定维修责任，建议补充相关条款", "MEDIUM")); // 记录中风险，提示补齐条款
        }

        // 风险三：检查是否包含提前解约条款
        if (!contractText.contains("提前解约") && !contractText.contains("提前终止")) { // 未出现提前退出关键词，视为提前解约规则不明确
            risks.add(new RiskItem("条款缺失", "合同未约定提前解约条款，建议明确违约处理方式", "MEDIUM")); // 记录中风险，降低后续扯皮概率
        }

        // 风险四：检查违约金是否过高（简单启发式：查找含"4个月"/"5个月"违约金的描述）
        if (contractText.contains("违约金") && monthlyRent != null) { // 仅在存在违约金语境且有月租基准时才继续判断
            // 简单启发规则：如果违约金附近出现4个月或5个月的描述，则标记为高风险
            // 说明：该规则是关键词启发式，不做 NLP 语义解析，目标是“先发现明显高风险条款”。
            if (contractText.contains("违约金") && // 二次确认当前语境确实讨论违约金
                    (contractText.contains("4个月") || contractText.contains("5个月") ||
                      contractText.contains("四个月") || contractText.contains("五个月"))) { // 命中“4/5个月”这类明显偏高描述
                risks.add(new RiskItem("违约金过高", "违约金条款可能超过3个月租金，请仔细核查", "HIGH")); // 记录高风险，提醒重点复核
            }
        }

        // 风险五：检查水电费标准是否有明确约定
        // 若合同未出现水电费关键词，通常意味着费用承担与计费口径不清，后续易引发争议。
        if (!contractText.contains("水费") && !contractText.contains("电费")) { // 水电费关键词均缺失，判定费用标准未约定
            risks.add(new RiskItem("费用未约定", "合同未约定水电费标准，建议明确收费标准", "LOW")); // 记录低风险，提示完善细节条款
        }

        return risks; // 返回本次规则扫描得到的全部风险项
    }
}
