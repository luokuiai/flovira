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
package com.luokuiai.flovira.core.config;

import lombok.Getter;
import lombok.Setter;
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.enums.ChartStatus;
import com.luokuiai.flovira.core.enums.FrameworkType;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.utils.ServiceLoaderUtil;
import com.luokuiai.flovira.core.utils.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Flovira属性配置文件
 *
 * @author warm
 */
@Getter
@Setter
public class Flovira implements Serializable {

    public static final int DEFAULT_SUBPROCESS_MAX_CHILDREN = 128;

    /**
     * 开关
     */
    private boolean enabled = true;

    /**
     * 框架类型
     */
    private FrameworkType framework;

    /**
     * 启动banner
     */
    private boolean banner = true;

    /**
     * id生成器类型, 不填默认为orm扩展自带生成器或者flovira内置的19位雪花算法, SnowId14:14位，SnowId15:15位， SnowFlake19：19位
     */
    private String keyType;

    /**
     * 是否开启逻辑删除
     */
    private boolean logicDelete = false;

    /**
     * 逻辑删除字段值
     */
    private String logicDeleteValue = "2";

    /**
     * 逻辑未删除字段
     */
    private String logicNotDeleteValue = "0";

    /**
     * 数据填充处理类路径
     */
    private String dataFillHandlerPath;

    /**
     * 租户模式处理类路径
     */
    private String tenantHandlerPath;

    /**
     * 办理人权限处理器类路径
     */
    private String permissionHandlerPath;

    /**
     * 全局监听器类路径
     */
    private String globalListenerPath;

    /**
     * 数据源类型, mybatis模块对orm进一步的封装, 由于各数据库分页语句存在差异,
     * 当配置此参数时, 以此参数结果为基准, 未配置时, 取DataSource中数据源类型,
     * 兜底为mysql数据库
     */
    private String dataSourceType;

    /**
     * ui开关
     */
    private boolean ui = true;

    /**
     * 如果需要工作流共享业务系统权限，默认Authorization，如果有多个token，用逗号分隔
     */
    private String tokenName = "Authorization";

    /**
     * 公共模型流程状态对应的三原色
     */
    private List<String> chartStatusColor;

    /**
     * 经典模式流程状态对应的三原色
     */
    private List<String> chartStatusColorClassics;

    /**
     * 仿钉钉模式流程状态对应的三原色
     */
    private List<String> chartStatusColorMimic;

    /**
     * 是否显示流程图顶部文字
     */
    private boolean topTextShow = true;

    /**
     * 单次子流程运行允许创建的最大子实例数
     */
    private int subprocessMaxChildren = DEFAULT_SUBPROCESS_MAX_CHILDREN;

    /**
     * 节点超时执行配置
     */
    private Timeout timeout = new Timeout();

    @Getter
    @Setter
    public static class Timeout implements Serializable {
        private boolean enabled = false;
        private long scanIntervalSeconds = 60L;
        private int batchSize = 100;
        private long claimTimeoutMillis = 300000L;
        private String schedulerLockKey = "flovira:timeout:scheduler";
    }

    public void init() {
        if (subprocessMaxChildren < 1) {
            throw new IllegalArgumentException("flovira.subprocess-max-children must be greater than 0");
        }
        if (timeout == null || timeout.getScanIntervalSeconds() < 1L || timeout.getBatchSize() < 1
            || timeout.getClaimTimeoutMillis() < 1000L || StringUtils.isEmpty(timeout.getSchedulerLockKey())) {
            throw new IllegalArgumentException("flovira.timeout configuration is invalid");
        }
        // 设置租户模式
        FlowEngine.initTenantHandler(this.getTenantHandlerPath());

        // 设置数据填充处理类
        FlowEngine.initDataFillHandler(this.getDataFillHandlerPath());

        // 设置办理人权限处理类
        FlowEngine.initPermissionHandler(this.getPermissionHandlerPath());

        // 设置全局监听器
        FlowEngine.initGlobalListener(this.getGlobalListenerPath());

        // 打印banner图
        printBanner();

        // 初始化流程状态对应的自定义三原色
        ChartStatus.initCustomColor(this.getChartStatusColor(), this.getChartStatusColorClassics(), this.getChartStatusColorMimic());

        // 通过SPI机制
        spiLoad();

    }

    public void spiLoad() {
        // 通过SPI机制加载json转换策略实现类
        List<JsonConvert> jsonConverts = ServiceLoaderUtil.loadList(JsonConvert.class);
        if (jsonConverts.size() != 1) {
            throw new IllegalStateException("Exactly one JsonConvert provider is required, found: "
                + jsonConverts.size());
        }
        FlowEngine.jsonConvert = jsonConverts.get(0);
    }

    private void printBanner() {
        if (this.isBanner()) {
            System.out.println("\n" +
                "    ______ _            _              \n" +
                "   |  ____| |          (_)             \n" +
                "   | |__  | | _____   ___ _ __ __ _    \n" +
                "   |  __| | |/ _ \\ \\ / / | '__/ _` |   \n" +
                "   | |    | | (_) \\ V /| | | | (_| |   \n" +
                "   |_|    |_|\\___/ \\_/ |_|_|  \\__,_|   \n" +
                "\n" +
                "\033[32m   :: Flovira ::     (v" + Flovira.class.getPackage()
                .getImplementationVersion() + ")\033[0m\n");
        }
    }

}
