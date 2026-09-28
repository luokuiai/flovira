# 超时功能接入

**业务系统必须自行接入超时调度。Flovira 不启动定时任务，也不自动配置 Redis 调度锁。**

在设计器中配置节点超时，并由宿主代码调用 `FlowEngine.setTimeoutEnabled(true)`，只会让新建任务保存截止时间和超时动作，并允许调用执行接口。没有宿主调度或消息触发，到期任务不会自动推进。

## 全局开关

全局开关不绑定 Spring Boot 配置，宿主应在初始化时显式开启：

```java
FlowEngine.setTimeoutEnabled(true);
```

默认关闭。关闭时不会给新任务写入可执行的超时快照，执行接口不推进任务。重新开启不会给之前缺少快照的任务补算截止时间；已有快照保留原截止时间。

## 方式一：由宿主定时调用

宿主已有调度平台时，在任务处理函数中调用：

```java
TimeoutExecutionResult result = FlowEngine.timeoutService().executeDue(new Date(), 100);
```

结果包含 `scanned`、`claimed`、`succeeded` 和 `failed`。单个任务失败会记录日志并继续处理后续任务；宿主必须监控 `failed`，安排下一次扫描重试。抢占或执行失败会回滚该任务事务。不要在整批调用外包一个大事务，否则任务之间不能独立提交。

以下是宿主 Spring 应用中的最小示例，**需要业务自行添加**：

```java
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.TimeoutExecutionResult;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class WorkflowTimeoutScheduling {
    private static final Logger log = LoggerFactory.getLogger(WorkflowTimeoutScheduling.class);

    @Scheduled(fixedDelayString = "${app.workflow-timeout-delay-millis:60000}")
    public void processTimeouts() {
        // 多租户应用：宿主在调用前设置当前租户上下文，并在调用后清理。
        TimeoutExecutionResult result = FlowEngine.timeoutService().executeDue(new Date(), 100);
        if (result.getFailed() > 0) {
            log.error("Workflow timeout failures: {}", result.getFailed());
        }
    }
}
```

此示例不包含集群调度锁。多实例应用可自行接入 ShedLock + Redis，或复用现有调度平台的协调能力。同一业务集群使用相同锁标识，不同应用或环境区分锁标识。锁的租约、续期、Redis 故障策略均由宿主负责。即使没有调度锁，引擎仍对每个任务进行数据库条件抢占，但各实例可能重复查询、等待行锁。

调用频率决定触发延迟；积压超过一批时，宿主应安排后续批次。不要仅根据 `scanned == batchSize` 无限重试同一批持续失败的任务，应限制连续批数并设置重试间隔和告警。

## 方式二：由宿主延迟消息触发

```java
boolean executed = FlowEngine.timeoutService().executeTimeout(taskId);
```

- 返回 `true`：本次成功执行超时动作。
- 返回 `false`：超时功能关闭、任务不存在或已完成、尚未到期，或者没有抢到任务。不会强制通过未到期任务。
- 执行失败：抛出异常，数据库事务回滚，由宿主消息重试策略决定后续处理。
- `taskId=null`：抛出参数异常。

宿主负责在任务持久化成功后安排消息投递；本次不提供消息发布器。消息可能提前到达，`false` 不代表永远无需处理，必须安排再次触发或补漏扫描。需要可靠投递时，由宿主使用事务消息或 outbox，并监控丢失、积压和失败。单任务入口与批量入口共用执行逻辑。

## 事务与并发边界

必须提供真实的 `TransactionExecutor`；Spring 集成使用已有事务管理器。普通超时任务的抢占和推进在同一事务中，数据库写锁保持到提交或回滚，不再单独提交抢占后再执行。等待节点的超时与外部恢复信号复用同一 WAIT 抢占入口。

宿主负责设置租户和数据源上下文；引擎沿用正常 DAO 的租户隔离。多租户调度应在对应租户上下文中调用，不应通过关闭租户过滤来实现扫描。

数据库事务不能回滚已发送的消息或外部 HTTP 副作用。业务监听器仍需幂等或 outbox；本功能不承诺外部副作用恰好一次，也不取代宿主对其他人工操作的并发控制。

## 从 alpha.3 及之前版本迁移

本次直接调整开发阶段接口和配置：

1. 在升级前准备好宿主调度任务或延迟消息消费者，并明确扫描周期、批量大小、失败重试、租户上下文和集群协调。
2. 移除全部 `flovira.timeout.*` 配置；将扫描间隔、批量大小和锁配置迁入宿主，并使用 `FlowEngine.setTimeoutEnabled(true)` 开启全局能力。批量大小通过 `executeDue(now, batchSize)` 传入。
3. 删除对 `TimeoutSchedulerLock`、`FlowEngine.setTimeoutSchedulerLock`、`FlowEngine.timeoutSchedulerLock`、`TimeoutSchedulingConfig`、`SpringRedisTimeoutSchedulerLock` 的引用。需要 Redis 调度锁时由业务自行配置。
4. 新版本不再因存在 `StringRedisTemplate` 自动创建锁，也不会隐式启用 Spring 定时调度。仅升级依赖而未接入宿主调度，会停止自动超时推进。
5. 切换期间先停止旧实例调度并等待在途处理完成，再启用新宿主调度，避免新旧事务策略同时执行。验收到期审批、等待恢复、重复消息和失败重试。

无需修改 MySQL、PostgreSQL、Oracle 表结构或历史数据，也不要重跑初始化 SQL。已有任务截止时间不变；旧普通任务遗留的过期 `RUNNING` 状态仍会按引擎默认恢复窗口重新参与抢占。

回滚时先停宿主调度和消息消费、等待在途事务完成，再回退依赖并恢复旧调度配置。不要同时运行旧内置调度与新宿主任务。
