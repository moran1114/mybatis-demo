# MyBatis 持久层实验一

本项目只实现课程指导书中的“实验一：MyBatis 与 MyBatis-Plus 环境搭建及 CRUD 实操”。它是 Java 11 的 Maven JAR 工程，不包含 Spring、Spring MVC、Controller/Service、REST、登录拦截、AOP、Web 容器或实验二交付物。

## 实验任务对应

| 指导书任务 | 项目实现 |
|---|---|
| 任务 1：环境与数据库 | `pom.xml`、`sql/init.sql`、`database.properties` |
| 任务 2：MyBatis 基础 CRUD | `StaffProfile`、`StaffProfileMapper`、Mapper XML、`MyBatisSessions`、控制台示例 |
| 任务 3：动态 SQL 与批量操作 | XML 中的 `if`、`where`、`trim`、`set`、`foreach`、`choose` |
| 任务 4：MyBatis-Plus | `BaseMapper` 通用 CRUD、Lambda 条件构造器、分页拦截器与分页查询 |
| 任务 5：日志、排错、ApiFox 与审查 | Log4j2 SQL 日志、JUnit 4、排错记录和人工操作清单 |

项目使用 Log4j2 替代已经停止维护的 Log4j 1.x，但仍通过 MyBatis 的 `LOG4J2` 日志实现输出 SQL、参数和结果行数。

## 精简后的目录

```text
src/main/java/dev/course/persistence
├── demo/PersistenceLabDemo.java
├── entity/StaffProfile.java
├── mapper/StaffProfileMapper.java
├── query/StaffSearch.java
└── util/MyBatisSessions.java

src/main/resources
├── mapper/StaffProfileMapper.xml
├── database.properties
├── log4j2.xml
└── mybatis-config.xml
```

## 数据库初始化

[初始化脚本](sql/init.sql)创建 `persistence_lab` 数据库，重建 `staff_profile` 表并写入 6 条独立样例数据。脚本只会删除并重建该实验表，不会删除其他库表。

本机旧项目的 `mybatisdb.user` 表已按要求删除，`mybatisdb` 数据库本身保留。

默认连接配置位于 [database.properties](src/main/resources/database.properties)。建议在运行配置中用环境变量覆盖本机账号信息：

```powershell
$env:LAB_DB_URL = 'jdbc:mysql://localhost:3306/persistence_lab?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8'
$env:LAB_DB_USER = 'root'
$env:LAB_DB_PASSWORD = '你的本机密码'
```

不要把真实密码提交到项目。

## 构建与运行

先执行自动化测试：

```powershell
mvn clean test
```

测试完全使用 H2 的 MySQL 兼容模式，不修改本机 MySQL。测试覆盖：

- XML 基础 CRUD 和自增主键回填；
- `where/if` 与等价 `trim` 条件查询；
- `set` 动态更新；
- `foreach` 批量插入、批量删除；
- `choose/when/otherwise` 分支查询；
- MyBatis-Plus `BaseMapper` CRUD；
- Lambda 条件构造器和分页。

初始化 MySQL 后，可在 IDEA 直接运行：

```text
dev.course.persistence.demo.PersistenceLabDemo
```

示例会依次演示原生 MyBatis 和 MyBatis-Plus，最后主动回滚，因此不会改变 6 条种子数据。

打包命令：

```powershell
mvn clean package
```

输出为 `target/persistence-lab.jar`，不再生成实验二的 WAR。

## 报告与人工步骤

- [实验实施记录](docs/experiment-record.md)
- [实验一逐项核对表](docs/experiment-one-checklist.md)
- [ApiFox 基础练习](docs/apifox-basic-practice.md)

姓名、学号、Git 提交记录、IDEA/数据库截图、SQL 日志截图、JUnit 截图、ApiFox 实机截图和个人体会必须由实验执行者基于真实操作补充。
