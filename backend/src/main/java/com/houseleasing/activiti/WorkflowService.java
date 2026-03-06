package com.houseleasing.activiti;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Workflow service stub - provides workflow operations interface.
 * Full Activiti integration can be enabled by adding activiti-spring-boot-starter dependency.
 */
@Slf4j
@Service
public class WorkflowService {

    public String startHouseApprovalProcess(Long houseId, Long ownerId) {
        log.info("Starting house approval process for house {} by owner {}", houseId, ownerId);
        // Return a pseudo process instance id
        return "house-approval-" + houseId + "-" + System.currentTimeMillis();
    }

    public void approveHouseProcess(String taskId, boolean approved, String comment) {
        log.info("Approving house process task {}: approved={}, comment={}", taskId, approved, comment);
    }

    public List<String> getHouseApprovalTask(Long houseId) {
        log.info("Getting house approval tasks for house {}", houseId);
        return List.of();
    }

    public String startContractSigningProcess(Long contractId) {
        log.info("Starting contract signing process for contract {}", contractId);
        return "contract-signing-" + contractId + "-" + System.currentTimeMillis();
    }

    public void completeContractTask(String taskId, boolean approved) {
        log.info("Completing contract task {}: approved={}", taskId, approved);
    }
}
