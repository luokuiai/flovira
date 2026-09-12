# Flovira 可组合完整示例

本目录提供可自由组合的完整示例：后端可选 PostgreSQL 或 MySQL，前端可选 React + Lumen、React + Ant Design 或 Vue + Ant Design Vue。所有前端使用同一套 HTTP 契约，因此只需修改启动时的后端地址，不需要修改源码。

## 组合矩阵

| 层 | 变体 | 默认地址 |
| --- | --- | --- |
| 数据库 | PostgreSQL 17 | `localhost:5433` |
| 数据库 | MySQL 8.4 | `localhost:3307` |
| 后端 | PostgreSQL | `http://localhost:8081` |
| 后端 | MySQL | `http://localhost:8082` |
| 前端 | React + Lumen | `http://localhost:5191` |
| 前端 | React + Ant Design | `http://localhost:5192` |
| 前端 | Vue + Ant Design Vue | `http://localhost:5193` |

后端业务 API 固定为 `/api/example/v1`，设计器 API 固定为 `/flovira`。前端默认连接 PostgreSQL 后端；设置 `EXAMPLE_BACKEND_URL=http://localhost:8082` 即可连接 MySQL 后端。

## 目录与公共代码

```text
flovira-example/
├── backend/
│   ├── common/       # API、业务数据、设计器数据源、身份上下文和工作流逻辑
│   ├── postgres/     # PostgreSQL 驱动、配置、入口和示例业务表 SQL
│   └── mysql/        # MySQL 驱动、配置、入口和示例业务表 SQL
├── frontend/
│   ├── common/       # 与框架无关的 DTO、HTTP 客户端和后端选择
│   ├── react-common/ # 两个 React 应用共用的生命周期 UI/状态
│   ├── react-lumen/
│   ├── react-antd/
│   └── vue-antd/
└── compose.yaml
```

两个后端只保留数据库差异，公共模块不依赖任何数据库驱动。两个 React 应用共享完整的 React 页面逻辑；Vue 应用复用框架无关的 HTTP 公共包。三套前端均通过仓库本地的设计器包和独立 UI 适配器消费组件，不复制设计器实现。

## 前置条件

- JDK 17（示例使用 Spring Boot 3）
- Docker Compose
- Bun 1.3 或更高版本

## 启动

先从仓库根目录启动数据库。初始化会依次执行权威 Flovira 建库脚本 `sql/<dialect>/flovira-v1.sql` 和对应后端中的示例业务表脚本。

```bash
docker compose -f flovira-example/compose.yaml up -d postgres mysql
```

分别启动需要的后端；可以只启动其中一个，也可以同时启动：

```bash
./gradlew :flovira-example-backend-postgres:bootRun
./gradlew :flovira-example-backend-mysql:bootRun
```

首次运行前构建设计器本地包并安装示例工作区依赖：

```bash
cd flovira-designer
bun install
bun run build:designer
cd ../flovira-example/frontend
bun install
```

任选一个前端连接默认 PostgreSQL 后端：

```bash
bun run dev:react-lumen
bun run dev:react-antd
bun run dev:vue-antd
```

连接 MySQL 后端时只需设置环境变量，例如：

```bash
EXAMPLE_BACKEND_URL=http://localhost:8082 bun run dev:react-lumen
EXAMPLE_BACKEND_URL=http://localhost:8082 bun run dev:react-antd
EXAMPLE_BACKEND_URL=http://localhost:8082 bun run dev:vue-antd
```

`EXAMPLE_SERVER_PORT`、`EXAMPLE_DATABASE_URL`、`EXAMPLE_DATABASE_USERNAME` 和 `EXAMPLE_DATABASE_PASSWORD` 可覆盖后端默认配置。

## 演示流程

1. 选择 `alice`，打开启动时自动创建并发布的采购审批流程，也可以新建设计并保存流程。
2. 发布流程定义，使用宿主业务数据 `purchase-001` 启动流程。
3. 切换到 `manager`，查看并通过或驳回经理任务。
4. 通过后切换到 `finance` 处理财务任务。
5. 在进度区域查看当前节点、历史任务、流程快照和宿主采购单快照。

示例流程包含 PASS 和 REJECT 连线。进程重启后，定义、实例、任务和历史仍保存在数据库中；启动初始化器按稳定 `flowCode` 检查，不会重复创建默认定义。

## 验证

后端公共测试与两个可执行包：

```bash
./gradlew :flovira-example-backend-common:test \
  :flovira-example-backend-postgres:bootJar \
  :flovira-example-backend-mysql:bootJar
```

三套前端公共测试与生产构建：

```bash
cd flovira-example/frontend
bun test
bun run build
```

## 重置开发数据

以下命令会永久删除两个示例数据库卷，只能用于本地开发：

```bash
docker compose -f flovira-example/compose.yaml down -v
docker compose -f flovira-example/compose.yaml up -d postgres mysql
```

若只重置一种数据库，请先停止并删除对应服务，再删除 `flovira-example_postgres-data` 或 `flovira-example_mysql-data` 卷并重新启动该服务。不要对已有业务数据库重新执行 V1 全量建库脚本。

## 仅限开发环境

Compose 中的口令、种子用户、`X-Demo-User` 身份头、固定采购单和 Vite 代理都仅用于演示。生产应用必须替换为真实认证与授权、密钥管理、CORS 策略和业务数据源。采购单和表单始终由宿主应用持有；Flovira 只保存 `businessId`、`formId` 及审批快照，不提供内置表单管理。
