# 实验一逐项核对表

该清单依据指导书“第二部分 实验一”重新整理，实验二内容不在本项目范围内。

| 编号 | 指导书要求 | 当前状态 | 证据或操作 |
|---|---|---|---|
| 1.1 | 新建数据库、员工类表和测试数据 | 已完成 | `sql/init.sql`；本机新库 `persistence_lab` |
| 1.2 | Maven 配置与依赖下载 | 项目已验证 | `mvn clean test` 可完成依赖解析；个人 Maven 镜像路径按本机填写 |
| 1.3 | Git 初始化与分阶段提交 | 需本人完成 | 不代改全局用户名/邮箱，也不伪造历史提交 |
| 1.4 | AI 插件安装与安全配置 | 需本人确认 | 截取实际插件与账号状态；不要提交口令 |
| 1.5 | 创建 Maven 项目并核对版本 | 已完成 | `pom.xml`，JAR、Java 11、MP 3.5.3.1、JUnit 4 |
| 2.1 | `mybatis-config.xml` | 已完成 | 驼峰映射、Log4j2、别名、连接池、Mapper 注册 |
| 2.2 | 实体类 | 已完成 | `StaffProfile.java`，8 个实验字段 |
| 2.3 | Mapper 与 XML | 已完成 | `resultMap`、SQL 片段、完整 CRUD |
| 2.4 | 第一个 MyBatis 程序 | 已完成 | `PersistenceLabDemo.java` |
| 2.5 | 日志集成 | 已完成 | `log4j2.xml`，控制台展示 SQL/参数/行数 |
| 2.6 | JUnit 测试 | 已完成 | `StaffProfileMapperTest` |
| 3.1 | `if/where` 多条件查询 | 已完成 | `selectBySearch` |
| 3.1 扩展 | `trim` 等价实现 | 已完成 | `selectBySearchWithTrim` 及结果一致性断言 |
| 3.2 | `set` 动态更新 | 已完成 | `updatePartial` |
| 3.3 | `foreach` 批量插入/删除 | 已完成 | `insertBatch`、`deleteBatch` |
| 3.4 | `choose` 优先分支 | 已完成 | `selectPreferred` 的三条分支均有断言 |
| 4.1 | 引入 MyBatis-Plus | 已完成 | `mybatis-plus:3.5.3.1` |
| 4.2 | 实体注解映射 | 已完成 | `@TableName`、`@TableId`、`@TableField` |
| 4.3 | `BaseMapper` 通用 CRUD | 已完成 | `MyBatisPlusFeatureTest` |
| 4.4 | 条件构造器 | 已完成 | Lambda 等值、范围和倒序 |
| 4.5 | 分页插件与分页查询 | 已完成 | `PaginationInnerInterceptor`、`Page.of` |
| 5.1 | 三类日志排错演练 | 正确代码与步骤已备 | 需本人临时制造故障、截图后恢复 |
| 5.2 | ApiFox GET 基础练习 | 操作说明已备 | 需本人在桌面端保存用例并截图 |
| 5.3 | AI 代码人工审查 | 已完成工程检查 | 依赖树、映射、参数化 SQL、构建与测试均已检查 |

## 提交前命令

```powershell
mvn clean test
mvn clean package
```

期望：5 个测试全部通过，并生成 `target/persistence-lab.jar`。

## 报告截图顺序建议

1. IDEA 项目结构和 `pom.xml`；
2. MySQL 的 `persistence_lab.staff_profile` 表结构与 6 条记录；
3. 控制台入口运行后的 SQL、参数和结果行数；
4. JUnit 绿色结果；
5. 三类临时故障各自的错误日志与修复结果；
6. MyBatis-Plus 条件查询、分页输出；
7. ApiFox 已保存的 GET 请求、请求头和响应；
8. Git 日志中的任务节点提交。
