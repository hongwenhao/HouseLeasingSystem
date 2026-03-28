package com.houseleasing.activiti;

import com.houseleasing.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流服务
 *
 * 使用 Activiti 引擎管理房源审核和合同签署的 BPMN 流程，
 * 封装流程启动、任务完成、状态查询等操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    /**
     * 启动房源审核流程
     *
     * @param houseId 房源 ID
     * @param ownerId 房东用户 ID
     * @return 流程实例 ID
     */
    public String startHouseApprovalProcess(Long houseId, Long ownerId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("houseId", houseId);
        variables.put("ownerId", ownerId);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "houseApprovalProcess",
                "HOUSE-" + houseId,
                variables);
        log.info("Started houseApprovalProcess instance {} for house {}", instance.getProcessInstanceId(), houseId);
        return instance.getProcessInstanceId();
    }

    /**
     * 管理员审核房源并完成当前任务
     *
     * @param processInstanceId 流程实例 ID
     * @param approved          是否通过
     * @param comment           审核意见
     */
    public void approveHouseProcess(String processInstanceId, boolean approved, String comment) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskCandidateGroup("admin")
                .singleResult();
        if (task == null) {
            throw new BusinessException(404, "未找到房源审核任务");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", approved);
        vars.put("comment", StringUtils.hasText(comment) ? comment : "");
        taskService.complete(task.getId(), vars);
        log.info("Completed house approval task {} with result {}", task.getId(), approved);
    }

    /**
     * 查询某房源的待处理审核任务
     *
     * @param houseId 房源 ID
     * @return 待处理任务 ID 列表
     */
    public List<String> getHouseApprovalTask(Long houseId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey("houseApprovalProcess")
                .processVariableValueEquals("houseId", houseId)
                .active()
                .list();
        return tasks.stream().map(Task::getId).toList();
    }

    /**
     * 启动合同签署流程
     *
     * @param contractId 合同 ID
     * @param tenantId   租客 ID
     * @param landlordId 房东 ID
     * @return 流程实例 ID
     */
    public String startContractSigningProcess(Long contractId, Long tenantId, Long landlordId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("contractId", contractId);
        variables.put("tenantId", tenantId);
        variables.put("landlordId", landlordId);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "contractSigningProcess",
                "CONTRACT-" + contractId,
                variables);
        log.info("Started contractSigningProcess instance {} for contract {}", instance.getProcessInstanceId(), contractId);
        return instance.getProcessInstanceId();
    }

    /**
     * 完成合同签署任务
     *
     * @param processInstanceId 流程实例 ID
     * @param userId            当前签署人 ID
     * @param role              签署角色 TENANT / LANDLORD
     * @param approved          是否同意签署
     */
    public void completeContractTask(String processInstanceId, Long userId, String role, boolean approved) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .singleResult();
        if (task == null) {
            throw new BusinessException(404, "未找到合同签署任务");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", approved);
        if ("TENANT".equals(role)) {
            vars.put("tenantSigned", approved);
        } else if ("LANDLORD".equals(role)) {
            vars.put("landlordSigned", approved);
        }
        taskService.complete(task.getId(), vars);
        log.info("User {} completed contract task {} with role {}", userId, task.getId(), role);
    }

    /**
     * 判断流程是否已经结束
     *
     * @param processInstanceId 流程实例 ID
     * @return true 表示已结束
     */
    public boolean isProcessFinished(String processInstanceId) {
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime != null) {
            return false;
        }
        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .singleResult();
        return history != null;
    }
}
