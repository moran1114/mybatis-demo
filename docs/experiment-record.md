# 实验一实施记录

本文件只记录能够由源码、数据库检查和自动化测试证明的内容。姓名、学号、实验日期、截图与个人体会需要由实验执行者补充。

## 一、实验环境

- 操作系统：Windows 11
- 本次验证 JDK：17.0.12；Maven 编译目标：Java 11
- Maven：3.9.11
- MyBatis-Plus：3.5.3.1（内含 MyBatis 核心）
- MySQL 驱动：8.0.33
- 日志：Log4j2 2.24.3
- 单元测试：JUnit 4.13.2
- 自动化测试数据库：H2 2.2.224，MySQL 兼容模式
- 本机数据库服务：MySQL 9.5.0

## 二、已完成内容

1. 使用 `SqlSessionFactory` 单例、短生命周期 `SqlSession` 和 Mapper 动态代理建立独立 MyBatis 运行环境。
2. 使用实体注解和 XML `resultMap` 映射 `staff_profile` 的 8 个字段。
3. 完成查询全部、按主键查询、新增、完整修改、按主键删除和自增主键回填。
4. 完成 `if/where` 多条件查询，并用 `trim` 实现等价查询。
5. 完成 `set` 动态更新、`foreach` 批量插入/删除、`choose` 优先级分支查询。
6. Mapper 继承 `BaseMapper<StaffProfile>`，完成 MyBatis-Plus 通用 CRUD。
7. 完成 Lambda 条件构造器的等值、范围、排序查询以及分页插件查询。
8. Log4j2 输出准备 SQL、绑定参数、更新行数和结果行数。
9. JUnit 使用独立内存库验证全部数据库功能，每个测试后回滚。

## 三、数据库变更

- 已删除旧项目唯一业务表 `mybatisdb.user`；复查 `information_schema.tables` 的结果为 0。
- 新实验数据库为 `persistence_lab`，实验表为 `staff_profile`。
- 新表只保留实验一所需的主键、姓名、性别、业务单元、岗位、薪资、入职日期和在职状态。
- 初始化脚本中的表名、列名和 6 条样例数据均重新设计。

## 四、真实问题与解决过程

### 问题 1：纯 Java 环境启动 MyBatis-Plus 时缺少 Spring 类

现象：第一次运行测试时，`MybatisSqlSessionFactoryBuilder` 抛出 `NoClassDefFoundError: org/springframework/core/GenericTypeResolver`。

定位：指导书指定的 MyBatis-Plus 3.5.3.1 默认泛型解析路径依赖 Spring Core；但本项目已经按“仅实验一”要求移除所有 Spring 依赖。

处理：使用 MyBatis-Plus 公开的 `GenericTypeUtils.setGenericTypeResolver` 扩展点，加入纯 JDK 反射解析器。这样保留 `BaseMapper` 能力，同时不重新引入 Spring/SSM。

### 问题 2：初版泛型解析丢失实体类型

现象：异常消失后，生成 SQL 变为 `INSERT INTO object VALUES`，Lambda 条件构造器也提示找不到属性缓存。

定位：`StaffProfileMapper -> BaseMapper<StaffProfile> -> Mapper<T>` 有两层泛型继承。初版解析器只读取当前层，把下一层的 `T` 错误解析成 `Object`。

处理：在递归遍历接口层级时保留 `TypeVariable -> 实际类型` 映射。修复后表信息正确解析为 `StaffProfile`，BaseMapper CRUD 和分页测试全部通过。

### 问题 3：Windows 默认字符集可能破坏中文测试数据

风险：直接使用平台默认字符集读取 SQL 脚本时，中文可能以错误字节写入数据库，导致 SQL 参数看似正确但条件查询返回 0 行。

处理：内存数据库脚本统一通过 `InputStreamReader(..., UTF_8)` 读取；Maven 测试进程也显式设置 `-Dfile.encoding=UTF-8`。

## 五、日志排错演练记录

指导书要求的三类故障不应永久留在可运行代码中，建议在个人分支临时修改并截图，完成后立即恢复：

| 临时故障 | 预期现象 | 定位与恢复 |
|---|---|---|
| 把 `#{name}` 改为不存在的 `#{namex}` | 参数取值异常或 SQL 绑定失败 | 对照实体 getter 与 XML 占位符，恢复为 `name` |
| 把 `resultMap` 的 `full_name` 写错 | `name` 映射为 `null` 或数据库报告未知列 | 对照 `SHOW CREATE TABLE`，恢复真实列名 |
| 删除 `useGeneratedKeys/keyProperty` | 插入成功但实体 `id` 仍为 `null` | 恢复主键回填配置并断言 `id != null` |

当前正确版本由 `StaffProfileMapperTest` 的主键回填和字段断言保护。

## 六、AI 应用与人工审查

AI 用于重新读取实验指导书、拆分实验一任务、生成初稿和分析测试异常。人工审查项目包括：

- 用 `mvn dependency:tree` 核对只保留实验一依赖；
- 核对表名、列名、`resultMap` 和实体注解一一对应；
- 全部值参数使用 `#{}`，项目中没有接收输入的 `${}` SQL 拼接；
- 用 JUnit 断言 CRUD 行数、主键回填、动态条件、批量数量和分页总数；
- 用数据库元数据确认旧表确实删除；
- 执行干净构建，排除 `target` 残留造成的假成功。

## 七、思考题参考答案

1. `#{}` 会生成 PreparedStatement 占位符，值与 SQL 结构分离，能正确处理类型并抵御注入；`${}` 是原样文本替换，只适合经过白名单限定的列名等结构片段，不能直接接收用户输入。
2. `SqlSessionFactory` 创建成本高且线程安全，应用中通常只建立一个；`SqlSession` 非线程安全，应按一次工作单元创建并及时关闭；Mapper 是绑定到当前 Session 的动态代理，生命周期不能超过对应 Session。
3. N+1 指先查一批主记录，再为每条记录分别查询关联数据。例如先查 20 名人员，再逐人查询部门详情，会执行 21 次 SQL。可通过联表查询、批量 `IN` 查询或结果映射的一次性加载解决。
4. XML 动态 SQL 适合复杂查询、批量语句、数据库特有语法和需要集中维护 SQL 的场景；Wrapper 适合单表常见筛选，类型安全的 Lambda 写法能减少列名拼写错误。两者可以在同一 Mapper 共存。
5. AI 可能生成错误版本组合、过时配置、不存在的字段、错误泛型、未参数化 SQL 或冗余架构。应通过官方 API、依赖树、数据库 DDL、编译器、SQL 日志、边界测试和代码审查逐项验证。

## 八、自动化验证

执行：

```text
mvn clean test
```

最近验证结果：5 个测试，0 失败，0 错误，0 跳过。

## 九、本人仍需完成的现场材料

- [ ] 填写姓名、学号、班级和实验日期
- [ ] 截取 JDK、Maven、IDEA、MySQL 环境信息
- [ ] 截取新数据库表结构和 6 条数据
- [ ] 截取 MyBatis SQL、参数、更新/查询行数日志
- [ ] 截取 JUnit 绿色测试结果
- [ ] 按指导书临时制造三类故障并记录真实截图
- [ ] 在 ApiFox 保存一个 GET 基础请求并截图
- [ ] 按任务节点完成自己的 Git 提交
- [ ] 写入自己的提示词摘要、人工修改过程和实验体会
