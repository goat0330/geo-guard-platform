# 本地业务数据库

`001-schema.sql` 来自本地业务源码附带的 PostgreSQL 表结构，包含 86 个业务/系统表、11 个序列，以及对应索引和约束。保留实际字段类型与 PostGIS geometry；没有导入用户、密码、业务记录、旧服务器地址、旧账号权限或 DROP TABLE。一个未被当前 Java 源码引用的备份表未纳入。PostGIS 自己管理的空间参考表和视图由扩展创建。

`002-business-additions.sql` 提取自同一源码的业务迁移，补充褶皱、断层、临时雨量、自动模式和任务链汇总等 8 个表，以及任务链的新增列、索引。只保留结构变更，原迁移中的地质记录、查询、DROP 和部署注释不进入本项目。空库初始化按文件名顺序执行所有 SQL 文件，使用同一个 PostgreSQL 事务。

`003-local-business-views.sql` 补齐当前 Java 数据模型引用的 11 个视图。人员/房屋视图读取已有本地表；监测点、设备、传感器、预警处置、防灾预案、机构、网格员和监测字典使用 9 个独立本地表。它们不再查询原来的私有 `dzzh` schema。视图直接反映本地表更新，不需要刷新物化视图。经纬度采用 CGCS2000（4490），关联斜坡时转换到斜坡表的 WGS84（4326），保留原字段名供 mapper 读取。

三份结构文件经过真实 PostgreSQL 18.4 + PostGIS 3.6.2 空库单事务执行验证，共 103 个业务/系统表、11 个业务视图。`tests/verify-local-views.sql` 使用明确标注的测试记录验证 11 个视图、空间关联、更新和空坐标，然后回滚全部测试记录。运行中的 Java 后端对应 10 个列表接口已返回 HTTP 200 / code 200；当前业务库为空，这不代表已有真实地灾、人员或监测数据，也不代表所有业务接口均已验收。业务素材和账号需由部署者自行录入，禁止把私有数据作为开源初始化记录。

## Windows 启动

先安装 PostgreSQL 与同版本的 PostGIS，然后在后端目录执行（路径换成自己的安装位置）：

```powershell
.\initialize-local-db.ps1 -PostgreSqlBin 'D:\PostgreSQL\18\bin'
```

脚本建立项目独立的 `geo_guard` 集群和数据库，默认仅监听 `127.0.0.1:15432`；数据、日志和随机生成的密码保存在被 Git 忽略的 `.runtime/`。它不修改机器上已有 PostgreSQL 服务。已有业务表时保留原库，停止重复初始化；新库的 schema 出错时由 PostgreSQL 回滚事务，不删除数据目录。

升级之前已初始化的本项目数据库时，执行 `initialize-local-db.ps1 -PostgreSqlBin 'D:\PostgreSQL\18\bin' -ApplyLocalViews`。只应用 `003` 的新增本地表和视图，不重放已有表结构、不导入记录、不删除业务数据。向 `data_monitor_point`、`data_monitor_device` 等本地表录入自己的数据后，相应 `v_*` 视图即可查询；原私有数据接口的抓取和同步服务未迁入。

业务应用读取 `config/geo-local.yml`。启动时通过环境变量 `GEO_DB_PASSWORD` 注入本地密码，不把密码加进 YAML 或命令行参数。Redis 与业务应用的运行验收仍需单独完成。
