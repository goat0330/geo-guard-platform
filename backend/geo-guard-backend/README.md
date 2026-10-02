# 重庆地灾后端

Spring Boot 业务 API 与本地认证服务。仓库内 Maven reactor 包含框架模块、认证、数据、业务服务和启动应用；构建不依赖组织内 `bwy-project` 父 POM 或私有 Maven 制品。

## 独立构建

Windows x64 的 JDK 21、Maven 3.9.16 和完整 Maven 依赖缓存随 GitHub Release 提供；下载后用脚本自动解压到被 Git 忽略的 `.offline-build-kit/`，构建使用随包 JDK 并强制 Maven 离线模式，不访问 Maven Central：

```powershell
.\build-offline.ps1 -KitArchive .\geo-guard-offline-build-kit.zip
```

如果已经解压过离线包，可直接运行 `.\build-offline.ps1`。离线包包括 Temurin JDK 21、Maven 3.9.16 和通过 33 个 reactor 模块离线验证所需的依赖缓存。`prepare-offline-kit.ps1` 是维护者从 JDK、Maven 发行版与仓库重新生成 Release 资产的脚本。

生成的 Spring Boot 包位于：

```text
chongqing-geological-disaster-start/target/chongqing-geological-disaster-start.jar
```

完整 `verify` 会运行 Java 测试并组装应用。Maven 编译与测试不要求模型 API Key。

## 本地运行配置

公开仓库提供不含密钥的 `config/geo-local.yml` 与 `database/` 空库结构；数据库密码、账号和外部服务凭据由部署者本地设置。Windows 可先使用已安装的 PostgreSQL/PostGIS 初始化项目独立集群：

```powershell
.\initialize-local-db.ps1 -PostgreSqlBin 'D:\PostgreSQL\18\bin'
```

默认数据库位于 `127.0.0.1:15432`。结构包含 94 个业务/系统表，不导入旧业务数据或用户账号；脚本已完成全新集群初始化验证。详见 `database/README.md`。数据库密码通过 `GEO_DB_PASSWORD` 注入，或使用初始化脚本生成的 `.runtime/postgres-password.txt`，勿放入源码或命令行。

本地业务 Redis 使用独立容器，默认仅绑定 `127.0.0.1:26379`，AOF 数据位于项目 `.runtime/redis-data`。不依赖 Yuxi 的 Redis 或完整 Docker Compose。先启动 Docker Engine，导入缓存的 `redis:7.4.10-alpine` 镜像，再运行下面的脚本；初始化脚本不会自动下载镜像。离线部署可以预先 `docker save` / `docker load` 该镜像。外部 Redis 可通过 `GEO_REDIS_HOST`、`GEO_REDIS_PORT`、`SPRING_DATA_REDIS_PASSWORD` 设置。

```powershell
.\initialize-local-redis.ps1
.\initialize-local-admin.ps1 -PostgreSqlBin 'D:\PostgreSQL\18\bin'
.\start-local-business.ps1
.\smoke-local-auth.ps1
```

这些运行脚本使用 PowerShell 7。Docker 不在 PATH 时传入 `-DockerExecutable` 的真实可执行文件路径。管理员初始化只适用于空用户表，已有用户会被保留。可提前设置 `GEO_ADMIN_PASSWORD`（12–30 字符）；未设置时生成随机密码，仅保存在被 Git 忽略的 `.runtime/admin-password.txt`，不会回显或提交。登录客户端 ID 为 `geo-local`。

启动脚本使用离线包内 JDK，读取本地数据库密码，并生成或复用 `.runtime/jwt-secret.txt`；也可通过 `GEO_JWT_SECRET` 注入签名密钥。服务监听 `127.0.0.1:8008`，日志在 `.runtime/business.out.log` 和 `business.err.log`。此端口用于完整业务服务联调，现有 `:8007` 认证/RAG 门面尚未替换。直接运行 JAR 时须自行注入数据库密码与 JWT 密钥。图形验证码已开启，真实答案和有效期存入独立 Redis；短信、邮箱和外部模型需要各自配置。

认证 smoke 使用真实 HTTP、PostgreSQL 和 Redis；为自动化校验，仅在测试进程内读取服务刚生成的验证码答案，浏览器接口不返回答案。菜单查询当前返回空列表，还需要初始化本项目的菜单。

实际启动和接口联调需要有效的本地数据库、Redis 与对应业务配置。只有构建成功不能证明服务已经启动或连接了这些依赖。模型 API Key 仅在启用对应外部模型能力时需要，由部署者通过本地配置提供，勿提交到 Git。

Dify 知识库文件下载通过 Java 服务端代理，使用 `dizai.dify.knowledge-base-api-key`（环境变量 `DIZAI_DIFY_KNOWLEDGE_BASE_API_KEY`）和 `dizai.dify.url`。浏览器不携带该 API Key，也不要把密钥放入 `VITE_*` 前端变量。

## 当前验证

- 使用完整离线构建包在干净源码副本构建；临时屏蔽系统 Java/Maven 路径，确认使用包内 Temurin JDK 21 和 Maven。
- `-o -B verify`：33 个 reactor 模块成功，48 个测试套件、387 项测试，失败 0、错误 0、跳过 0。
- Maven 可执行包构建通过；JAR 内含 Spring Boot launcher、启动类和业务依赖。实际运行依赖用户提供的数据库、Redis 和配置。
- PostgreSQL 18.4 / PostGIS 3.6.2：94 个业务/系统表在全新集群初始化成功；再次运行脚本保留已有表。Redis 7.4.10 返回 `PONG`，数据位于 D 盘。
- 完整业务 JAR 使用包内 JDK 21 启动成功并监听 `:8008`；图形验证码、Redis 有效期、验证码不可复用、错误密码拒绝、真实管理员登录、用户信息、菜单查询、客户端 ID 校验、退出和退出后拒绝均通过真实运行验收。管理员再次初始化保留已有账号。
- 监测视图、本项目菜单数据、完整业务 API、前端鉴权和最终 `:8007` 门面替换尚待联调；上述认证结果不等于全部业务运行验收完成。

单元测试不替代外部短信、APP 推送、会商平台、数据库和模型服务的联调。
