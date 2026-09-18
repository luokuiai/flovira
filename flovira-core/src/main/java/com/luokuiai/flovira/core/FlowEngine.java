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
package com.luokuiai.flovira.core;

import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.entity.*;
import com.luokuiai.flovira.core.handler.ApproverResolver;
import com.luokuiai.flovira.core.handler.DataFillHandler;
import com.luokuiai.flovira.core.handler.FormFieldProvider;
import com.luokuiai.flovira.core.handler.PermissionHandler;
import com.luokuiai.flovira.core.handler.TenantHandler;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleListenerRegistry;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleDispatcher;
import com.luokuiai.flovira.core.service.*;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import com.luokuiai.flovira.core.utils.ClassUtil;
import com.luokuiai.flovira.core.utils.ObjectUtil;
import com.luokuiai.flovira.core.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import com.luokuiai.flovira.core.dto.ApproverStrategyDefinition;
import java.util.function.Supplier;

/**
 * 流程引擎
 *
 * @author warm
 */
public class FlowEngine {

    private static final LifecycleListenerRegistry lifecycleListeners = new LifecycleListenerRegistry();

    private static volatile LifecycleDispatcher.FailureHandler lifecycleFailureHandler = (code, event, failure) ->
        org.slf4j.LoggerFactory.getLogger(FlowEngine.class).error(
            "Lifecycle notification failed: listener={}, event={}", code, event.getEventId(), failure);
    private static final LifecycleDispatcher lifecycleDispatcher = new LifecycleDispatcher(lifecycleListeners,
        (code, event, failure) -> lifecycleFailureHandler.failed(code, event, failure));

    public static LifecycleDispatcher lifecycleDispatcher() { return lifecycleDispatcher; }

    public static void setLifecycleFailureHandler(LifecycleDispatcher.FailureHandler handler) {
        lifecycleFailureHandler = java.util.Objects.requireNonNull(handler, "handler");
    }

    /** 框架独立的代码回调注册入口，支持多个监听器实例。 */
    public static LifecycleListenerRegistry lifecycleListeners() {
        return lifecycleListeners;
    }

    private static final DefService defService = null;
    private static final NodeService nodeService = null;
    private static final SkipService skipService = null;
    private static final InstanceService instanceService = null;
    private static final TaskService taskService = null;
    private static final HisTaskService hisTaskService = null;
    private static final UserService userService = null;
    private static final FormService formService = null;
    private static final ChartService chartService = null;
    private static final SubprocessService subprocessService = null;
    private static final WaitService waitService = null;
    private static final TimeoutService timeoutService = null;
    private static final ProgressService progressService = null;

    private static Supplier<Definition> defSupplier;
    private static Supplier<Node> nodeSupplier;
    private static Supplier<Skip> skipSupplier;
    private static Supplier<Instance> insSupplier;
    private static Supplier<Task> taskSupplier;
    private static Supplier<HisTask> hisTaskSupplier;
    private static Supplier<User> userSupplier;
    private static Supplier<Form> formSupplier;
    private static Supplier<SubprocessRun> subprocessRunSupplier;
    private static Supplier<SubprocessChild> subprocessChildSupplier;
    private static Supplier<SubprocessEvent> subprocessEventSupplier;
    private static Supplier<NodeExecution> nodeExecutionSupplier;

    public static void setNewNodeExecution(Supplier<NodeExecution> supplier) {
        nodeExecutionSupplier = supplier;
    }

    public static NodeExecution newNodeExecution() {
        if (nodeExecutionSupplier == null) throw new IllegalStateException("Node execution supplier is not configured");
        return nodeExecutionSupplier.get();
    }

    private static TransactionExecutor transactionExecutor;

    private static Flovira flowConfig;

    private static DataFillHandler dataFillHandler;

    private static TenantHandler tenantHandler;

    private static PermissionHandler permissionHandler;


    public static JsonConvert jsonConvert;

    public static DefService defService() {
        return getObj(defService, DefService.class);
    }

    public static NodeService nodeService() {
        return getObj(nodeService, NodeService.class);
    }

    public static SkipService skipService() {
        return getObj(skipService, SkipService.class);
    }

    public static InstanceService instanceService() {
        return getObj(instanceService, InstanceService.class);
    }

    public static TaskService taskService() {
        return getObj(taskService, TaskService.class);
    }

    public static HisTaskService hisTaskService() {
        return getObj(hisTaskService, HisTaskService.class);
    }

    public static UserService userService() {
        return getObj(userService, UserService.class);
    }

    public static FormService formService() {
        return getObj(formService, FormService.class);
    }

    public static ChartService chartService() {
        return getObj(chartService, ChartService.class);
    }

    public static SubprocessService subprocessService() {
        return getObj(subprocessService, SubprocessService.class);
    }

    public static WaitService waitService() {
        return getObj(waitService, WaitService.class);
    }

    public static TimeoutService timeoutService() {
        return getObj(timeoutService, TimeoutService.class);
    }

    public static ProgressService progressService() {
        return getObj(progressService, ProgressService.class);
    }

    public static void setNewDef(Supplier<Definition> supplier) {
        FlowEngine.defSupplier = supplier;
    }

    public static Definition newDef() {
        return defSupplier.get();
    }

    public static void setNewNode(Supplier<Node> supplier) {
        FlowEngine.nodeSupplier = supplier;
    }

    public static Node newNode() {
        return nodeSupplier.get();
    }

    public static void setNewSkip(Supplier<Skip> supplier) {
        FlowEngine.skipSupplier = supplier;
    }

    public static Skip newSkip() {
        return skipSupplier.get();
    }

    public static void setNewIns(Supplier<Instance> supplier) {
        FlowEngine.insSupplier = supplier;
    }

    public static Instance newIns() {
        return insSupplier.get();
    }

    public static void setNewTask(Supplier<Task> supplier) {
        FlowEngine.taskSupplier = supplier;
    }

    public static Task newTask() {
        return taskSupplier.get();
    }

    public static void setNewHisTask(Supplier<HisTask> supplier) {
        FlowEngine.hisTaskSupplier = supplier;
    }

    public static HisTask newHisTask() {
        return hisTaskSupplier.get();
    }

    public static void setNewUser(Supplier<User> supplier) {
        FlowEngine.userSupplier = supplier;
    }

    public static User newUser() {
        return userSupplier.get();
    }

    public static void setNewForm(Supplier<Form> supplier) {
        FlowEngine.formSupplier = supplier;
    }

    public static Form newForm() {
        return formSupplier.get();
    }

    public static void setNewSubprocessRun(Supplier<SubprocessRun> supplier) {
        subprocessRunSupplier = supplier;
    }

    public static SubprocessRun newSubprocessRun() {
        return subprocessRunSupplier.get();
    }

    public static void setNewSubprocessChild(Supplier<SubprocessChild> supplier) {
        subprocessChildSupplier = supplier;
    }

    public static SubprocessChild newSubprocessChild() {
        return subprocessChildSupplier.get();
    }

    public static void setNewSubprocessEvent(Supplier<SubprocessEvent> supplier) {
        subprocessEventSupplier = supplier;
    }

    public static SubprocessEvent newSubprocessEvent() {
        return subprocessEventSupplier.get();
    }

    public static void setTransactionExecutor(TransactionExecutor executor) {
        transactionExecutor = executor;
    }

    public static TransactionExecutor transactionExecutor() {
        if (transactionExecutor == null) {
            throw new IllegalStateException("Subprocess transaction executor is not configured");
        }
        return transactionExecutor;
    }


    /**
     * 可选的业务表单字段名称提供者，通过 FrameInvoker 注册。
     */
    public static FormFieldProvider formFieldProvider() {
        return getObj(null, FormFieldProvider.class);
    }

    /**
     * 按策略编码查找接入方注册的办理人解析器。
     *
     * @param strategy 策略编码
     * @return 对应的业务解析器；未注册时明确报错
     */
    public static ApproverResolver approverResolver(String strategy) {
        ApproverResolver resolver = approverResolvers().get(strategy);
        if (resolver == null) {
            throw new IllegalStateException("Unregistered approver strategy: " + strategy);
        }
        return resolver;
    }

    /** 同一注册表用于设计器声明、保存校验、运行时和预览。 */
    public static Map<String, ApproverResolver> approverResolvers() {
        Map<String, ApproverResolver> result = new LinkedHashMap<String, ApproverResolver>();
        for (ApproverResolver resolver : FrameInvoker.getBeans(ApproverResolver.class)) {
            if (resolver == null) {
                throw new IllegalStateException("Null approver resolver");
            }
            String code = resolver.getStrategy();
            ApproverStrategyDefinition descriptor = resolver.getDefinition();
            if (code == null || code.trim().isEmpty() || !code.equals(code.trim())
                || descriptor == null || !code.equals(descriptor.getCode())
                || descriptor.getVersion() < 1 || descriptor.getName() == null
                || descriptor.getName().trim().isEmpty()) {
                throw new IllegalStateException("Invalid approver strategy definition: " + code);
            }
            if (result.put(code, resolver) != null) {
                throw new IllegalStateException("Duplicate approver strategy: " + code);
            }
        }
        return result;
    }

    public static Flovira getFlowConfig() {
        return FlowEngine.flowConfig;
    }

    public static void setFlowConfig(Flovira flowConfig) {
        FlowEngine.flowConfig = flowConfig;
    }

    public static void initDataFillHandler(String handlerPath) {
        dataFillHandler = initBean(DataFillHandler.class, handlerPath, () -> new DataFillHandler() {});
    }

    public static void initTenantHandler(String handlerPath) {
        tenantHandler = initBean(TenantHandler.class, handlerPath, null);
    }

    public static void initPermissionHandler(String handlerPath) {
        permissionHandler = initBean(PermissionHandler.class, handlerPath, null);
    }


    /**
     * 获取填充类
     */
    public static DataFillHandler dataFillHandler() {
        return dataFillHandler;
    }

    /**
     * 获取填充类
     */
    public static PermissionHandler permissionHandler() {
        return permissionHandler;
    }

    /**
     * 获取租户数据
     */
    public static TenantHandler tenantHandler() {
        return tenantHandler;
    }

    /**
     * 获取数据库类型
     */
    public static String dataSourceType() {
        return flowConfig.getDataSourceType();
    }

    public static <T> T getObj(T t, Class<T> tClass) {
        if (ObjectUtil.isNotNull(t)) {
            return t;
        }
        t = FrameInvoker.getBean(tClass);
        return t;
    }

    /**
     * 初始化bean，先从yml配置获取bean的全包名路径，否则从spring容器获取bean，如果都没有，则通过supplier获取bean
     *
     * @param tClazz   bean的class类型
     * @param beanPath bean全包名路径
     * @param supplier 获取bean的lambda
     * @param <T>      bean类型
     * @return bean
     */
    private static <T> T initBean(Class<T> tClazz, String beanPath, Supplier<T> supplier) {
        T hander = null;
        try {
            if (!StringUtils.isEmpty(beanPath)) {
                Class<?> clazz = ClassUtil.getClazz(beanPath);
                if (clazz != null && tClazz.isAssignableFrom(clazz)) {
                    Constructor<?> constructor = clazz.getConstructor();
                    hander = tClazz.cast(constructor.newInstance());
                }
            }
        } catch (Exception ignored) {
        }
        if (hander == null) {
            hander = FrameInvoker.getBean(tClazz);
        }
        if (hander == null && supplier != null) {
            hander = supplier.get();
        }
        return hander;
    }

}
