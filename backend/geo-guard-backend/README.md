# 重庆地灾后端

Spring Boot 业务 API 与本地认证服务。仓库内 Maven reactor 包含框架模块、认证、数据、业务服务和启动应用；构建不依赖组织内 `bwy-project` 父 POM 或私有 Maven 制品。

## 独立构建

需要 JDK 21 和 Maven。于本目录运行：

```powershell
mvn -B clean verify
```

生成的 Spring Boot 包位于：

```text
chongqing-geological-disaster-start/target/chongqing-geological-disaster-start.jar
```

只需要组装可执行包时可运行 `mvn -B -DskipTests package`。Maven 编译与测试不要求模型 API Key。

## 本地运行配置

公开仓库不包含 `application*.yml`、业务数据库凭据或部署环境参数。启动前需要提供 PostgreSQL、Redis、数据库结构和应用配置，可通过 Spring Boot 外部配置文件或环境变量注入。例如将自有配置放在 `./config/` 后运行：

```powershell
java -jar chongqing-geological-disaster-start/target/chongqing-geological-disaster-start.jar --spring.config.additional-location=file:./config/ --server.port=8007
```

实际启动和接口联调需要有效的本地数据库、Redis 与对应业务配置。只有构建成功不能证明服务已经启动或连接了这些依赖。模型 API Key 仅在启用对应外部模型能力时需要，由部署者通过本地配置提供，勿提交到 Git。

Dify 知识库文件下载通过 Java 服务端代理，使用 `dizai.dify.knowledge-base-api-key`（环境变量 `DIZAI_DIFY_KNOWLEDGE_BASE_API_KEY`）和 `dizai.dify.url`。浏览器不携带该 API Key，也不要把密钥放入 `VITE_*` 前端变量。

## 当前验证

- Maven Java 编译通过。
- `mvn -B test`：48 个测试套件、387 项测试，失败 0、错误 0、跳过 0。
- Maven 可执行包构建通过；JAR 内含 Spring Boot launcher、启动类和业务依赖。实际运行依赖用户提供的数据库、Redis 和配置。

单元测试不替代外部短信、APP 推送、会商平台、数据库和模型服务的联调。
