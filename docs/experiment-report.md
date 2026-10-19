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
| `BookFaultDiagnosisTest` | 独立注入四种 Mapper 错误，检查异常或错误映射，并用原始映射再次验证修复。 |

## 故障诊断演练

`BookFaultDiagnosisTest` 在内存中复制 `BookMapper.xml`，**不修改工作区真实文件**，所以每个故障都可独立复现：

1. 把插入参数 `#{title}` 错写成不存在的 `#{missingTitle}`，观察 `ReflectionException`；修复为 Java 实体真实属性名。
2. 把命名参数 `#{bookId}` 错写成 `#{missingBookId}`，观察 `BindingException`；检查 Mapper 的 `@Param("bookId")` 与 XML 一致。
3. 把结果映射 `title` 的列名改成不存在的 `wrong_title`，关闭自动映射后看到 `Book.title == null`；恢复真实列名或 SQL 别名。
4. 移除 `useGeneratedKeys="true"`，记录已经写入但 Java 对象的 `bookId` 仍是 `null`；恢复 `useGeneratedKeys`、`keyProperty` 和 `keyColumn`。

每个用例在复现后重新加载正确 XML，确认正常读取或主键回填。SQL 日志由 `log4j2.xml` 输出，可对照实际绑定参数与返回行数。

## 验证结果与边界

最后一次完整构建命令为 `mvn clean test`，共 42 个用例，通过 42 个，失败与错误均为 0。此验证覆盖 H2 MySQL 兼容模式；`reload_library.sql` 是给本地 MySQL 8 的初始化脚本，运行它会删除现有图书与旧实验表，执行前需检查数据并备份。这里的自动测试未对真实 MySQL 执行该脚本。
