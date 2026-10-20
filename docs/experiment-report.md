# 图书系统实验与故障诊断记录

## 验证范围

运行 `mvn clean test` 时，Maven 先删除旧编译输出，再编译图书系统代码并用 H2 建表、装入样例数据。测试使用独立的内存数据库名，各测试方法在事务中运行并回滚，不依赖本机 MySQL 数据。

| 测试类 | 重点 |
| --- | --- |
| `BookBasicQueryTest` | 10 条独立 SQL 与 10 个 Mapper 方法逐一对照结果及投影列数。 |
| `BookXmlMapperTest` | XML CRUD、生成主键回填、动态 WHERE/TRIM、注解查询、排序白名单、`set` 部分更新、`foreach` 批量操作和控制台示例。 |
| `LibraryMapperTest` | 出版社→书籍、书籍→详情、书籍→出版社、书籍↔作者；含不存在实体及空关联。 |
| `BookAdvancedQueryTest` | QueryWrapper/Lambda、OR 嵌套、可选条件、单条/计数、投影、Map、HAVING 聚合、条件更新及 NOT_NULL。 |
| `BookLifecycleTest` | Page 物理分页与大小上限、时间自动填充、逻辑删除、旧版本冲突、重新读取、仅 Wrapper 更新边界。 |
| `LibraryDemoTest` | 交互菜单拆分、输入不存在编号的中文反馈、四项机制的输出与逐项回滚。 |
| `BookFaultDiagnosisTest` | 独立注入四种 Mapper 错误，检查异常或错误映射，并用原始映射再次验证修复。 |
| `LocalMySqlIT` | 显式运行的本机 MySQL 集成验证：五张表、含 `USE` 的 10 条独立 SQL、XML 操作、菜单反馈和生命周期；写入回滚。 |

## 故障诊断演练

`BookFaultDiagnosisTest` 在内存中复制 `BookMapper.xml`，**不修改工作区真实文件**，所以每个故障都可独立复现：

1. 把插入参数 `#{title}` 错写成不存在的 `#{missingTitle}`，观察 `ReflectionException`；修复为 Java 实体真实属性名。
2. 把命名参数 `#{bookId}` 错写成 `#{missingBookId}`，观察 `BindingException`；检查 Mapper 的 `@Param("bookId")` 与 XML 一致。
3. 把结果映射 `title` 的列名改成不存在的 `wrong_title`，关闭自动映射后看到 `Book.title == null`；恢复真实列名或 SQL 别名。
4. 移除 `useGeneratedKeys="true"`，记录已经写入但 Java 对象的 `bookId` 仍是 `null`；恢复 `useGeneratedKeys`、`keyProperty` 和 `keyColumn`。

每个用例在复现后重新加载正确 XML，确认正常读取或主键回填。默认日志级别由 `log4j.properties` 设为 INFO；需要对照实际 SQL 与绑定参数时，可把 Mapper 级别临时改为 DEBUG。

## 验证结果与边界

2026-10-20 运行 `mvn clean test`，45 个 H2 用例全部通过，失败与错误均为 0。另运行 `mvn "-Dtest=LocalMySqlIT" test`，5 个本机 MySQL 用例全部通过：五张图书表存在，当前有 8 本未删除书籍；10 条基础 SQL 连同 `USE persistence_lab` 在 MySQL 执行并与 Mapper 结果一致；XML CRUD、批量操作、中文菜单、自动填充、字段策略、逻辑删除和乐观锁均通过。测试写入事务已回滚，`book` 表总行数在测试前后相同；MySQL 自增编号可能前进。没有运行会重建表的 `reload_library.sql`。
