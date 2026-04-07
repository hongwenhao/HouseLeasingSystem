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

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流服务
 *
 * 使用 Activiti 引擎管理合同签署的 BPMN 流程，
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
     * 启动合同签署流程
     *
     * @param contractId 合同 ID
     * @param tenantId   租客 ID
     * @param landlordId 房东 ID
     * @return 流程实例 ID
     */
    public String startContractSigningProcess(Long contractId, Long tenantId, Long landlordId) {
        Map<String, Object> variables = new HashMap<>();
        //组装流程变量，供后续用户任务节点读取使用：
        //contractId：用于在签署回调时反查业务合同；
        //tenantId / landlordId：驱动 BPMN 中两个 userTask 的 assignee 绑定。
        variables.put("contractId", contractId);
        variables.put("tenantId", tenantId);
        variables.put("landlordId", landlordId);
        //启动流程，并设置业务主键为 CONTRACT-{id}：
        //业务侧可按 businessKey 直接追溯流程实例；
        //避免仅靠流程实例 ID（随机值）导致排障时难以定位具体合同。
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
        // 1. 查询待办任务
        // 构建任务查询条件：匹配指定的流程实例ID以及当前用户ID（需转为String以匹配Assignee字段）
        // singleResult() 确保查询结果唯一，如果无结果返回null，多于一个结果则抛出异常
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .singleResult();
        // 2. 校验任务是否存在
        // 如果查询结果为空，说明当前用户在该流程中没有待处理的签署任务，抛出业务异常
        if (task == null) {
            throw new BusinessException(404, "未找到合同签署任务");
        }
        // 3. 准备流程变量
        // 用于在完成任务时回写给工作流引擎，驱动流程流转或更新流程状态。
        // 约定：
        // - approved = false 表示本次签署拒绝，流程可在网关中走驳回/结束分支；
        // - approved = true  表示当前节点签署同意，流程继续推进到下一节点。
        Map<String, Object> vars = new HashMap<>();
        // 设置通用的审批结果变量，供流程网关判断走向（如：通过则进入下一节点，拒绝则结束或驳回）
        vars.put("approved", approved);
        // 4. 根据角色设置特定的签署状态变量
        // 这种设计允许流程中记录每一方的独立签署状态，便于后续查询或并行签署逻辑
        if ("TENANT".equals(role)) {
            vars.put("tenantSigned", approved);
            // 如果是租客角色，设置租客已签署变量
        } else if ("LANDLORD".equals(role)) {
            vars.put("landlordSigned", approved);
        }
        // 5. 完成任务
        // 调用工作流服务，传入任务ID和流程变量集合，正式结束当前任务节点
        taskService.complete(task.getId(), vars);
        // 6. 记录操作日志
        // 记录用户ID、任务ID和角色，便于后续审计和问题追踪
        log.info("User {} completed contract task {} with role {}", userId, task.getId(), role);
    }

    /**
     * 判断流程是否已经结束
     *
     * @param processInstanceId 流程实例 ID
     * @return true 表示已结束
     */
    public boolean isProcessFinished(String processInstanceId) {
        // 优先查 runtime：只要还能查到实例，说明流程仍在运行（包括待办/并行分支未结束）。
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime != null) {
            return false;
        }
        // runtime 查不到时再查 history.finished：
        // - 命中表示流程已自然结束；
        // - 未命中通常表示 processInstanceId 非法或历史清理后不可见。
        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .singleResult();
        return history != null;
    }
}
