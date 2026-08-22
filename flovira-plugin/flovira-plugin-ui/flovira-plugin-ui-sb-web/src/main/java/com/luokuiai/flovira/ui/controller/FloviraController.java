/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.ui.controller;

import com.luokuiai.flovira.core.dto.ApiResult;
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.FlowDto;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.dto.SubprocessSummary;
import com.luokuiai.flovira.core.dto.SubprocessChildSummary;
import com.luokuiai.flovira.core.dto.SubprocessHistoryEntry;
import com.luokuiai.flovira.core.utils.page.Page;
import com.luokuiai.flovira.core.dto.BusinessRelationQuery;
import com.luokuiai.flovira.core.dto.BusinessSubject;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.service.FloviraService;
import com.luokuiai.flovira.ui.vo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设计器默认 Controller。业务可声明本类的子类 Bean，增加业务注解或按需覆盖接口；
 * {@code FloviraUiConfig} 检测到子类后不再注册默认实例。
 *
 * @author warm
 */
@RestController
@RequestMapping("${flovira.ui-api-prefix:/flovira}")
public class FloviraController {

    /**
     * 返回流程定义和设计器配置。
     */
    @GetMapping("/config")
    public ApiResult<FloviraVo> config() {
        return FloviraService.config();
    }

    /**
     * 返回业务系统允许使用的设计能力。
     */
    @GetMapping("/integration/capabilities")
    public ApiResult<DesignerCapabilities> capabilities() {
        return FloviraService.capabilities();
    }

    /**
     * 查询业务系统提供的用户、角色、组织、表单字段、字典和子流程。
     */
    @GetMapping("/integration/resources")
    public ApiResult<DesignerResourcePage> queryResources(DesignerResourceQuery query) {
        return FloviraService.queryResources(query);
    }

    /**
     * 查询部门负责人、分管领导、角色成员和组织链等业务关系。
     */
    @PostMapping("/integration/relationships/resolve")
    public ApiResult<List<BusinessSubject>> resolveRelationship(@RequestBody BusinessRelationQuery query) {
        return FloviraService.resolveRelationship(query);
    }

    /**
     * 保存流程json字符串
     *
     * @param defJson 流程数据集合
     * @return {@code ApiResult<Void>}
     * @throws Exception 异常
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    @PostMapping("/save-json")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Void> saveJson(@RequestBody DefJson defJson, @RequestHeader("onlyNodeSkip") boolean onlyNodeSkip) throws Exception {
        return FloviraService.saveJson(defJson, onlyNodeSkip);
    }

    /**
     * 获取流程定义数据(包含节点和跳转)
     *
     * @param id 流程定义id
     * @return {@code ApiResult<DefVo>}
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    @GetMapping(value = {"/query-def", "/query-def/{id}"})
    public ApiResult<DefJson> queryDef(@PathVariable(value = "id", required = false) Long id) {
        return FloviraService.queryDef(id);
    }

    /**
     * 获取流程图
     *
     * @param id 流程实例id
     * @return {@code ApiResult<DefJson>}
     */
    @GetMapping("/query-flow-chart/{id}")
    public ApiResult<DefJson> queryFlowChart(@PathVariable("id") Long id) {
        return FloviraService.queryFlowChart(id);
    }

    @GetMapping("/subprocess/summary/{parentTaskId}")
    public ApiResult<SubprocessSummary> subprocessSummary(@PathVariable("parentTaskId") Long parentTaskId) {
        return FloviraService.subprocessSummary(parentTaskId);
    }

    @GetMapping("/subprocess/runs/{runId}/children")
    public ApiResult<Page<SubprocessChildSummary>> subprocessChildren(@PathVariable("runId") Long runId,
        @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return FloviraService.subprocessChildren(runId, pageNum, pageSize);
    }

    @GetMapping("/subprocess/runs/{runId}/events")
    public ApiResult<List<SubprocessEvent>> subprocessEvents(@PathVariable("runId") Long runId) {
        return FloviraService.subprocessEvents(runId);
    }

    @GetMapping("/subprocess/runs/{runId}/history")
    public ApiResult<List<SubprocessHistoryEntry>> subprocessHistory(@PathVariable("runId") Long runId,
        @RequestParam(value = "childId", required = false) Long childId) {
        return FloviraService.subprocessHistory(runId, childId);
    }

    /**
     * 读取表单内容
     *
     * @param id
     * @return
     */
    @GetMapping("/form-content/{id}")
    public ApiResult<String> getFormContent(@PathVariable("id") Long id) {
        return FloviraService.getFormContent(id);
    }

    /**
     * 保存表单内容,该接口不需要系统实现
     *
     * @param flowDto
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/form-content")
    public ApiResult<Void> saveFormContent(@RequestBody FlowDto flowDto) {
        return FloviraService.saveFormContent(flowDto);
    }


    /**
     * 根据任务id获取待办任务表单及数据
     *
     * @param taskId 当前任务id
     * @return {@link ApiResult< FlowDto >}
     * @author liangli
     * Date: 2024/8/21 17:08
     **/
    @GetMapping(value = "/execute/load/{taskId}")
    public ApiResult<FlowDto> load(@PathVariable("taskId") Long taskId) {
        return FloviraService.load(taskId);
    }

    /**
     * 根据任务id获取已办任务表单及数据
     *
     * @param hisTaskId
     * @return
     */
    @GetMapping(value = "/execute/hisLoad/{taskId}")
    public ApiResult<FlowDto> hisLoad(@PathVariable("taskId") Long hisTaskId) {
        return FloviraService.hisLoad(hisTaskId);
    }

    /**
     * 通用表单流程审批接口
     *
     * @param formData
     * @param taskId
     * @param skipType
     * @param message
     * @param nodeCode
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping(value = "/execute/handle")
    public ApiResult<Instance> handle(@RequestBody Map<String, Object> formData, @RequestParam("taskId") Long taskId
        , @RequestParam("skipType") String skipType, @RequestParam("message") String message
        , @RequestParam(value = "nodeCode", required = false) String nodeCode) {
        return FloviraService.handle(formData, taskId, skipType, message, nodeCode);
    }

}
