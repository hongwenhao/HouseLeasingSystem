package com.houseleasing.config;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Activiti 工作流引擎配置。
 * 使用 Spring 管理的 DataSource 和事务管理器创建流程引擎，
 * 并自动部署 resources/processes 目录下的 BPMN 模型。
 * <p>
 * 设计目标：
 * 避免手工初始化流程引擎，统一交给 Spring 生命周期管理。
 * 通过统一的事务管理器保证流程状态与业务数据的一致性。
 * 在应用启动时自动加载 BPMN 文件，减少部署步骤。
 */
@Configuration
public class ActivitiConfig {

    /**
     * BPMN 模型文件扫描路径。
     * 约定将流程定义统一放在 classpath:/processes 目录下，文件后缀为 .bpmn20.xml。
     */
    private static final String BPMN_RESOURCE_PATTERN = "classpath*:/processes/*.bpmn20.xml";

    /**
     * 构建 Activiti 与 Spring 集成的核心配置对象。
     * <p>
     * 关键配置说明：
     * <p>
     * databaseSchemaUpdate=TRUE：启动时自动创建/更新 Activiti 所需表结构。
     * deploymentResources：自动部署流程定义文件。
     * historyLevel=AUDIT：记录流程与任务的关键历史，满足审计与追踪需求。
     *
     * @param dataSource         应用主数据源（与业务库复用）
     * @param transactionManager Spring 事务管理器
     * @return SpringProcessEngineConfiguration 实例
     * @throws Exception 读取流程资源失败时抛出
     */
    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(DataSource dataSource,
                                                                             PlatformTransactionManager transactionManager)
            throws Exception {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();//构建配置对象
        configuration.setDataSource(dataSource);//将 Spring 管理的数据源注入给 Activiti
        configuration.setTransactionManager(transactionManager);//将 Spring 的事务管理器注入给 Activiti
        configuration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);//引擎启动时，会自动检查数据库。如果 Activiti 的表不存在，则自动创建；如果已存在但结构与当前引擎版本不匹配，则自动更新表结构。
        configuration.setDeploymentResources(
                new PathMatchingResourcePatternResolver().getResources(BPMN_RESOURCE_PATTERN));//一次性找到所有匹配的流程文件
        configuration.setHistoryLevel(HistoryLevel.AUDIT);//配置历史级别为AUDIT，记录流程实例的关键历史数据（如流程启动、任务完成等），满足审计和追踪需求，同时避免过多无用的历史数据。
        return configuration;
    }

    /**
     * 创建 ProcessEngine 工厂 Bean。
     * 由工厂负责按配置构建流程引擎单例，避免业务代码直接 new 引擎对象。
     *
     * @param configuration 已完成注入的流程引擎配置
     * @return ProcessEngineFactoryBean
     */
    @Bean
    public ProcessEngineFactoryBean processEngine(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();//创建并配置工厂
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean;
    }

    /**
     * 暴露 RepositoryService 用于流程定义管理（部署、查询流程模型等）。
     *
     * @param processEngine 流程引擎
     * @return RepositoryService
     */
    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * 暴露 RuntimeService 用于流程实例运行时操作（启动流程、查询执行实例等）。
     *
     * @param processEngine 流程引擎
     * @return RuntimeService
     */
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    /**
     * 暴露 TaskService 用于用户任务处理（签收、完成、查询待办等）。
     *
     * @param processEngine 流程引擎
     * @return TaskService
     */
    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    /**
     * 暴露 HistoryService 用于历史数据查询（已完成流程、历史任务审计等）。
     *
     * @param processEngine 流程引擎
     * @return HistoryService
     */
    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }
}
