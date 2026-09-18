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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.constant.ExceptionCons;
import com.luokuiai.flovira.core.constant.FlowCons;
import com.luokuiai.flovira.core.dto.*;
import com.luokuiai.flovira.core.entity.*;
import com.luokuiai.flovira.core.enums.*;
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
import com.luokuiai.flovira.core.orm.dao.FlowInstanceDao;
import com.luokuiai.flovira.core.listener.lifecycle.ProcessLifecycleState;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleTransition;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleCallbackGuard;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleEventType;
import com.luokuiai.flovira.core.orm.service.impl.FloviraServiceImpl;
import com.luokuiai.flovira.core.service.TaskService;
import com.luokuiai.flovira.core.utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 待办任务Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
public class TaskServiceImpl extends FloviraServiceImpl<FlowTaskDao<Task>, Task> implements TaskService {

    private static final ThreadLocal<String> OPERATION_SOURCE = new ThreadLocal<String>();

    @Override
    public TaskService setDao(FlowTaskDao<Task> floviraDao) {
        this.floviraDao = floviraDao;
        return this;
    }

    @Override
    public Instance pass(Long taskId, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(SkipType.PASS.getKey(), message, variable));
    }

    @Override
    public Instance passAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.PASS.getKey(), message, variable));
    }

    @Override
    public Instance pass(Long taskId, String message, Map<String, Object> variable, String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(SkipType.PASS.getKey(), message, variable, flowStatus, hisStatus));
    }

    @Override
    public Instance passAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable
        , String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.PASS.getKey(), message, variable, flowStatus, hisStatus));
    }


    @Override
    public Instance reject(Long taskId, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(SkipType.REJECT.getKey(), message, variable));
    }

    @Override
    public Instance rejectAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.REJECT.getKey(), message, variable));
    }

    @Override
    public Instance reject(Long taskId, String message, Map<String, Object> variable, String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(SkipType.REJECT.getKey(), message, variable, flowStatus, hisStatus));
    }

    @Override
    public Instance rejectAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable
        , String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.REJECT.getKey(), message, variable, flowStatus, hisStatus));
    }


    @Override
    public Instance skip(Long taskId, FlowParams flowParams) {
        // 获取待办任务
        Task task = getById(taskId);
        return skip(flowParams, task);
    }

    @Override
    public Instance skipByInsId(Long instanceId, FlowParams flowParams) {
        return skip(flowParams, getTask(instanceId));
    }

    @Override
    public Instance rejectLastByInsId(Long instanceId, FlowParams flowParams) {
        return rejectLast(getTask(instanceId), flowParams);
    }

    @Override
    public Instance rejectLast(Long taskId, FlowParams flowParams) {
        return rejectLast(getById(taskId), flowParams);
    }

    @Override
    public Instance rejectLast(Task task, FlowParams flowParams) {
        flowParams.skipType(SkipType.REJECT.getKey());
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        // 获取当前任务的前置任务
        List<HisTask> hisTaskList = FlowEngine.hisTaskService().getByInsId(task.getInstanceId());
        // 获取hisTaskList中TargetNodeCod等于task.getNodeCode()的，并且id最大的
        HisTask lastHisTask = hisTaskList.stream()
            .filter(hisTask -> StringUtils.isNotEmpty(hisTask.getTargetNodeCode()))
            .filter(hisTask -> SkipType.isPass(hisTask.getSkipType()))
            .filter(hisTask -> {
                String targetCode = hisTask.getTargetNodeCode();
                if (targetCode.contains(",")) {
                    return Arrays.asList(targetCode.split(",")).contains(task.getNodeCode());
                } else {
                    return targetCode.equals(task.getNodeCode());
                }
            })
            .max(Comparator.comparingLong(HisTask::getId))
            .orElse(null);

        AssertUtil.isNull(lastHisTask, ExceptionCons.NOT_FOUNT_LAST_TASK);
        flowParams.nodeCode(lastHisTask.getNodeCode());
        return skip(flowParams, task);
    }

    @Override
    public Instance taskBackByInsId(Long instanceId, FlowParams flowParams) {
        // 获取当前任务的前置任务
        HisTask lastHisTask = taskBack(flowParams, instanceId);
        List<Node> suffixNodeList = FlowEngine.nodeService().suffixNodeList(lastHisTask.getDefinitionId()
            , lastHisTask.getNodeCode());

        List<String> suffixNodeCodes = StreamUtils.toList(suffixNodeList, Node::getNodeCode);
        List<Task> taskList = FlowEngine.taskService().getByInsIdAndNodeCodes(instanceId, suffixNodeCodes);
        AssertUtil.isEmpty(taskList, ExceptionCons.NOT_FOUNT_HANDLED_TASK_HANDLER);
        return skip(flowParams, taskList.get(0));
    }

    @Override
    public Instance taskBack(Long taskId, FlowParams flowParams) {
        Task task = getById(taskId);
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        taskBack(flowParams, task.getInstanceId());
        return skip(flowParams, task);
    }

    @Override
    public Instance skip(FlowParams flowParams, Task task) {
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        return FlowEngine.transactionExecutor().execute(() -> {
            lockInstance(task.getInstanceId());
            // 等待实例锁期间任务可能已被其他请求消费，不使用调用方的旧对象。
            return doSkip(flowParams, getById(task.getId()), OPERATION_SOURCE.get() == null ? "USER" : OPERATION_SOURCE.get());
        });
    }

    private Instance doSkip(FlowParams flowParams, Task task, String source) {
        // 流程开启前正确性校验
        R r = getAndCheck(task);
        if (ProcessLifecycleState.AWAITING_RESUBMISSION.name().equals(r.instance.getLifecycleState())) {
            throw new IllegalStateException("Instance requires explicit initiator resubmission");
        }
        // 非第一个记得跳转类型必传
        if (!NodeType.isStart(task.getNodeType())) {
            AssertUtil.isFalse(StringUtils.isNotEmpty(flowParams.getSkipType()), ExceptionCons.NULL_CONDITION_VALUE);
        }
        task.setUserList(FlowEngine.userService().listByTaskIdAndTypes(task.getId()));
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(r.definition.getId());
        NodeControlConfig control = NodeControlConfigUtil.read(r.nowNode);
        if (SkipType.isReject(flowParams.getSkipType()) && control != null && !control.isAllowRollback()) {
            throw new IllegalStateException("Rollback is disabled on this node");
        }

        checkAuth(task, flowParams);

        LifecycleTransition transition = new LifecycleTransition(r.instance, r.definition, r.nowNode, task,
            flowParams, SkipType.isReject(flowParams.getSkipType()) ? "REJECT" : "APPROVE", source);

        // 如果是受托人在处理任务，需要处理一条委派记录，并且更新委托人，回到计划审批人,然后直接返回流程实例
        if (!flowParams.isIgnoreDepute() && handleDepute(task, flowParams)) {
            FlowEngine.instanceService().updateById(r.instance);
            transition.finish(true, true, "COMPLETED", null, null);
            return r.instance;
        }

        // 判断当前处理人是否有权限处理
        checkAuth(task, flowParams);

        //或签、会签、票签逻辑处理
        if (!flowParams.isIgnoreCooperate() && cooperate(r.nowNode, task, flowParams)) {
            FlowEngine.instanceService().updateById(r.instance);
            transition.finish(true, false, "COMPLETED", null, null);
            return r.instance;
        }

        if (SkipType.isReject(flowParams.getSkipType()) && NodeControlConfigUtil.toInitiator(control)) {
            return returnToInitiator(r, control, flowParams, transition, false);
        }

        if (NodeType.isSubProcess(task.getNodeType())) {
            FlowEngine.subprocessService().beforeTaskLeave(task, flowParams.getSkipType());
        }

        // 获取后续任务节点结合
        PathWayData pathWayData = new PathWayData().setInsId(task.getInstanceId()).setSkipType(flowParams.getSkipType());
        Node nextNode = FlowEngine.nodeService().getNextNode(r.nowNode, flowParams.getNodeCode()
            , flowParams.getSkipType(), pathWayData, flowCombine);
        List<Node> nextNodes = FlowEngine.nodeService().getNextByCheckGateway(flowParams.getVariables()
            , nextNode, pathWayData, flowCombine);

        // 判断并行网关和包容网关节点只剩一个前置代办任务，才能生成新的代办任务
        isGenerateNewTask(pathWayData, r.instance, nextNodes);
        pathWayData.getTargetNodes().addAll(nextNodes);

        // 设置流程图元数据
        r.instance.setDefJson(FlowEngine.chartService().skipMetadata(pathWayData));

        // 构建增待办任务和设置结束任务历史记录
        List<Task> addTasks = StreamUtils.toList(nextNodes, node -> addTask(node, r.instance, r.definition, flowParams));

        // 办理人变量替换
        ExpressionUtil.evalVariable(addTasks, flowParams.variables(MapUtil.mergeAll(r.instance.getVariableMap(), flowParams.getVariables())));

        transition.prepare(addTasks, nextNodes);

        // 更新流程信息
        updateFlowInfo(task, r.instance, addTasks, flowParams, nextNodes);

        // 一票否决（谨慎使用），如果退回，退回指向节点后还存在其他正在执行的待办任务，转历史任务，状态都为失效,重走流程。
        if (CollUtil.isNotEmpty(nextNodes) && SkipType.isReject(flowParams.getSkipType())) {
            oneVoteVeto(task, nextNodes.get(0).getNodeCode(), flowCombine);
        }

        // 处理未完成的任务，当流程完成，还存在待办任务未完成，转历史任务，状态完成。
        handUndoneTask(r.instance);

        transition.finish(true, false, SkipType.isReject(flowParams.getSkipType()) ? "REJECTED" : "COMPLETED", null, null);

        if (containsSubprocessTask(addTasks)) {
            FlowEngine.subprocessService().onTasksCreated(addTasks);
        }
        CarbonCopyUtil.advanceTasks(addTasks, flowParams.getVariables());
        Instance advanced = ApproverPolicyUtil.advanceTasks(addTasks, flowParams.getVariables());
        if (NodeType.isEnd(r.instance.getNodeType()) && isSubprocessChild(r.instance)) {
            FlowEngine.subprocessService().onInstanceTerminal(r.instance, SubprocessOutcome.SUCCEEDED);
        }

        return advanced == null ? r.instance : advanced;
    }

    @Override
    public Instance skipSystemTask(FlowParams flowParams, Task task) {
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        flowParams.ignore(true).ignoreDepute(true).ignoreCooperate(true);
        String previousSource = OPERATION_SOURCE.get();
        OPERATION_SOURCE.set("SYSTEM");
        try { return skip(flowParams, task); }
        finally {
            if (previousSource == null) OPERATION_SOURCE.remove();
            else OPERATION_SOURCE.set(previousSource);
        }
    }

    private Instance lockInstance(Long instanceId) {
        LifecycleCallbackGuard.check(instanceId);
        if (!FlowEngine.transactionExecutor().isTransactionActive()) {
            throw new IllegalStateException("Workflow transition requires an active transaction");
        }
        FlowInstanceDao<Instance> dao = FlowEngine.instanceService().getDao();
        if (dao == null) throw new IllegalStateException("Instance DAO is unavailable");
        // 通过现有查询应用宿主 ORM 的租户规则；锁查询必须刷新缓存并返回最新状态。
        Instance visible = FlowEngine.instanceService().getById(instanceId);
        AssertUtil.isNull(visible, ExceptionCons.NOT_FOUNT_INSTANCE);
        Instance instance = dao.lockForUpdate(visible.getTenantId(), instanceId);
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        return instance;
    }

    private Instance returnToInitiator(R r, NodeControlConfig control, FlowParams params,
                                       LifecycleTransition transition, boolean withdrawn) {
        AssertUtil.isEmpty(r.instance.getCreatedBy(), "Instance initiator is missing");
        if (StringUtils.isNotEmpty(params.getNodeCode())) {
            throw new IllegalArgumentException("TO_INITIATOR does not accept an explicit target node");
        }
        if (definitionHasSubprocess(r.definition.getId())) {
            FlowEngine.subprocessService().cancelByParent(r.instance.getId(), "RETURNED_TO_INITIATOR");
        }
        Node initiator = initiatorNode(r.instance);
        Task next = FlowEngine.newTask().setInstanceId(r.instance.getId()).setDefinitionId(r.definition.getId())
            .setNodeCode(initiator.getNodeCode()).setNodeName(initiator.getNodeName()).setNodeType(initiator.getNodeType())
            .setFormId(r.definition.getFormId()).setFlowStatus(FlowStatus.TOBESUBMIT.getKey())
            .setPermissionList(Collections.singletonList(r.instance.getCreatedBy())).setCreatedAt(new Date());
        next.setTenantId(r.instance.getTenantId());
        FlowEngine.dataFillHandler().idFill(next);
        ResubmissionContext context = new ResubmissionContext();
        context.setDefinitionId(r.definition.getId());
        context.setSourceTaskId(r.task.getId());
        context.setSourceNodeCode(r.task.getNodeCode());
        context.setInitiatorTaskId(next.getId());
        context.setStrategy(control.getResubmitStrategy());
        r.instance.setResubmissionContext(FlowEngine.jsonConvert.objToStr(context));
        r.instance.setLifecycleState(ProcessLifecycleState.AWAITING_RESUBMISSION.name());
        updateResubmissionChart(r.instance, Collections.emptyList(), null, false);
        // 退回发起人结束本次所有在途待办，不能留下可继续审批的并行分支。
        List<Task> active = getByInsId(r.instance.getId());
        for (Task current : active) {
            FlowParams historyParams = current.getId().equals(r.task.getId()) ? params : FlowParams.build()
                .skipType(SkipType.NONE.getKey()).handler(params.getHandler()).message("退回发起人，取消剩余待办");
            FlowEngine.hisTaskService().save(FlowEngine.hisTaskService()
                .setSkipInsHis(current, Collections.singletonList(initiator), historyParams));
        }
        removeAndUser(active);
        List<Task> nextTasks = new ArrayList<Task>(Collections.singletonList(next));
        transition.prepare(nextTasks, Collections.singletonList(initiator));
        setInsFinishInfo(r.instance, nextTasks, params);
        saveBatch(nextTasks);
        FlowEngine.userService().saveBatch(FlowEngine.userService().taskAddUsers(nextTasks));
        FlowEngine.instanceService().updateById(r.instance);
        transition.finish(!withdrawn, false, withdrawn ? "WITHDRAWN" : "REJECTED", null,
            withdrawn ? LifecycleEventType.PROCESS_WITHDRAWN : null);
        return r.instance;
    }

    private Node initiatorNode(Instance instance) {
        return FlowEngine.newNode().setDefinitionId(instance.getDefinitionId())
            .setNodeCode(NodeControlConfigUtil.INITIATOR_CODE).setNodeName("发起人")
            .setNodeType(NodeType.INITIATOR.getKey());
    }

    @Override
    public Instance resubmit(Long instanceId, FlowParams params) {
        Objects.requireNonNull(params, "flowParams");
        return FlowEngine.transactionExecutor().execute(() -> doResubmit(lockInstance(instanceId), params));
    }

    private Instance doResubmit(Instance instance, FlowParams params) {
        if (!ProcessLifecycleState.AWAITING_RESUBMISSION.name().equals(instance.getLifecycleState())) {
            throw new IllegalStateException("Instance is not awaiting resubmission");
        }
        AssertUtil.isEmpty(instance.getCreatedBy(), "Instance initiator is missing");
        if (!instance.getCreatedBy().equals(params.getHandler())) {
            throw new IllegalStateException("Only the instance initiator may resubmit");
        }
        if (StringUtils.isNotEmpty(params.getNodeCode())) {
            throw new IllegalArgumentException("Resubmission target is determined by the captured strategy");
        }
        Definition definition = FlowEngine.defService().getById(instance.getDefinitionId());
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);
        AssertUtil.isFalse(judgeActivityStatus(definition, instance), ExceptionCons.NOT_ACTIVITY);
        ResubmissionContext context = FlowEngine.jsonConvert.strToBean(instance.getResubmissionContext(), ResubmissionContext.class);
        if (context == null || context.getSchemaVersion() != 1
                || !Objects.equals(context.getDefinitionId(), instance.getDefinitionId())
                || context.getSourceTaskId() == null || StringUtils.isEmpty(context.getSourceNodeCode())) {
            throw new IllegalStateException("Invalid persisted resubmission context");
        }
        NodeControlConfigUtil.validateStrategy(context.getStrategy());
        Task task = getTask(instance.getId());
        if (!NodeType.INITIATOR.getKey().equals(task.getNodeType())
                || !Objects.equals(context.getInitiatorTaskId(), task.getId())) {
            throw new IllegalStateException("Initiator task does not match resubmission context");
        }
        LifecycleTransition transition = new LifecycleTransition(instance, definition, initiatorNode(instance), task,
            params, "RESUBMIT", "USER");
        params.skipType(SkipType.PASS.getKey());
        DefJson snapshot = FlowEngine.jsonConvert.strToBean(instance.getDefJson(), DefJson.class);
        if (snapshot == null || CollUtil.isEmpty(snapshot.getNodeList())) {
            throw new IllegalStateException("Instance definition snapshot is unavailable");
        }
        FlowCombine graph = DefJson.copyCombine(snapshot);
        graph.getAllNodes().forEach(node -> node.setDefinitionId(instance.getDefinitionId()));
        PathWayData route = new PathWayData().setInsId(instance.getId()).setSkipType(SkipType.PASS.getKey());
        List<Node> nextNodes;
        if (NodeControlConfigUtil.CONTINUE.equals(context.getStrategy())) {
            Node source = StreamUtils.filterOne(graph.getAllNodes(), node -> context.getSourceNodeCode().equals(node.getNodeCode()));
            if (source == null || !NodeType.isBetween(source.getNodeType())) {
                throw new IllegalStateException("Captured rejection node is unavailable");
            }
            nextNodes = Collections.singletonList(source);
        } else {
            Node start = StreamUtils.filterOne(graph.getAllNodes(), node -> NodeType.isStart(node.getNodeType()));
            AssertUtil.isNull(start, ExceptionCons.LOST_START_NODE);
            // 仅使用开始节点的出边选路，不重新激活开始事件。
            nextNodes = FlowEngine.nodeService().getNextNodeList(start, null, SkipType.PASS.getKey(),
                params.getVariables(), route, graph);
        }
        AssertUtil.isEmpty(nextNodes, ExceptionCons.NULL_DEST_NODE);
        List<Task> nextTasks = StreamUtils.toList(nextNodes, node -> addTask(node, instance, definition, params));
        ExpressionUtil.evalVariable(nextTasks, params);
        transition.prepare(nextTasks, nextNodes);
        updateResubmissionChart(instance, nextNodes, route, NodeControlConfigUtil.RESTART.equals(context.getStrategy()));
        instance.setLifecycleState(ProcessLifecycleState.ACTIVE.name()).setResubmissionContext("{}");
        updateFlowInfo(task, instance, nextTasks, params, nextNodes);
        transition.finish(false, false, "COMPLETED", null, LifecycleEventType.PROCESS_RESUBMITTED);
        if (containsSubprocessTask(nextTasks)) FlowEngine.subprocessService().onTasksCreated(nextTasks);
        CarbonCopyUtil.advanceTasks(nextTasks, params.getVariables());
        Instance advanced = ApproverPolicyUtil.advanceTasks(nextTasks, params.getVariables());
        return advanced == null ? instance : advanced;
    }

    private void updateResubmissionChart(Instance instance, List<Node> targets, PathWayData route, boolean restart) {
        DefJson snapshot = FlowEngine.jsonConvert.strToBean(instance.getDefJson(), DefJson.class);
        if (snapshot == null || CollUtil.isEmpty(snapshot.getNodeList())) {
            throw new IllegalStateException("Instance definition snapshot is unavailable");
        }
        Map<String, NodeJson> nodes = StreamUtils.toMap(snapshot.getNodeList(), NodeJson::getNodeCode, node -> node);
        for (NodeJson node : snapshot.getNodeList()) {
            if (ChartStatus.isToDo(node.getStatus()) || (restart && !NodeType.isStart(node.getNodeType()))) {
                node.setStatus(ChartStatus.NOT_DONE.getKey());
            }
            if (restart && node.getSkipList() != null) {
                node.getSkipList().forEach(skip -> skip.setStatus(ChartStatus.NOT_DONE.getKey()));
            }
        }
        if (route != null) {
            for (Node visited : route.getPathWayNodes()) {
                nodes.get(visited.getNodeCode()).setStatus(ChartStatus.DONE.getKey());
            }
            for (Skip visited : route.getPathWaySkips()) {
                NodeJson source = nodes.get(visited.getSourceNodeCode());
                for (SkipJson skip : source.getSkipList()) {
                    if (Objects.equals(skip.getTargetNodeCode(), visited.getTargetNodeCode())
                            && Objects.equals(skip.getSkipType(), visited.getSkipType())
                            && Objects.equals(skip.getSkipCondition(), visited.getSkipCondition())) {
                        skip.setStatus(ChartStatus.DONE.getKey());
                    }
                }
            }
        }
        for (Node target : targets) {
            nodes.get(target.getNodeCode()).setStatus(NodeType.isEnd(target.getNodeType())
                ? ChartStatus.DONE.getKey() : ChartStatus.TO_DO.getKey());
        }
        instance.setDefJson(FlowEngine.jsonConvert.objToStr(snapshot));
    }

    @Override
    public Instance revoke(Long instanceId, FlowParams flowParams) {
        return FlowEngine.transactionExecutor().execute(() -> {
            Instance instance = lockInstance(instanceId);
            requireNotAwaitingResubmission(instance);
            return doRevoke(instanceId, flowParams);
        });
    }

    private Instance doRevoke(Long instanceId, FlowParams flowParams) {
        Instance instance = FlowEngine.instanceService().getById(instanceId);
        List<Task> tasks = getByInsId(instanceId);
        AssertUtil.isEmpty(tasks, ExceptionCons.NOT_FOUND_FLOW_TASK);
        R r = getAndCheck(tasks.get(0));
        if (!flowParams.isIgnore()) {
            AssertUtil.isFalse(Objects.equals(instance.getCreatedBy(), flowParams.getHandler()), ExceptionCons.NOT_DEF_PROMOTER_NOT_CANCEL);
        }
        flowParams.skipType(SkipType.REJECT.getKey());
        LifecycleTransition transition = new LifecycleTransition(r.instance, r.definition, null, r.task,
            flowParams, "WITHDRAW", "USER");
        NodeControlConfig control = new NodeControlConfig();
        control.setResubmitStrategy(NodeControlConfigUtil.RESTART);
        return returnToInitiator(r, control, flowParams, transition, true);
    }

    @Override
    public Instance terminationByInsId(Long instanceId, FlowParams flowParams) {
        AssertUtil.isNull(instanceId, ExceptionCons.NULL_INSTANCE_ID);
        // 获取待办任务
        List<Task> taskList = FlowEngine.taskService().getByInsId(instanceId);
        AssertUtil.isEmpty(taskList, ExceptionCons.NOT_FOUNT_TASK);
        Task task = taskList.get(0);
        return termination(task, flowParams);
    }

    @Override
    public Instance termination(Long taskId, FlowParams flowParams) {
        return termination(getById(taskId), flowParams);
    }

    @Override
    public Instance termination(Task task, FlowParams flowParams) {
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        return FlowEngine.transactionExecutor().execute(() -> {
            lockInstance(task.getInstanceId());
            return doTermination(getById(task.getId()), flowParams);
        });
    }

    private Instance doTermination(Task task, FlowParams flowParams) {
        R r = getAndCheck(task);
        flowParams.skipType(SkipType.PASS.getKey());

        // 判断当前处理人是否有权限处理
        task.setUserList(FlowEngine.userService().listByTaskIdAndTypes(task.getId()));
        checkAuth(task, flowParams);
        LifecycleTransition transition = new LifecycleTransition(r.instance, r.definition, r.nowNode, task,
            flowParams, "TERMINATE", "USER");

        if (definitionHasSubprocess(r.definition.getId())) {
            FlowEngine.subprocessService().cancelByParent(r.instance.getId(), "PARENT_TERMINATED");
        }

        // 所有待办转历史
        Node endNode = FlowEngine.nodeService().getEndNode(r.instance.getDefinitionId());

        // 设置流程图元数据
        PathWayData pathWayData = new PathWayData()
            .setInsId(task.getInstanceId())
            .setSkipType(flowParams.getSkipType())
            .setPathWayNodes(Collections.singletonList(r.nowNode))
            .setTargetNodes(Collections.singletonList(endNode));
        if (NodeType.INITIATOR.getKey().equals(task.getNodeType())) {
            updateResubmissionChart(r.instance, Collections.singletonList(endNode), null, false);
        } else {
            r.instance.setDefJson(FlowEngine.chartService().skipMetadata(pathWayData));
        }

        // 流程实例完成
        r.instance.setNodeType(endNode.getNodeType())
            .setNodeCode(endNode.getNodeCode())
            .setNodeName(endNode.getNodeName())
            .setFlowStatus(StringUtils.emptyDefault(flowParams.getFlowStatus(), FlowStatus.TERMINATE.getKey()))
            .setLifecycleState(ProcessLifecycleState.ENDED.name()).setResubmissionContext("{}");

        // 待办任务转历史
        flowParams.flowStatus(r.instance.getFlowStatus());
        HisTask insHis = FlowEngine.hisTaskService().setSkipInsHis(task, Collections.singletonList(endNode)
            , flowParams);
        FlowEngine.hisTaskService().save(insHis);
        FlowEngine.instanceService().updateById(r.instance);

        // 删除流程相关办理人
        FlowEngine.userService().deleteByTaskIds(Collections.singletonList(task.getId()));

        // 处理未完成的任务，当流程完成，还存在待办任务未完成，转历史任务，状态完成。
        handUndoneTask(r.instance);
        transition.finish(false, false, "CANCELLED", "TERMINATED", null);
        if (isSubprocessChild(r.instance)) {
            FlowEngine.subprocessService().onInstanceTerminal(r.instance, SubprocessOutcome.CANCELLED);
        }
        return r.instance;
    }

    @Override
    public boolean deleteByInsIds(List<Long> instanceIds) {
        List<Instance> instanceList = FlowEngine.instanceService().getByIds(instanceIds);
        Definition definition;
        for (Instance instance : instanceList) {
            definition = FlowEngine.defService().getById(instance.getDefinitionId());
            AssertUtil.isFalse(judgeActivityStatus(definition, instance), ExceptionCons.NOT_ACTIVITY);
        }
        return SqlHelper.retBool(getDao().deleteByInsIds(instanceIds));
    }

    @Override
    public boolean transfer(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getAddHandlers(), ExceptionCons.NULL_TRANSFER_HANDLER);
        List<User> users = FlowEngine.userService().getByProcessedBys(taskId, flowParams.getAddHandlers(), UserType.TRANSFER.getKey());
        AssertUtil.isNotEmpty(users, ExceptionCons.IS_ALREADY_TRANSFER);
        flowParams.cooperationType(CooperationType.TRANSFER.getKey())
            .reductionHandlers(Collections.singletonList(flowParams.getHandler()));

        return updateHandler(taskId, flowParams);
    }

    @Override
    public boolean depute(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getAddHandlers(), ExceptionCons.NULL_DEPUTE_HANDLER);
        List<User> users = FlowEngine.userService().getByProcessedBys(taskId, flowParams.getAddHandlers(), UserType.DEPUTE.getKey());
        AssertUtil.isNotEmpty(users, ExceptionCons.IS_ALREADY_DEPUTE);
        flowParams.cooperationType(CooperationType.DEPUTE.getKey())
            .reductionHandlers(Collections.singletonList(flowParams.getHandler()));

        return updateHandler(taskId, flowParams);
    }

    @Override
    public boolean addSignature(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getAddHandlers(), ExceptionCons.NULL_ADD_SIGNATURE_HANDLER);
        List<User> users = FlowEngine.userService().getByProcessedBys(taskId, flowParams.getAddHandlers(), UserType.APPROVAL.getKey());
        AssertUtil.isNotEmpty(users, ExceptionCons.IS_ALREADY_SIGN);
        flowParams.cooperationType(CooperationType.ADD_SIGNATURE.getKey());

        return updateHandler(taskId, flowParams);
    }

    @Override
    public boolean reductionSignature(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getReductionHandlers(), ExceptionCons.NULL_REDUCTION_SIGNATURE_HANDLER);
        List<User> users = FlowEngine.userService().listByTaskIdAndTypes(taskId
            , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey());
        AssertUtil.isTrue(CollUtil.isEmpty(users) || users.size() == 1, ExceptionCons.REDUCTION_SIGN_ONE_ERROR);
        flowParams.cooperationType(CooperationType.REDUCTION_SIGNATURE.getKey());

        return updateHandler(taskId, flowParams);
    }

    @Override
    public boolean updateHandler(Long taskId, FlowParams flowParams) {
        Task task = getById(taskId);
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        return FlowEngine.transactionExecutor().execute(() -> {
            requireNotAwaitingResubmission(lockInstance(task.getInstanceId()));
            return doUpdateHandler(taskId, flowParams);
        });
    }

    private boolean doUpdateHandler(Long taskId, FlowParams flowParams) {
        // 获取待办任务
        R r = getAndCheck(taskId);

        // 获取给谁的权限
        if (!flowParams.isIgnore()) {
            // 判断当前处理人是否有权限，获取当前办理人的权限
            List<String> permissions = flowParams.getPermissionFlag();
            // 获取任务权限人
            List<String> taskPermissions = FlowEngine.userService().getPermission(taskId
                , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());
            AssertUtil.isTrue(CollUtil.isNotEmpty(taskPermissions) && (CollUtil.isEmpty(permissions)
                || CollUtil.notContainsAny(permissions, taskPermissions)), ExceptionCons.NOT_AUTHORITY);
        }
        LifecycleTransition transition = new LifecycleTransition(r.instance, r.definition, r.nowNode, r.task,
            flowParams, "ASSIGNEES_CHANGED:" + flowParams.getCooperationType(), "USER");
        // 留存历史记录
        flowParams.skipType(SkipType.NONE.getKey());
        HisTask hisTask = null;
        // 删除对应的操作人
        if (CollUtil.isNotEmpty(flowParams.getReductionHandlers())) {
            for (String reductionHandler : flowParams.getReductionHandlers()) {
                FlowEngine.userService().remove(FlowEngine.newUser().setTaskId(taskId)
                    .setProcessedBy(reductionHandler));
            }
            hisTask = FlowEngine.hisTaskService().setCooperateHis(r.task, flowParams, flowParams.getReductionHandlers());
        }

        // 新增权限人
        if (CollUtil.isNotEmpty(flowParams.getAddHandlers())) {
            String type;
            if (CooperationType.TRANSFER.getKey().equals(flowParams.getCooperationType())) {
                type = UserType.TRANSFER.getKey();
            } else if (CooperationType.DEPUTE.getKey().equals(flowParams.getCooperationType())) {
                type = UserType.DEPUTE.getKey();
            } else {
                type = UserType.APPROVAL.getKey();
            }
            FlowEngine.userService().saveBatch(StreamUtils.toList(flowParams.getAddHandlers(), permission ->
                FlowEngine.userService().structureUser(taskId, permission
                    , type, flowParams.getHandler())));
            hisTask = FlowEngine.hisTaskService().setCooperateHis(r.task, flowParams, flowParams.getAddHandlers());
        }
        if (ObjectUtil.isNotNull(hisTask)) {
            FlowEngine.hisTaskService().save(hisTask);
        }
        FlowEngine.instanceService().updateById(r.instance);
        transition.finish(false, true, "COMPLETED", null, null);
        return true;
    }

    @Override
    public Instance pendingByInsId(Long instanceId, FlowParams flowParams) {
        return pending(getTask(instanceId), flowParams);
    }

    @Override
    public Instance pending(Long taskId, FlowParams flowParams) {
        // 获取待办任务
        Task task = getById(taskId);
        return pending(task, flowParams);
    }

    @Override
    public Instance pending(Task task, FlowParams flowParams) {
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        return FlowEngine.transactionExecutor().execute(() -> {
            requireNotAwaitingResubmission(lockInstance(task.getInstanceId()));
            return doPending(getById(task.getId()), flowParams);
        });
    }

    private void requireNotAwaitingResubmission(Instance instance) {
        if (ProcessLifecycleState.AWAITING_RESUBMISSION.name().equals(instance.getLifecycleState())) {
            throw new IllegalStateException("Instance requires explicit initiator resubmission");
        }
    }

    private Instance doPending(Task task, FlowParams flowParams) {
        // 流程开启前正确性校验
        R r = getAndCheck(task);
        flowParams.flowStatus(StringUtils.emptyDefault(flowParams.getFlowStatus(), FlowStatus.PENDING.getKey()));

        // 判断当前处理人是否有权限处理
        r.task.setUserList(FlowEngine.userService().listByTaskIdAndTypes(r.task.getId()));
        checkAuth(r.task, flowParams);
        new LifecycleTransition(r.instance, r.definition, r.nowNode, r.task, flowParams, "PENDING", "USER");

        // 设置流程历史任务信息
        HisTask insHis = FlowEngine.hisTaskService().notSkip(r.task, flowParams);
        FlowEngine.hisTaskService().save(insHis);

        FlowEngine.instanceService().updateById(r.instance.setFlowStatus(flowParams.getFlowStatus()));


        return r.instance;
    }

    @Override
    public Task addTask(Node node, Instance instance, Definition definition, FlowParams flowParams) {
        // 人员解析与预览使用相同的实例变量视图，本次提交值覆盖已保存变量。
        flowParams.variables(MapUtil.mergeAll(instance.getVariableMap(), flowParams.getVariables()));
        Task addTask = FlowEngine.newTask();
        Date now = new Date();
        FlowEngine.dataFillHandler().idFill(addTask);
        addTask.setTenantId(instance.getTenantId());
        addTask.setDefinitionId(instance.getDefinitionId())
            .setInstanceId(instance.getId())
            .setNodeCode(node.getNodeCode())
            .setNodeName(node.getNodeName())
            .setNodeType(node.getNodeType())
            .setFlowStatus(StringUtils.emptyDefault(flowParams.getFlowStatus(),
                setFlowStatus(node.getNodeType(), flowParams.getSkipType())))
            .setCreatedAt(now)
            .setPermissionList(ApproverRuleUtil.resolve(node, instance, flowParams, false));

        TimeoutConfigUtil.applySnapshot(node, addTask, now, flowParams.getVariables());

        // 所有节点使用流程表单；任务保存该引用的快照。
        addTask.setFormId(definition.getFormId());

        return addTask;
    }

    @Override
    public List<Task> getByInsId(Long instanceId) {
        return list(FlowEngine.newTask().setInstanceId(instanceId));
    }

    @Override
    public List<Task> getByInsIdAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        return getDao().getByInsIdAndNodeCodes(instanceId, nodeCodes);
    }

    @Override
    public List<Task> getByInsIdAndNodeType(Long instanceId, Integer nodeType) {
        return getDao().getByInsIdAndNodeType(instanceId, nodeType);
    }

    @Override
    public List<Task> listByBusinessKey(String businessType, String businessId) {
        List<Instance> instances = FlowEngine.instanceService().listByBusinessKey(businessType, businessId);
        if (CollUtil.isEmpty(instances)) {
            return Collections.emptyList();
        }
        List<Long> instanceIds = StreamUtils.toList(instances, Instance::getId);
        return new ArrayList<Task>(getDao().listByInsIds(instanceIds));
    }

    @Override
    public List<Task> listDueTimeoutTasks(Date dueBefore, Date staleBefore, int limit) {
        return new ArrayList<Task>(getDao().listDueTimeoutTasks(dueBefore, staleBefore, limit));
    }

    @Override
    public boolean claimTimeout(Long taskId, Date claimedAt, Date staleBefore) {
        Task task = getById(taskId);
        if (task == null) return false;
        lockInstance(task.getInstanceId());
        return getDao().claimTimeout(taskId, claimedAt, staleBefore) == 1;
    }

    @Override
    public boolean claimWait(Long taskId, Date claimedAt) {
        Task task = getById(taskId);
        if (task == null) return false;
        lockInstance(task.getInstanceId());
        return getDao().claimWait(taskId, claimedAt) == 1;
    }

    @Override
    public boolean releaseTimeout(Long taskId) {
        return getDao().releaseTimeout(taskId) == 1;
    }

    @Override
    public void setInsFinishInfo(Instance instance, List<Task> addTasks, FlowParams flowParams) {
        instance.setUpdatedAt(new Date());
        // 合并流程变量到实例对象
        mergeVariable(instance, flowParams.getVariables());
        if (CollUtil.isNotEmpty(addTasks)) {
            AtomicReference<Task> finallyTask = new AtomicReference<>();
            addTasks.removeIf(addTask -> {
                if (NodeType.isEnd(addTask.getNodeType())) {
                    finallyTask.set(addTask);
                    return true;
                }
                return false;
            });
            if (ObjectUtil.isNull(finallyTask.get())) {
                finallyTask.set(getNextTask(addTasks));
            }
            instance.setNodeType(finallyTask.get().getNodeType())
                .setNodeCode(finallyTask.get().getNodeCode())
                .setNodeName(finallyTask.get().getNodeName())
                .setFlowStatus(finallyTask.get().getFlowStatus());
        }
        if (NodeType.isEnd(instance.getNodeType())) instance.setLifecycleState(ProcessLifecycleState.ENDED.name());
        else if (!ProcessLifecycleState.AWAITING_RESUBMISSION.name().equals(instance.getLifecycleState())) {
            instance.setLifecycleState(ProcessLifecycleState.ACTIVE.name());
        }
    }

    @Override
    public void mergeVariable(Instance instance, Map<String, Object> variable) {
        if (MapUtil.isNotEmpty(variable)) {
            String variableStr = instance.getVariables();
            Map<String, Object> deserialize = FlowEngine.jsonConvert.strToMap(variableStr);
            deserialize.putAll(variable);
            instance.setVariables(FlowEngine.jsonConvert.objToStr(deserialize));
        }
    }

    /**
     * 根据流程实例id获取操作人最近的已办历史任务
     *
     * @param flowParams 包含流程相关参数的对象
     * @param instanceId 流程实例id
     * @return 最近的已办历史任务
     */
    private HisTask taskBack(FlowParams flowParams, Long instanceId) {
        flowParams.skipType(SkipType.REJECT.getKey())
            .ignore(true)
            .ignoreDepute(true)
            .ignoreCooperate(true)
            .flowStatus(StringUtils.emptyDefault(flowParams.getFlowStatus(), FlowStatus.TASK_BACK.getKey()));
        // 获取当前任务的前置任务
        List<HisTask> hisTaskList = FlowEngine.hisTaskService().getByInsId(instanceId);
        // 获取hisTaskList中TargetNodeCod等于task.getNodeCode()的，并且id最大的
        HisTask lastHisTask = hisTaskList.stream()
            .filter(hisTask -> StringUtils.isNotEmpty(hisTask.getApprover()))
            .filter(hisTask -> SkipType.isPass(hisTask.getSkipType()))
            .filter(hisTask -> hisTask.getApprover().equals(flowParams.getHandler()))
            .max(Comparator.comparingLong(HisTask::getId))
            .orElse(null);
        AssertUtil.isNull(lastHisTask, ExceptionCons.NOT_FOUNT_HANDLED_TASK);
        flowParams.nodeCode(lastHisTask.getNodeCode());
        return lastHisTask;
    }

    /**
     * 获取待办任务
     *
     * @param instanceId 实例id
     * @return 待办任务
     */
    private Task getTask(Long instanceId) {
        List<Task> taskList = getByInsId(instanceId);
        AssertUtil.isEmpty(taskList, ExceptionCons.NOT_FOUNT_TASK);
        AssertUtil.isTrue(taskList.size() > 1, ExceptionCons.TASK_NOT_ONE);
        return taskList.get(0);
    }

    private String setFlowStatus(Integer nodeType, String skipType) {
        // 根据审批动作确定流程状态
        if (NodeType.isStart(nodeType)) {
            return FlowStatus.TOBESUBMIT.getKey();
        } else if (NodeType.isEnd(nodeType)) {
            return FlowStatus.FINISHED.getKey();
        } else if (SkipType.isReject(skipType)) {
            return FlowStatus.REJECT.getKey();
        } else {
            return FlowStatus.APPROVAL.getKey();
        }
    }

    private Task getNextTask(List<Task> tasks) {
        if (tasks.size() == 1) {
            return tasks.get(0);
        }
        for (Task task : tasks) {
            if (NodeType.isEnd(task.getNodeType())) {
                return task;
            }
        }
        return tasks.stream().max(Comparator.comparingLong(Task::getId)).orElse(null);
    }

    private void removeAndUser(List<Task> taskList) {
        removeByIds(StreamUtils.toList(taskList, Task::getId));
        FlowEngine.userService().deleteByTaskIds(StreamUtils.toList(taskList, Task::getId));
    }

    private R getAndCheck(Long taskId) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        return getAndCheck(getById(taskId));
    }

    private R getAndCheck(Task task) {
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        Instance instance = FlowEngine.instanceService().getById(task.getInstanceId());
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        Definition definition = FlowEngine.defService().getById(instance.getDefinitionId());
        AssertUtil.isFalse(judgeActivityStatus(definition, instance), ExceptionCons.NOT_ACTIVITY);
        AssertUtil.isTrue(NodeType.isEnd(instance.getNodeType()), ExceptionCons.FLOW_FINISH);
        Node nowNode = FlowEngine.nodeService().getByDefIdAndNodeCode(task.getDefinitionId(), task.getNodeCode());
        if (NodeType.INITIATOR.getKey().equals(task.getNodeType())) nowNode = initiatorNode(instance);
        AssertUtil.isNull(nowNode, ExceptionCons.LOST_CUR_NODE);
        return new R(instance, definition, nowNode, task);
    }

    private static class R {
        public final Instance instance;
        public final Definition definition;
        public final Node nowNode;
        public final Task task;

        public R(Instance instance, Definition definition, Node nowNode, Task task) {
            this.instance = instance;
            this.definition = definition;
            this.nowNode = nowNode;
            this.task = task;
        }
    }

    private boolean handleDepute(Task task, FlowParams flowParams) {
        // 获取受托人
        List<User> entrustedUserList = StreamUtils.filter(task.getUserList(),
            user -> UserType.DEPUTE.getKey().equals(user.getType())
                && Objects.equals(flowParams.getHandler(), user.getProcessedBy()));
        if (CollUtil.isEmpty(entrustedUserList)) {
            return false;
        }

        // 记录受托人处理任务记录
        User entrustedUser = entrustedUserList.get(0);
        HisTask hisTask = FlowEngine.hisTaskService().setDeputeHisTask(task, flowParams, entrustedUser);
        FlowEngine.hisTaskService().save(hisTask);
        FlowEngine.userService().removeById(entrustedUser.getId());

        // 查询委托人，如果在flow_user不存在，则给委托人新增待办记录
        User deputeUser = FlowEngine.userService().getOne(FlowEngine.newUser().setTaskId(task.getId())
            .setProcessedBy(entrustedUser.getCreatedBy()).setType(UserType.APPROVAL.getKey()));
        if (ObjectUtil.isNull(deputeUser)) {
            User newUser = FlowEngine.userService().structureUser(entrustedUser.getTaskId()
                , entrustedUser.getCreatedBy()
                , UserType.APPROVAL.getKey(), entrustedUser.getProcessedBy());
            FlowEngine.userService().save(newUser);
        }

        return true;
    }

    /**
     * 会签，票签，协作处理，返回true；或签或者会签、票签结束返回false
     *
     * @param nowNode    当前节点
     * @param task       任务
     * @param flowParams 流程参数
     * @return boolean
     */
    private boolean cooperate(Node nowNode, Task task, FlowParams flowParams) {
        if (flowParams.isIgnore()) {
            return false;
        }
        String nodeRatio = nowNode.getNodeRatio();
        // 或签，直接返回
        if (CooperationType.isOrSign(nodeRatio)) {
            return false;
        }

        // 办理人和转办人列表
        List<User> todoList = FlowEngine.userService().listByTaskIdAndTypes(task.getId()
            , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());

        // 判断办理人是否有办理权限
        AssertUtil.isEmpty(flowParams.getHandler(), ExceptionCons.SIGN_NULL_HANDLER);
        User todoUser = CollUtil.getOne(StreamUtils.filter(todoList, u -> Objects.equals(u.getProcessedBy(), flowParams.getHandler())));
        AssertUtil.isNull(todoUser, ExceptionCons.NOT_AUTHORITY);

        // 除当前办理人外剩余办理人列表
        List<User> restList = StreamUtils.filter(todoList, u -> !Objects.equals(u.getProcessedBy(), flowParams.getHandler()));

        // 会签并且当前人退回直接返回
        if (CooperationType.isCountersign(nodeRatio) && SkipType.isReject(flowParams.getSkipType())) {
            return removeRestList(restList);
        }

        // 查询会签票签已办列表
        List<HisTask> doneList = FlowEngine.hisTaskService().listByTaskId(task.getId());
        doneList = CollUtil.emptyDefault(doneList);

        // 总人数
        int allNum = todoList.size() + doneList.size();

        // 通过历史记录
        List<HisTask> donePassList = StreamUtils.filter(doneList
            , hisTask -> Objects.equals(hisTask.getSkipType(), SkipType.PASS.getKey()));

        // 驳回历史记录
        List<HisTask> doneRejectList = StreamUtils.filter(doneList
            , hisTask -> Objects.equals(hisTask.getSkipType(), SkipType.REJECT.getKey()));

        boolean isPass = SkipType.isPass(flowParams.getSkipType());
        // 如果是票签默认或者spel表达式策略，则执行表达式
        if (CooperationType.isVoteSignDefault(nodeRatio) || CooperationType.isVoteSignRejectSpel(nodeRatio)) {
            Map<String, Object> variable = MapUtil.clone(flowParams.getVariables());
            variable.put("skipType", flowParams.getSkipType());
            variable.put("passNum", donePassList.size());
            variable.put("rejectNum", doneRejectList.size());
            variable.put("todoNum", todoList.size());
            variable.put("allNum", allNum);
            variable.put("passList", donePassList);
            variable.put("rejectList", doneRejectList);
            variable.put("todoList", todoList);
            if (ExpressionUtil.evalVoteSign(nodeRatio, variable)) {
                return removeRestList(restList);
            }
        } else {
            // 计算通过率
            BigDecimal passRatio = (isPass ? BigDecimal.ONE : BigDecimal.ZERO)
                .add(BigDecimal.valueOf(donePassList.size()))
                .divide(BigDecimal.valueOf(allNum), 4, RoundingMode.HALF_UP).multiply(MathUtil.ONE_HUNDRED);
            // 计算驳回率
            BigDecimal rejectRatio = (isPass ? BigDecimal.ZERO : BigDecimal.ONE)
                .add(BigDecimal.valueOf(doneRejectList.size()))
                .divide(BigDecimal.valueOf(allNum), 4, RoundingMode.HALF_UP).multiply(MathUtil.ONE_HUNDRED);

            // 判断是否是票签中的固定通过人数，如果是则判断是否达到该人数
            if (CooperationType.isVoteSignPassCount(nodeRatio)) {
                String passCount = StringUtils.substring(nodeRatio, nodeRatio.indexOf("=") + 1);
                if ((isPass && donePassList.size() + 1 >= Integer.parseInt(passCount))
                    || (!isPass && doneRejectList.size() + 1 > allNum - Integer.parseInt(passCount))) {
                    return removeRestList(restList);
                }
            } else if (CooperationType.isVoteSignRejectCount(nodeRatio)) {
                // 判断是否是票签中的固定驳回人数，如果是则判断是否达到该人数
                String rejectCount = StringUtils.substring(nodeRatio, nodeRatio.indexOf("=") + 1);
                if ((!isPass && doneRejectList.size() + 1 >= Integer.parseInt(rejectCount))
                || (isPass && donePassList.size() + 1 > allNum - Integer.parseInt(rejectCount))) {
                    return removeRestList(restList);
                }
            } else if ((!isPass && rejectRatio.compareTo(MathUtil.ONE_HUNDRED.subtract(new BigDecimal(nodeRatio))) > 0)
                || (isPass && passRatio.compareTo(new BigDecimal(nodeRatio)) >= 0)) {
                // 提前不满足通过率或者满足通过率，删除剩余办理人，流程正常流程流转
                return removeRestList(restList);
            }
        }

        // 当只剩一位待办用户时，由当前用户决定走向
        if (todoList.size() == 1) {
            return false;
        }

        // 添加历史任务
        HisTask hisTask = FlowEngine.hisTaskService().setSignHisTask(task, flowParams, nodeRatio, isPass);
        FlowEngine.hisTaskService().save(hisTask);

        // 删掉待办用户
        FlowEngine.userService().removeById(todoUser.getId());
        return true;
    }

    /**
     * 删除剩余办理人
     * @param restList 待办用户列表
     * @return  boolean
     */
    private static boolean removeRestList(List<User> restList) {
        if (CollUtil.isNotEmpty(restList)) {
            FlowEngine.userService().removeByIds(StreamUtils.toList(restList, User::getId));
        }
        return false;
    }

    /**
     * 判断并行网关和包容网关节点只剩一个前置代办任务，才能生成新的代办任务
     *
     * @param pathWayData 办理过程中途径数据
     * @param instance    实例
     */
    private void isGenerateNewTask(PathWayData pathWayData, Instance instance, List<Node> nextNodes) {
        if (SkipType.isReject(pathWayData.getSkipType())) {
            return;
        }

        DefJson defJson = FlowEngine.jsonConvert.strToBean(instance.getDefJson(), DefJson.class);
        Map<String, NodeJson> nodeJsonMap = StreamUtils.toMap(defJson.getNodeList(), NodeJson::getNodeCode, node -> node);
        // 遍历目标节点，获取目标节点中第一个互斥或者包含网关，并且判断只剩一个前置代办任务，才能生成新的代办任务
        List<Node> parallelOrInclusiveList = Optional.of(pathWayData)
            .map(PathWayData::getPathWayNodes)
            .orElse(Collections.emptyList())
            .stream()
            .filter(t -> NodeType.isGateWayParallel(t.getNodeType()) || NodeType.isGateWayInclusive(t.getNodeType()))
            .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(parallelOrInclusiveList)) {
            List<Node> previousNodeList = FlowEngine.nodeService().previousNodeList(instance.getDefinitionId()
                , parallelOrInclusiveList.get(parallelOrInclusiveList.size() - 1).getNodeCode());
            // 获取前置节点中代办节点的数量
            long statusOneCount = previousNodeList.stream()
                .map(Node::getNodeCode)
                .map(nodeJsonMap::get)
                .filter(Objects::nonNull)
                .filter(nodeJson -> nodeJson.getStatus() == 1)
                .count();
            // 并行网关和包容网关节点超过一个前置代办任务，说明可以不可生成新任务,
            if (statusOneCount > 1) {
                nextNodes.clear();
                AtomicBoolean flag = new AtomicBoolean(false);
                pathWayData.getPathWayNodes().removeIf(nodeJson -> {
                    if (nodeJson.getNodeCode().equals(parallelOrInclusiveList.get(0).getNodeCode())) {
                        flag.set(true);
                        return false;
                    }
                    return flag.get();
                });
                flag.set(false);
                pathWayData.getPathWaySkips().removeIf(nodeJson -> {
                    if (nodeJson.getSourceNodeCode().equals(parallelOrInclusiveList.get(0).getNodeCode())) {
                        flag.set(true);
                    }
                    return flag.get();
                });
            }
        }
    }

    /**
     * 判断当前处理人是否有权限处理
     *
     * @param task                   当前任务（任务id）
     * @param flowParams:包含流程相关参数的对象
     */
    private void checkAuth(Task task, FlowParams flowParams) {
        if (flowParams.isIgnore()) {
            return;
        }
        // 查询审批人和转办人
        List<String> permissions = StreamUtils.toList(task.getUserList(), User::getProcessedBy);
        // 当前办理人拥有的权限和设计时候填的权限集合是否有交集，有说明有权限办理
        AssertUtil.isTrue(CollUtil.isNotEmpty(permissions) && (CollUtil.isEmpty(flowParams.getPermissionFlag())
            || CollUtil.notContainsAny(flowParams.getPermissionFlag(), permissions)), ExceptionCons.NULL_ROLE_NODE);
    }


    /**
     * 一票否决（谨慎使用），如果退回，退回指向节点后还存在其他正在执行的待办任务，转历史任务，状态都为退回,重走流程。
     *
     * @param task         当前任务
     * @param targetNodeCode 下一个节点编码
     * @param flowCombine  流程数据集合
     */
    private void oneVoteVeto(Task task, String targetNodeCode, FlowCombine flowCombine) {
        // 一票否决（谨慎使用），如果退回，退回指向节点后还存在其他正在执行的待办任务，转历史任务，状态失效,重走流程。
        List<Task> tasks = list(FlowEngine.newTask().setInstanceId(task.getInstanceId()));
        // 属于退回指向节点的后置未完成的任务
        List<Task> noDoneTasks = new ArrayList<>();
        List<Node> suffixNodeList = FlowEngine.nodeService().suffixNodeList(targetNodeCode, flowCombine);
        List<String> suffixCodes = StreamUtils.toList(suffixNodeList, Node::getNodeCode);
        for (Task flowTask : tasks) {
            if (suffixCodes.contains(flowTask.getNodeCode())) {
                noDoneTasks.add(flowTask);
            }
        }
        if (CollUtil.isNotEmpty(noDoneTasks)) {
            removeAndUser(noDoneTasks);
        }
    }


    /**
     * 处理未完成的任务，当流程完成，还存在待办任务未完成，转历史任务，状态完成。
     *
     * @param instance 流程实例
     */
    private void handUndoneTask(Instance instance) {
        if (NodeType.isEnd(instance.getNodeType())) {
            List<Task> taskList = list(FlowEngine.newTask().setInstanceId(instance.getId()));
            if (CollUtil.isNotEmpty(taskList)) {
                removeAndUser(taskList);
            }
        }
    }

    /**
     * 更新流程信息
     *
     * @param task       当前任务
     * @param instance   流程实例
     * @param addTasks   新增待办任务
     * @param flowParams 包含流程相关参数的对象
     * @param nextNodes  下一个节点集合
     */
    private void updateFlowInfo(Task task, Instance instance, List<Task> addTasks, FlowParams flowParams
        , List<Node> nextNodes) {
        // 设置流程历史任务信息
        HisTask insHis = FlowEngine.hisTaskService().setSkipInsHis(task, nextNodes, flowParams);
        FlowEngine.hisTaskService().save(insHis);
        removeAndUser(Collections.singletonList(task));
        // 待办任务设置处理人
        List<User> users = FlowEngine.userService().taskAddUsers(addTasks);

        // 设置任务完成后的实例相关信息
        setInsFinishInfo(instance, addTasks, flowParams);
        if (CollUtil.isNotEmpty(addTasks)) {
            saveBatch(addTasks);
        }
        FlowEngine.instanceService().updateById(instance);
        // 保存下一个待办任务的权限人
        FlowEngine.userService().saveBatch(users);
    }

    private boolean containsSubprocessTask(List<Task> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return false;
        }
        for (Task task : tasks) {
            if (NodeType.isSubProcess(task.getNodeType())) {
                return true;
            }
        }
        return false;
    }

    private boolean definitionHasSubprocess(Long definitionId) {
        List<Node> nodes = FlowEngine.nodeService().getByDefId(definitionId);
        for (Node node : nodes) {
            if (NodeType.isSubProcess(node.getNodeType())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSubprocessChild(Instance instance) {
        return instance != null && instance.getVariableMap() != null
            && instance.getVariableMap().containsKey("flovira.subprocess.parentInstanceId");
    }

    private boolean judgeActivityStatus(Definition definition, Instance instance) {
        return ActivityStatus.isActivity(definition.getActivityStatus())
            && ActivityStatus.isActivity(instance.getActivityStatus());
    }


    @Override
    public FlowDto load(Long taskId, FlowParams flowParams) {
        R r = getAndCheck(taskId);

        FlowDto flowDto = new FlowDto();
        flowDto.setFormId(r.task.getFormId());
        flowDto.setData(r.instance.getVariableMap().get(FlowCons.FORM_DATA));

        return flowDto;
    }

    @Override
    public FlowDto hisLoad(Long hisTaskId, FlowParams flowParams) {
        HisTask hisTask = FlowEngine.hisTaskService().getById(hisTaskId);
        AssertUtil.isNull(hisTask, ExceptionCons.NOT_FOUND_FLOW_TASK);

        FlowDto flowDto = new FlowDto();
        // 历史记录使用办理时的表单引用，不重新读取当前流程配置。
        flowDto.setFormId(hisTask.getFormId());
        flowDto.setData(hisTask.getVariableMap().get(FlowCons.FORM_DATA));

        return flowDto;
    }
}
