# 统一设计器（2026-09-08）

不再按流程区分经典和仿钉钉模式。React 不再生成模式字段；Vue 固定使用现有审批画布，移除模式选择、模式校验和切换重置逻辑，后续视觉与交互再参考 React 改造。

删除 Definition、DefJson、两套 ORM 的 modelValue 以及 MyBatis 映射；MySQL、PostgreSQL、Oracle 初始化脚本删除 model_value。SQL Server 支持及初始化脚本已按用户要求移除。

删除 ModelEnum、按模式区分的配色配置及查询参数，统一使用 chartStatusColor。原 chartStatusColorClassics/chartStatusColorMimic 的消费者需迁移到统一配置。旧 getChartRgb(String) 改为 getChartRgb()。

Vue 移除经典画布的 paletteNodes 属性和 sidebar 插槽入口。独立导出的 DiagramSidebar 组件仍可单独使用，不作为设计器模式入口。

这是公共契约变更。现有数据库若 model_value 为非空且无默认值，需要在升级前由维护者调整该约束或删除旧列，否则新代码插入可能失败；本次未执行任何数据库变更。旧数据 JSON 顶层 modelValue 可在导入前移除；前端转换输出不再包含它，不改业务扩展内部同名键。

旧经典画布定义不再恢复自由拖拽交互；重新编辑前需在统一画布中确认布局，不承诺旧自由布局自动转换为 React 风格。

验证：后端 ./gradlew test --offline 通过；前端 bun run test 通过（包含旧模式 JSON 转换和业务扩展同名键保留检查）；11 个 MyBatis resultMap 与实体字段核对通过。后续已按用户要求移除 SQL Server 支持。
前端 bun run build 全工作区构建通过，包含组件库、适配器和六个示例；仅有包体积提示。git diff --check 通过。
