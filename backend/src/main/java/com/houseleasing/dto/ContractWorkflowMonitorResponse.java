package com.houseleasing.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同流程可视化监控响应对象。
 *
 * <p>用于前端在“合同详情”页面绘制流程图和节点状态，
 * 包含流程整体状态、当前活动节点、查询时间与节点明细。</p>
 */
@Data
public class ContractWorkflowMonitorResponse {

    /** 合同绑定的流程实例 ID */
    private String processInstanceId;

    /** 流程是否已经结束（true=已结束，false=运行中） */
    private Boolean finished;

    /** 当前活动节点 ID（流程结束时可能为空） */
    private String currentNodeId;

    /** 当前活动节点名称（流程结束时可能为空） */
    private String currentNodeName;

    /** 本次监控数据查询时间（用于前端实时停留时长计算） */
    private LocalDateTime queryTime;

    /** 流程节点监控明细（按流程图顺序排列） */
    private List<NodeMonitorItem> nodes;

    /**
     * 流程节点监控明细项。
     */
    @Data
    public static class NodeMonitorItem {
        /** BPMN 节点 ID（taskDefinitionKey） */
        private String nodeId;
        /** BPMN 节点显示名称 */
        private String nodeName;
        /**
         * 节点状态：
         * COMPLETED=已完成、ACTIVE=处理中（当前停留）、PENDING=未到达
         */
        private String status;
        /** 处理人用户 ID（字符串形式，与 Activiti assignee 对齐） */
        private String assigneeId;
        /** 处理人显示名称（业务层补充） */
        private String assigneeName;
        /** 节点开始时间（进入节点时刻） */
        private LocalDateTime enterTime;
        /** 节点完成时间（仅已完成节点有值） */
        private LocalDateTime completeTime;
        /** 节点停留时长（毫秒） */
        private Long stayDurationMs;
    }
}
