package com.houseleasing.activiti;

import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify Activiti workflow completes for contract signing.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=LEGACY;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.data.redis.repositories.enabled=false"
})
class WorkflowServiceTests {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private TaskService taskService;

    @Test
    void contractSigningFlowCompletes() {
        Long tenantId = 10L;
        Long landlordId = 20L;
        String instanceId = workflowService.startContractSigningProcess(100L, tenantId, landlordId);
        assertNotNull(instanceId);

        Task tenantTask = taskService.createTaskQuery()
                .processInstanceId(instanceId)
                .taskAssignee(String.valueOf(tenantId))
                .singleResult();
        assertNotNull(tenantTask);
        workflowService.completeContractTask(instanceId, tenantId, "TENANT", true);

        Task landlordTask = taskService.createTaskQuery()
                .processInstanceId(instanceId)
                .taskAssignee(String.valueOf(landlordId))
                .singleResult();
        assertNotNull(landlordTask);
        workflowService.completeContractTask(instanceId, landlordId, "LANDLORD", true);

        assertTrue(workflowService.isProcessFinished(instanceId));
    }
}
