/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *    Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.luokuiai.flovira.plugin.modes.sb.config;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.enums.FrameworkType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.lock.TimeoutSchedulerLock;
import com.luokuiai.flovira.core.orm.dao.*;
import com.luokuiai.flovira.core.service.*;
import com.luokuiai.flovira.core.service.impl.*;
import com.luokuiai.flovira.core.utils.ExpressionUtil;
import com.luokuiai.flovira.orm.dao.*;
import com.luokuiai.flovira.orm.entity.*;
import com.luokuiai.flovira.plugin.modes.sb.expression.*;
import com.luokuiai.flovira.plugin.modes.sb.helper.SpelHelper;
import com.luokuiai.flovira.plugin.modes.sb.utils.SpringUtil;
import com.luokuiai.flovira.plugin.modes.sb.transaction.SpringTransactionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Objects;

/**
 * 工作流bean注册配置
 *
 * @author warm
 * @since 2023/6/5 23:01
 */
@SuppressWarnings("rawtypes unchecked")
@Import({SpringUtil.class, SpelHelper.class, TimeoutSchedulingConfig.class})
@ConditionalOnProperty(value = "flovira.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FloviraProperties.class)
public class BeanConfig {

    private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

    @Bean
    public FlowDefinitionDao definitionDao() {
        return new FlowDefinitionDaoImpl();
    }

    @Bean
    public DefService definitionService(FlowDefinitionDao definitionDao) {
        return new DefServiceImpl().setDao(definitionDao);
    }

    @Bean
    public ChartService chartService() {
        return new ChartServiceImpl();
    }

    @Bean
    public FlowNodeDao nodeDao() {
        return new FlowNodeDaoImpl();
    }

    @Bean
    public NodeService nodeService(FlowNodeDao nodeDao) {
        return new NodeServiceImpl().setDao(nodeDao);
    }

    @Bean
    public FlowSkipDao skipDao() {
        return new FlowSkipDaoImpl();
    }

    @Bean
    public SkipService skipService(FlowSkipDao skipDao) {
        return new SkipServiceImpl().setDao(skipDao);
    }

    @Bean
    public FlowInstanceDao instanceDao() {
        return new FlowInstanceDaoImpl();
    }

    @Bean
    public InsService instanceService(FlowInstanceDao instanceDao) {
        return new InsServiceImpl().setDao(instanceDao);
    }

    @Bean
    public FlowTaskDao taskDao() {
        return new FlowTaskDaoImpl();
    }

    @Bean
    public TaskService taskService(FlowTaskDao taskDao) {
        return new TaskServiceImpl().setDao(taskDao);
    }

    @Bean
    public WaitService waitService() {
        return new WaitServiceImpl();
    }

    @Bean
    public TimeoutService timeoutService() {
        return new TimeoutServiceImpl();
    }

    @Bean
    public ProgressService progressService() {
        return new ProgressServiceImpl();
    }

    @Bean
    public FlowHisTaskDao hisTaskDao() {
        return new FlowHisTaskDaoImpl();
    }

    @Bean
    public HisTaskService hisTaskService(FlowHisTaskDao hisTaskDao) {
        return new HisTaskServiceImpl().setDao(hisTaskDao);
    }

    @Bean
    public FlowUserDao flowUserDao() {
        return new FlowUserDaoImpl();
    }

    @Bean
    public UserService flowUserService(FlowUserDao userDao) {
        return new UserServiceImpl().setDao(userDao);
    }

    @Bean
    public FlowFormDao formDao() {
        return new FlowFormDaoImpl();
    }

    @Bean
    public FormService flowFormService(FlowFormDao formDao) {
        return new FormServiceImpl().setDao(formDao);
    }

    @Bean
    public FlowSubprocessRunDao subprocessRunDao() {
        return new FlowSubprocessRunDaoImpl();
    }

    @Bean
    public FlowSubprocessChildDao subprocessChildDao() {
        return new FlowSubprocessChildDaoImpl();
    }

    @Bean
    public FlowSubprocessEventDao subprocessEventDao() {
        return new FlowSubprocessEventDaoImpl();
    }

    @Bean
    public SubprocessService subprocessService(FlowSubprocessRunDao runDao, FlowSubprocessChildDao childDao,
        FlowSubprocessEventDao eventDao) {
        return new SubprocessServiceImpl().setDao(runDao, childDao, eventDao);
    }

    @Bean
    public com.luokuiai.flovira.core.transaction.TransactionExecutor transactionExecutor(
        PlatformTransactionManager transactionManager) {
        return new SpringTransactionExecutor(transactionManager);
    }

    @Bean
    public Flovira initFlow() {
        setNewEntity();
        FrameInvoker.setCfgFunction((key) -> Objects.requireNonNull(SpringUtil.getBean(Environment.class)).getProperty(key));
        FrameInvoker.setBeanFunction(SpringUtil::getBean);
        FlowEngine.setTransactionExecutor(SpringUtil.getBean(com.luokuiai.flovira.core.transaction.TransactionExecutor.class));
        FlowEngine.setTimeoutSchedulerLock(FrameInvoker.getBean(TimeoutSchedulerLock.class));
        FloviraProperties flovira = SpringUtil.getBean(FloviraProperties.class);
        flovira.init();
        flovira.setFramework(FrameworkType.SPRING_BOOT);
        FlowEngine.setFlowConfig(flovira);
        setExpression();
        after(flovira);
        log.info("[flovira] loaded successfully");
        return flovira;
    }

    private void setExpression() {
        ExpressionUtil.setExpression(new ConditionStrategyDefault());
        ExpressionUtil.setExpression(new ConditionStrategySpel());
        ExpressionUtil.setExpression(new ListenerStrategySpel());
        ExpressionUtil.setExpression(new HandlerStrategySpel());
        ExpressionUtil.setExpression(new VoteSignStrategyDefault());
        ExpressionUtil.setExpression(new VoteSignStrategySpel());
    }

    public void setNewEntity() {
        FlowEngine.setNewDef(FlowDefinition::new);
        FlowEngine.setNewIns(FlowInstance::new);
        FlowEngine.setNewHisTask(FlowHisTask::new);
        FlowEngine.setNewNode(FlowNode::new);
        FlowEngine.setNewSkip(FlowSkip::new);
        FlowEngine.setNewTask(FlowTask::new);
        FlowEngine.setNewUser(FlowUser::new);
        FlowEngine.setNewForm(FlowForm::new);
        FlowEngine.setNewSubprocessRun(FlowSubprocessRun::new);
        FlowEngine.setNewSubprocessChild(FlowSubprocessChild::new);
        FlowEngine.setNewSubprocessEvent(FlowSubprocessEvent::new);
    }

    public void after(Flovira flowConfig) {
    }
}
