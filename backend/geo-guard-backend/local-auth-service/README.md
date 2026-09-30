# geo-guard 本地认证服务

这是独立的本地开发兼容服务，不依赖原后端缺失的私有 Maven 父 POM、业务数据库或 Redis。它只负责登录闭环所需的验证码、账号校验、用户信息、空权限路由、心跳和空闲 SSE 连接；不会冒充原地灾业务后端。

## 启动

在 PowerShell 中：

```powershell
cd backend/geo-guard-backend/local-auth-service
.\build-local.ps1
.\run-local.ps1
```

`run-local.ps1` 会在当前终端安全提示本地账号和密码。密码只作为当前服务进程的环境变量使用；服务不保存账号或密码。验证码通过 JDK 生成 GIF 图片，5 分钟过期且一次性使用。会话令牌保存在内存中，服务重启后失效。

服务仅监听 `127.0.0.1:8007`。前端已通过被 Git 忽略的 `.env.development.local` 指向此地址；Vite 需要重启一次才能读取新代理目标。

如需本地 Dify 知识库文件下载代理，可在启动前设置 `GEO_DIFY_BASE_URL` 和 `GEO_DIFY_KNOWLEDGE_BASE_API_KEY`。密钥只留在 Java 服务进程环境中，不要写入前端变量或仓库。

## 已实现接口

- `GET /auth/code`
- `POST /auth/login`
- `POST /auth/logout`
- `GET /system/user/getInfo`
- `GET /system/menu/getRouters`
- `POST /dizai/online/heartbeat`
- `GET /dizai/sse/connect`
- `GET /dify/datasets/{datasetId}/documents/{documentId}/upload-file`
- `GET /health`

## 边界

该服务适合本机调试智能体广场的登录门禁，不提供真实用户数据库、短信/SSO、业务数据接口或生产级授权。用户信息和权限是本地开发身份；不能部署到公网或替代正式认证系统。后续业务接口应按前端真实页面逐项接入独立后端和 RAG/LangGraph 服务。
