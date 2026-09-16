# 发布版本

前后端共用 `v` 标签，不使用独立前端标签前缀。

| 触发方式 | Maven 版本 | npm 渠道 |
| --- | --- | --- |
| 推送 `develop` | `1.0.0-<提交前七位>-SNAPSHOT` | 不发布 |
| 推送 `v1.0.0-alpha.1` | `1.0.0-alpha.1` | 同版本发布到 `next` |
| 推送 `v1.0.0` | `1.0.0` | 同版本发布到 `latest` |

后端接受 `vX.Y.Z` 和 `vX.Y.Z-alpha.N`，其中 N 为非负整数，不带前导零。
alpha 使用 Maven Central 非 Snapshot 发布流程，不追加 `-SNAPSHOT`。
beta、RC 和其他标签格式目前不在后端允许范围内。

## 标签与前端版本

前端采用 Lerna 统一版本，标签不会自动修改 npm 包版本。发布前应在 `main`
同步前端代码，使用已有 release 命令更新 Vue、React、adapters 和 Lerna 版本：

```bash
cd flovira-designer
bun run release -- 1.0.0-alpha.1
```

该命令会创建版本提交并推送标签，触发前后端发布，必须在确认要公开发布时执行。
正式版将参数换成 `1.0.0`。不要只手动打标签而保留旧的前端包版本。

前端需要各 npm 包配置 Trusted Publishing；后端需要现有 Maven Central 凭据和签名配置。
两条流水线独立执行，不是原子发布：须分别检查结果，不可将其中一条成功视为全部成功。
本次脚本调整不执行任何实际发布，也不修改已有包版本或标签。
