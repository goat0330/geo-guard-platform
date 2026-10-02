# 本地业务数据库

`001-schema.sql` 来自本地业务源码附带的 PostgreSQL 表结构，包含 86 个业务/系统表、11 个序列，以及对应索引和约束。保留实际字段类型与 PostGIS geometry；没有导入用户、密码、业务记录、旧服务器地址、旧账号权限或 DROP TABLE。一个未被当前 Java 源码引用的备份表未纳入。PostGIS 自己管理的空间参考表和视图由扩展创建。

`002-business-additions.sql` 提取自同一源码的业务迁移，补充褶皱、断层、临时雨量、自动模式和任务链汇总等 8 个表，以及任务链的新增列、索引。只保留结构变更，原迁移中的地质记录、查询、DROP 和部署注释不进入本项目。空库初始化按文件名顺序执行所有 SQL 文件，使用同一个 PostgreSQL 事务。

当前文件经过真实 PostgreSQL 18.4 + PostGIS 3.6.2 单事务执行验证。这是空数据库的初始化结构；不能据此认为所有 mapper、后续迁移和外部监测视图均已对齐。业务素材和账号需由部署者自行录入，禁止把私有数据作为开源初始化记录。

## Windows 启动

先安装 PostgreSQL 与同版本的 PostGIS，然后在后端目录执行（路径换成自己的安装位置）：

```powershell
.\initialize-local-db.ps1 -PostgreSqlBin 'D:\PostgreSQL\18\bin'
```

脚本建立项目独立的 `geo_guard` 集群和数据库，默认仅监听 `127.0.0.1:15432`；数据、日志和随机生成的密码保存在被 Git 忽略的 `.runtime/`。它不修改机器上已有 PostgreSQL 服务。已有业务表时保留原库，停止重复初始化；新库的 schema 出错时由 PostgreSQL 回滚事务，不删除数据目录。

业务应用读取 `config/geo-local.yml`。启动时通过环境变量 `GEO_DB_PASSWORD` 注入本地密码，不把密码加进 YAML 或命令行参数。Redis 与业务应用的运行验收仍需单独完成。
