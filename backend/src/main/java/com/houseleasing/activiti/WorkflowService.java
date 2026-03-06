package com.houseleasing.activiti;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工作流服务（存根实现）
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供工作流操作的接口定义和存根实现，模拟房源审核和合同签署的流程管理。
 *              完整的 Activiti 工作流集成可通过添加 activiti-spring-boot-starter 依赖来启用。
 */
@Slf4j
@Service
public class WorkflowService {

    /**
     * 启动房源审核工作流
     * 生成并返回伪造的流程实例 ID，实际项目中应调用 Activiti 引擎启动流程
     *
     * @param houseId 需要审核的房源 ID
     * @param ownerId 提交审核的房东用户 ID
     * @return 流程实例 ID 字符串
     */
    public String startHouseApprovalProcess(Long houseId, Long ownerId) {
        log.info("Starting house approval process for house {} by owner {}", houseId, ownerId);
        // 返回伪造的流程实例 ID（格式：house-approval-{houseId}-{时间戳}）
        return "house-approval-" + houseId + "-" + System.currentTimeMillis();
    }

    /**
     * 处理房源审核任务（批准或拒绝）
     *
     * @param taskId   工作流任务 ID
     * @param approved true 表示批准，false 表示拒绝
     * @param comment  审核意见备注
     */
    public void approveHouseProcess(String taskId, boolean approved, String comment) {
        log.info("Approving house process task {}: approved={}, comment={}", taskId, approved, comment);
    }

    /**
     * 查询指定房源的待处理审核任务列表
     *
     * @param houseId 房源 ID
     * @return 待处理的任务 ID 列表（存根实现返回空列表）
     */
    public List<String> getHouseApprovalTask(Long houseId) {
        log.info("Getting house approval tasks for house {}", houseId);
        return List.of();
    }

    /**
     * 启动合同签署工作流
     * 生成并返回伪造的流程实例 ID
     *
     * @param contractId 需要签署的合同 ID
     * @return 流程实例 ID 字符串
     */
    public String startContractSigningProcess(Long contractId) {
        log.info("Starting contract signing process for contract {}", contractId);
        return "contract-signing-" + contractId + "-" + System.currentTimeMillis();
    }

    /**
     * 完成合同签署任务
     *
     * @param taskId   工作流任务 ID
     * @param approved true 表示同意签署，false 表示拒绝签署
     */
    public void completeContractTask(String taskId, boolean approved) {
        log.info("Completing contract task {}: approved={}", taskId, approved);
    }
}
