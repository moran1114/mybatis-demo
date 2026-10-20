# 图书系统 MyBatis / MyBatis-Plus Demo

Java 11+、Maven、MyBatis 与 MyBatis-Plus 的控制台教学项目。数据模型由 `publisher`、`book`、`book_detail`、`author`、`book_author` 五张表组成，覆盖出版社与书籍一对多、书籍与详情一对一、书籍与出版社多对一，以及作者与书籍双向多对多。

## 功能入口

| 内容 | 入口 |
| --- | --- |
| 10 条基础 SQL 及对应 Mapper 查询 | [book-basic-queries.sql](sql/book-basic-queries.sql)、`BookMapper.xml`、`BookBasicQueryTest` |
| XML CRUD、主键回填、注解查询、动态条件与排序、部分更新、批量操作 | `BookXmlDemo`、`BookXmlMapperTest` |
| 四种关联查询 | `LibraryDemo` 菜单 1–5、`LibraryMapperTest` |
| BaseMapper CRUD、QueryWrapper/Lambda、OR 嵌套、投影、Map、聚合 | `LibraryDemo` 菜单 6 条件查询、菜单 7 聚合查询；`BookAdvancedQueryTest` |
| 物理分页、自动填充、字段策略、逻辑删除、乐观锁与冲突处理 | `LibraryDemo` 菜单 8–12、`BookLifecycleTest` |
| 不存在编号及菜单反馈 | `LibraryDemo` 菜单 1–5、`LibraryDemoTest` |
| 独立的错误注入与修复演练 | `BookFaultDiagnosisTest` |

## 先运行测试

测试使用内存 H2，自动加载图书表和样例数据，不需要启动 MySQL：

```powershell
mvn clean test
```

只运行某个测试类时：

```powershell
mvn "-Dtest=BookXmlMapperTest" test
```

在 IntelliJ IDEA 的 Maven 参数栏输入 `-Dtest=BookXmlMapperTest test`，不要把单引号写入参数，否则会出现 `Unknown lifecycle phase "'-Dtest=...'"`。

已配置本机 MySQL 且图书样例表存在时，可额外运行真实数据库集成验证：

```powershell
mvn "-Dtest=LocalMySqlIT" test
```

它会执行含 `USE persistence_lab` 的 10 条 SQL 并与 Mapper 对照，还会验证交互菜单、XML 操作和 MyBatis-Plus 生命周期。写入测试使用事务回滚并核对 `book` 表总行数；MySQL 的自增编号仍可能前进。该测试类不会被默认的 `mvn clean test` 自动运行。

## 运行控制台演示

在 MySQL 8 中执行 [reload_library.sql](src/main/resources/sql/reload_library.sql)。**该脚本会删除并重建图书表，也会删除旧实验表；已有数据会被清除。** 然后在未跟踪的 `src/main/resources/db.properties` 中填写本机连接信息，或设置 `LAB_DB_DRIVER`、`LAB_DB_URL`、`LAB_DB_USER`、`LAB_DB_PASSWORD` 环境变量。可参考已提交的 [db-defaults.properties](src/main/resources/db-defaults.properties)，其中密码只是占位符。

在 IDEA 运行 `com.demo2.util.BookXmlDemo.main`，可一次观看基础 SQL 与 XML CRUD 演示。运行 `com.demo2.util.LibraryDemo.main`，菜单 6、7、8 分别展示条件、聚合、分页。这三项使用预设参数，**仅作只读演示，不接收自定义查询输入**。菜单 9–12 分别解释自动填充、字段策略、逻辑删除、乐观锁。输入不存在的书籍、出版社或作者编号会显示对应表和编号；各写入演示完成后立即回滚。

默认只显示必要的控制台信息。如需在课堂上观察 SQL 和绑定参数，把 [log4j.properties](src/main/resources/log4j.properties) 中的 `log4j.logger.com.demo2.mapper=INFO` 临时改为 `DEBUG`。

## 文档

- [项目文件功能说明](docs/project-file-guide.md)
- [图书数据模型与运行说明](docs/library-demo.md)
- [8.2 条件构造器与高级查询](docs/08-02-query-wrapper.md)
- [8.3 分页、自动填充、逻辑删除与乐观锁](docs/08-03-pagination-lifecycle.md)
- [测试与故障诊断说明](docs/experiment-report.md)
