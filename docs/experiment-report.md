# MyBatis 实验审查与 AI 应用记录

审查日期：2026-09-24。业务字段以本次确认的经典员工表为准：

```text
emp(empno, ename, job, mgr, hiredate, sal, comm, deptno)
```

## 一、审查结论与范围

代码、SQL 脚本、JUnit、三类故障排查及 AI 代码审查已补齐，并通过 H2 自动化验证。尚不能认定整个现场实验全部完成：本轮未连接本机 MySQL 检查表结构或运行演示。本轮修改统一记录为一次整合提交，不补造此前各任务节点的提交记录。

按用户最新要求，Maven 阿里云镜像配置、插件登录、IDEA 设置、ApiFox 桌面操作暂不处理，也不列为本轮需补齐的代码缺项。

## 二、逐项核对

| 步骤 | 结论 | 实现或证据 |
|---|---|---|
| 1.1 数据库与员工表 | 脚本完成；本机 MySQL 待验收 | `sql/init.sql` 建库建表及 8 条样例，`sql/basic-queries.sql` 保留指定的 10 条查询；H2 执行查询并核对结果 |
| 1.2 Maven 镜像与 IDEA Maven 设置 | 按约定暂缓 | 本轮不改全局或个人 Maven 设置 |
| 1.3 Git 初始化与阶段提交 | 部分完成 | 仓库已初始化，有历史提交；用户名、邮箱已配置；本轮修改整合为一次提交，未补造各任务节点历史 |
| 1.4 AI 插件登录及配置 | 按约定暂缓 | 不操作账号与插件设置 |
| 1.5 Maven 项目与依赖 | 完成 | artifactId 为 `mybatis-lab`，Java 11 编译目标；依赖树和打包验证通过 |
| 2.1 全局配置 | 完成 | 外部连接属性、JDBC 事务、POOLED 数据源、实体别名、驼峰映射、LOG4J2、Mapper 包扫描 |
| 2.2 实体类 | 完成 | `Emp` 八字段对应数据库；金额 BigDecimal、日期 LocalDate、可空上级与奖金使用引用类型 |
| 2.3 接口与映射 | 完成 | XML CRUD、完整 resultMap、投影 resultMap、SQL 片段；补充 `@Select` 注解查询 |
| 2.4 首个程序与会话工具 | 完成 | `EmpDemo`；工具统一命名 `MyBatisUtil`，工厂复用、会话及时关闭、事务显式回滚 |
| 2.5 日志 | 完成，采用 Log4j2 | `log4j2.xml` + `LOG4J2` 输出 SQL、参数、行数；不混用 Log4j 1 的配置格式 |
| 2.6 JUnit | 完成 | 4 个测试类、26 个用例，通过 Maven Surefire 运行 |
| 3.1 if / where / trim | 完成 | 按要求改为 `selectByCondition(Emp emp)`；补充 `selectByConditionWithTrim`，复用条件片段并验证等效结果 |
| 3.2 set | 完成 | 只更新非 null 字段，测试确认零奖金能写入、未提供的字段保持原值 |
| 3.3 foreach | 完成 | 批量插入及 IN 删除，集合用 `@Param` 命名；空集合删除不影响员工 |
| 3.4 choose | 完成 | 姓名精确匹配优先、其次部门、最后全部；验证姓名分支查不到时也不回退，以及与 if 叠加的区别 |
| 4.1 MP 依赖与配置替换 | 完成 | `MybatisSqlSessionFactoryBuilder`；Mapper 通过 package 扫描，XML 位于对应的包资源路径 |
| 4.2 注解映射 | 完成 | `@TableName`、`@TableId`、`@TableField` |
| 4.3 BaseMapper CRUD | 完成 | 实测 insert / selectById / updateById / deleteById，无须手写这些通用 SQL |
| 4.4 Wrapper | 完成 | Lambda 条件构造器组合部门、工资范围及排序 |
| 4.5 分页 | 完成 | 分页拦截器、Page；测试总数、第一页、第二页、超过末页 |
| 5.1 日志错误演练 | 完成 | `FaultDiagnosisTest` 复现三类故障及额外的 @Param 错误，随后验证恢复；见下文和真实日志 |
| 5.2 ApiFox | 按约定暂缓 | 未执行桌面安装、请求保存或截图 |
| 5.3 AI 代码审查 | 完成代码审查 | 依赖、SQL 参数、映射和冗余代码检查结果见第四节；本人审阅与截图仍应基于实际操作 |

## 三、故障排查实验记录

执行 `mvn -Dtest=FaultDiagnosisTest test` 可独立复现。测试仅修改内存中的 XML 副本，使用 H2；正常项目 XML 不被破坏，写操作全部回滚。此次实际输出保存于 [fault-diagnosis.log](fault-diagnosis.log)。

### 1. 参数或实体属性拼写错误

当前字段为 `ename`，因此将 `#{ename}` 改为 `#{empname}`。日志先输出 INSERT 的预编译 SQL，随后报错，无法完成参数绑定。

实际根异常是 `ReflectionException`，信息包含 `There is no getter for property named 'empname'`。不能把讲义示意的异常名直接当作所有情况下的实际异常类型。

额外把 `#{empno}` 改为 `#{missingEmpno}`，复现真正的 `BindingException`：`Parameter 'missingEmpno' not found. Available parameters are [empno, param1]`。

修复：核对实体 getter、XML 属性名和 `@Param` 名称；恢复后插入成功、主键非 null，按编号查询返回正确员工。

### 2. resultMap 的 column 写错

把姓名列映射为 `wrong_ename`，在故障副本上设置 `autoMapping="false"` 以排除自动映射的补偿行为。SQL 仍返回一行；员工编号和职位正常，但姓名为 null。

这里必须区分 SQL 返回行与 Java 映射结果。自动映射开启时，真实的 `ename` 列有可能按同名属性重新填充姓名，不能断言“任何列名写错都一定为 null”。

修复：恢复 `column="ename"`，重新查询确认姓名为“林澈”。

### 3. 删除 useGeneratedKeys

从 INSERT 中移除 `useGeneratedKeys="true"`。实际日志为 `Updates: 1`，同一事务按姓名能查到新增员工，但 Java 对象 `empno` 为 null。

这说明数据库生成主键和 JDBC/MyBatis 把主键回填到 Java 对象是两个步骤。

修复：恢复 `useGeneratedKeys="true" keyProperty="empno" keyColumn="empno"`。测试验证插入行数为 1 且主键不为 null。

### 日志在五个方面的作用

| 作用 | 本项目中的观察方式 |
|---|---|
| 追踪 SQL | 查看 `Preparing`，确认实际分支及 WHERE / SET / LIMIT |
| 核对参数 | 查看 `Parameters` 的值、类型与 SQL 中问号的顺序 |
| 验证结果映射 | 对照 `Total` 行数、Java 属性和 JUnit 断言；有行不等于每个属性映射正确 |
| 定位异常 | 检查最底层原因，区分实体属性错误、参数名错误、映射错误与主键未回填 |
| 排查性能 | 借助 SQL 次数和分页语句查找重复查询、未分页查询；需要精确耗时或执行计划时另用计时与 EXPLAIN。本次没有伪造性能测量 |

## 四、AI 应用记录与审查结论

### 版本统一性

已运行 `mvn dependency:tree -Dverbose -DoutputFile=target/dependency-tree.txt`。解析结果：

- MyBatis-Plus 主模块、extension、core、annotation 均为 3.5.3.1；传递引入 MyBatis 3.5.10。
- JSqlParser 4.4；MySQL Connector/J 8.0.33；Log4j API/Core 均为 2.24.3。
- JUnit 4.13.2、H2 2.2.224 为 test 依赖。
- 未出现 `omitted for conflict`；Log4j API 的 `omitted for duplicate` 表示相同版本去重，并非冲突。

当前组合在 JDK 17 上以 Java 11 目标编译并通过测试。此结论不代表已验证本机 MySQL 服务端版本兼容性。保留了 MP 3.5.3.1 所需的 JDK 泛型解析器，避免纯 Java 项目因缺少 Spring 的泛型解析类而启动失败。

### SQL 与注入防护

- 10 条指定查询的表名、字段名和筛选条件均保留；脚本与 Mapper 分别执行并核对结果。
- 姓名、部门、工资和主键等值参数用 `#{}`，测试输入 `' OR 1=1 --` 不会绕过筛选。
- 仅在动态排序列使用 `${sort.column}`；`sort` 的类型限定为 `EmpSort` 枚举，列值只能为 empno、sal、hiredate。非法字符串不能转换为合法枚举。
- 全局配置中的 `${jdbc.url}` 等是配置属性替换，不是接收业务输入的 SQL 拼接。
- 批量插入须传非空集合；动态更新须给出主键及至少一个非 null 字段。动态更新的 null 表示跳过，清空可空字段使用完整更新方法。

### 映射完整性

- 完整 resultMap 覆盖全部八字段；只查姓名、工资、部门的第 2 条查询使用独立的三字段 resultMap。
- `mgr/comm` 可为 null；零奖金与 null 奖金分开测试；主键使用 AUTO、自增回填。
- 经典 emp 列名本身不含下划线，因此另用测试查询 `ename AS emp_name` 映射到 `empName`，实际验证驼峰转换生效。
- 包扫描测试能获取 Mapper 动态代理；构建产物中接口及同包 XML 均存在。

### 命名、冗余与修改记录

1. 将会话工具统一为讲义建议的 `MyBatisUtil`，同步所有调用。
2. 将条件方法统一为 `selectByCondition(Emp emp)`，删除不再使用的 `EmpQuery`。
3. 补回本次明确要求的 trim 等效写法；where 与 trim 复用 SQL 条件片段，避免复制两份规则。
4. 使用 Mapper package 扫描并移动 XML 至 `dev/course/persistence/mapper/EmpMapper.xml`。
5. 补充注解查询、受枚举限制的动态排序、注入输入测试、代理及驼峰映射验证。
6. 新增故障复现与修复验证，按实际异常类型记录；测试副本隔离错误，不保留故障生产代码。
7. Maven artifactId 与产物统一为 `mybatis-lab`。数据库仍为已约定的 `persistence_lab`，Java 包保持一致，无需为了示例包名再全量改包。

## 五、核心知识点与验证证据

执行链：读取 `mybatis-config.xml` → 构建工厂 → 开启 SqlSession → 获取 EmpMapper 动态代理 → 绑定参数执行 SQL → resultMap/自动映射 → 返回 Emp。

`SqlSessionFactory` 在同一配置下复用。此项目为了分别演示原生 MyBatis 与 MP，按两种配置各缓存一个工厂；不是每次查询重新构建。H2 测试按独立数据库构建测试工厂。`SqlSession` 不跨线程共享，业务演示使用 try-with-resources，写入需 commit，演示及测试按设计 rollback。

关于日志配置，本项目保留 Log4j2 的 `log4j2.xml`，而不是照搬 Log4j 1 的 `log4j.properties`。MyBatis 官方支持 `LOG4J2`，旧 `LOG4J` 实现已标注弃用。参见 [MyBatis 日志说明](https://mybatis.org/mybatis-3/logging.html)。

动态标签及自动映射行为核对了 [MyBatis 动态 SQL](https://mybatis.org/mybatis-3/dynamic-sql.html) 与 [全局配置说明](https://mybatis.org/mybatis-3/configuration.html)，最终以本项目运行断言为验证依据。

| 测试类 | 用例数 | 覆盖内容 |
|---|---:|---|
| EmpBasicQueryTest | 10 | 指定 SQL 脚本和对应 Mapper、比较边界、NULL、排序 |
| EmpMapperTest | 10 | XML CRUD、if/where、trim、set、foreach、choose、注解、受限排序、参数安全、代理与驼峰 |
| EmpMyBatisPlusTest | 2 | BaseMapper CRUD、Lambda 条件查询和分页 |
| FaultDiagnosisTest | 4 | 三类讲义故障，以及额外的命名参数异常；全部含恢复验证 |

本次最终执行 `mvn package`：26 个测试，0 失败，0 错误，0 跳过，BUILD SUCCESS。产物为 `target/mybatis-lab.jar`。测试日志和 XML 报告位于 `target/surefire-reports`。

## 六、剩余验收

- 在本机 MySQL 上核对新版表结构并运行 EmpDemo；初始化脚本会重建 emp，执行前应确认该表数据可重置。
- 本轮已按用户要求整理为一次整合提交；后续按真实任务节点提交，不伪造早前的分阶段轨迹。
- 如课程要求，依据真实操作补充姓名、学号、截图和个人体会；本人确认本报告中的审查结论。
- Maven 镜像、插件登录、IDEA 设置和 ApiFox 桌面步骤按用户约定暂缓。
