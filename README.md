# 员工管理 MyBatis / MyBatis-Plus 实验

Java 11+ 的 Maven 控制台项目。实验数据库为 `persistence_lab`，员工表为：

```text
emp(empno, ename, job, mgr, hiredate, sal, comm, deptno)
```

字段依次为员工编号、姓名、职位、直属上级编号、入职日期、工资、奖金、部门编号。`empno` 为自增主键；`sal/comm` 使用 DECIMAL / BigDecimal；`hiredate` 使用 DATE / LocalDate；`deptno` 使用 INT / Integer。`mgr` 和 `comm` 允许为 NULL，奖金为 0 与奖金为 NULL 含义不同。

## 五项实验任务

| 任务 | 实现位置与内容 |
|---|---|
| 1. 开发环境与项目初始化 | `pom.xml` 的 artifactId 为 `mybatis-lab`；`sql/init.sql` 创建 `persistence_lab.emp` 和 8 条数据；Git 已初始化。按本次约定跳过 Maven 镜像、插件登录和 IDEA 设置 |
| 2. MyBatis 基础 CRUD | `mybatis-config.xml`、`Emp`、`EmpMapper`、`MyBatisUtil`、`EmpDemo`；Log4j2 SQL 日志、JUnit 测试 |
| 3. 动态 SQL 与批量操作 | `selectByCondition(Emp)` 的 `if/where`、等效 `trim`、`set`、`foreach`、`choose/when/otherwise` |
| 4. MyBatis-Plus | Mapper 包扫描、实体注解、`BaseMapper<Emp>` CRUD、Lambda 条件构造器和分页插件 |
| 5. 日志排查与 AI 审查 | `FaultDiagnosisTest` 复现并修复三类故障；[实验审查报告](docs/experiment-report.md)记录依赖、SQL、映射审查。ApiFox 桌面操作按约定暂缓 |

本轮代码和 H2 自动化验证已完成，代码、SQL、测试与审查材料统一记录在本次 Git 提交中；本机 MySQL 表结构和演示运行仍需验收。

## 项目文件

```text
sql/init.sql
sql/basic-queries.sql
src/main/java/dev/course/persistence/
  entity/Emp.java
  query/EmpSort.java
  mapper/EmpMapper.java
  util/MyBatisUtil.java
  demo/EmpDemo.java
src/main/resources/
  mybatis-config.xml
  database.properties
  log4j2.xml
  dev/course/persistence/mapper/EmpMapper.xml
src/test/java/dev/course/persistence/
  mapper/EmpMapperTest.java
  mapper/EmpBasicQueryTest.java
  mapper/EmpMyBatisPlusTest.java
  mapper/FaultDiagnosisTest.java
  support/EmbeddedDatabase.java
src/test/resources/
  schema-h2.sql
  data-h2.sql
```

## 初始化与运行

1. 使用 JDK 11 或更高版本，在 IDEA 中以 Maven 项目打开 `pom.xml`。
2. 在 MySQL 客户端执行 `sql/init.sql`。脚本会重建 `persistence_lab.emp` 并插入 8 条样例数据，重复执行会清空该员工表。已有上一版字段的表也需要用新脚本重建。
3. 在 `src/main/resources/database.properties` 中配置本机连接，或在 IDEA 运行配置中设置 `LAB_DB_URL`、`LAB_DB_USER`、`LAB_DB_PASSWORD` 环境变量。
4. 执行 `sql/basic-queries.sql`，查看要求的 10 条基础查询结果。
5. 运行 `dev.course.persistence.demo.EmpDemo`。程序依次展示 10 条基础查询、原生 MyBatis CRUD 和动态 SQL、MyBatis-Plus；控制台同时输出 SQL 和结果。

两个演示事务最后均回滚，新增、修改、删除的数据不会持久保存，但数据库自增序列可能推进。需要保存业务修改时，在对应会话中调用 `session.commit()`。

## 原生 MyBatis 与 MyBatis-Plus 的配置区别

`MyBatisUtil.buildMyBatis(...)` 使用原生 `SqlSessionFactoryBuilder`，用于 XML CRUD 和动态 SQL。

`MyBatisUtil.build(...)` 替换为 `MybatisSqlSessionFactoryBuilder`，自动注入 `BaseMapper` 的 `insert/selectById/updateById/deleteById` 等 SQL，并注册 `MybatisPlusInterceptor` 与 `PaginationInnerInterceptor`。两种方式共用 `mybatis-config.xml` 和 `EmpMapper.xml`，原生模式调用 XML 或注解中声明的方法。Mapper 通过 `<package name="dev.course.persistence.mapper"/>` 注册；XML 放在与接口全限定名对应的资源目录，供框架自动加载。

保留 MyBatis-Plus 3.5.3.1。该版本默认的泛型解析依赖 Spring Core，工具类中的 JDK 泛型解析器用于让此控制台工程独立运行。

## 动态 SQL 使用说明

- `selectByCondition(Emp)`：`ename/job` 使用模糊匹配，`deptno` 使用等值匹配，`sal` 表示工资下限。各个非空条件通过 `AND` 叠加；无条件时查询全部。
- `selectByConditionWithTrim(Emp)`：使用 `<trim prefix="WHERE" prefixOverrides="AND |OR ">` 实现等效查询，与 `where` 复用条件片段。
- `countByDeptno(Integer)`：演示 `@Select` 注解与 `@Param` 参数绑定。
- `selectSorted(EmpSort)`：演示 `${sort.column}` 用于动态排序列；参数只能是枚举中的 `EMPNO/SAL/HIREDATE`，不可传入任意列名字符串。所有用户值仍使用 `#{}`。
- `updatePartial(Emp)`：必须传入 `empno` 和至少一个非 null 的待更新字段；只更新已提供的字段，`comm=0` 也会更新。null 表示跳过该字段，若需要把 `mgr/comm` 清空为 NULL，请读取完整员工后调用 `updateEmp`。
- `insertBatch(Collection<Emp>)`：传入非空员工集合，一条 INSERT 写入多行。每个员工须提供姓名、职位、入职日期、工资和部门编号；上级编号和奖金可以为 null。
- `deleteBatch(Collection<Long>)`：使用 `IN` 批量删除；null 或空集合返回 0，不删除员工。

### 步骤 3.4：choose 与 if 的区别

`selectPreferred(exactName, deptno)` 按参数是否为空选择分支：姓名非空时按姓名精确查询；否则部门编号非 null 时按部门编号查询；否则查询全部。姓名分支没有查到记录，也不会继续执行部门分支。

例如同时传入姓名“林澈”和部门编号 `30`：`selectPreferred` 返回林澈，因为只采用姓名条件；`selectByCondition` 同时约束姓名和部门，返回空列表。参见 [MyBatis 动态 SQL 官方说明](https://mybatis.org/mybatis-3/dynamic-sql.html)。

## 必须掌握的 10 条基础查询

完整 SQL 在 [sql/basic-queries.sql](sql/basic-queries.sql)，对应语句也写入 `EmpMapper.xml`。

| 编号 | 查询内容 | Mapper 方法 | 初始数据结果数 |
|---|---|---|---|
| 1 | 所有员工的所有字段 | `selectAllEmps` | 8 |
| 2 | 姓名、工资、部门编号 | `selectNameSalDeptno` | 8 |
| 3 | 工资大于 10000 | `selectSalAbove10000` | 4 |
| 4 | 部门编号为 20 或 30 | `selectDeptno20Or30` | 6 |
| 5 | 入职日期晚于 2001-01-01 | `selectHiredAfter2001` | 4 |
| 6 | 奖金不为 NULL | `selectWithComm` | 4 |
| 7 | 职位为销售员 | `selectSalespeople` | 3 |
| 8 | 工资在 8000 到 20000 之间 | `selectSalBetween8000And20000` | 5 |
| 9 | 奖金为 NULL 且工资小于 15000 | `selectWithoutCommAndSalBelow15000` | 3 |
| 10 | 按工资降序排列 | `selectOrderBySalDesc` | 8 |

第 2 条只查询三个字段，返回的 Emp 对象也只填充这三个属性。数据包含工资 8000、10000、15000、20000 和入职日期 2001-01-01 等边界值：`BETWEEN` 包含两端，`>` 不包含相等值，奖金 0 会被 `IS NOT NULL` 查出。

## 构建与验证

```powershell
mvn clean test
mvn package
```

测试使用 H2 的 MySQL 兼容模式，无需本机 MySQL 账号；修改数据的测试事务回滚。26 个测试覆盖 10 条 SQL 脚本及对应 Mapper 的结果、字段映射、主键回填、两套 CRUD、模糊查询、空条件、零奖金动态更新、批量操作、choose 三分支及优先级、条件构造器及分页。

打包输出 `target/mybatis-lab.jar`。这是普通 Maven JAR；运行控制台示例请使用 IDEA，使依赖自动加入类路径。

## 日志排查和审查记录

完整结论见 [实验审查报告](docs/experiment-report.md)，实测故障日志见 [fault-diagnosis.log](docs/fault-diagnosis.log)。单独运行故障演练：

```powershell
mvn -Dtest=FaultDiagnosisTest test
```

故障测试只修改内存 XML 副本，在 H2 中复现错误再验证修复。正常 Mapper 文件始终保持正确；演练日志中的预期异常不代表测试失败。
