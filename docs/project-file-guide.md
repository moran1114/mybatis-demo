# MyBatis Demo 项目文件功能说明

更新日期：2026-10-20。本项目以图书系统为唯一业务示例，保留纯 MyBatis 与 MyBatis-Plus 两套教学入口。Java 11、MySQL 8 用于本地演示；自动测试使用 H2 的 MySQL 兼容模式。

## 项目结构

```text
pom.xml
README.md
sql/
  book-basic-queries.sql
src/main/java/com/demo2/
  entity/Author.java, Book.java, BookDetail.java, BookQuery.java, Publisher.java
  mapper/AuthorMapper.java, BookMapper.java, PublisherMapper.java
  handler/AuditMetaObjectHandler.java
  util/BookXmlDemo.java, LibraryDemo.java, MyBatisUtil.java
src/main/resources/
  db-defaults.properties, mybatis-config.xml, log4j.properties
  mapper/AuthorMapper.xml, BookMapper.xml, PublisherMapper.xml
  sql/reload_library.sql
src/test/java/com/demo2/
  mapper/BookBasicQueryTest.java, BookXmlMapperTest.java
  mapper/LibraryMapperTest.java, BookAdvancedQueryTest.java
  mapper/BookLifecycleTest.java, BookFaultDiagnosisTest.java
  mapper/LocalMySqlIT.java
  util/LibraryDemoTest.java
  support/EmbeddedDatabase.java
src/test/resources/
  schema-h2.sql, data-h2.sql
docs/
  library-demo.md, 08-02-query-wrapper.md, 08-03-pagination-lifecycle.md
  project-file-guide.md, experiment-report.md
```

### 根目录

| 文件 | 功能 |
| --- | --- |
| [pom.xml](../pom.xml) | 声明 Java 11、MyBatis-Plus、MySQL 驱动、Reload4j、JUnit 4、H2 和 Maven 插件。 |
| [README.md](../README.md) | 最短运行步骤、测试命令与各演示入口。 |
| [book-basic-queries.sql](../sql/book-basic-queries.sql) | 10 条从投影、条件、NULL 到排序的基础 SQL；每条都有 `BookMapper` 方法对应。 |

### Java 实体与 Mapper

| 文件 | 功能 |
| --- | --- |
| [Book.java](../src/main/java/com/demo2/entity/Book.java) | `book` 表实体。`@TableId(AUTO)`、`@TableField(updateStrategy = NOT_NULL)`、审计字段、`@TableLogic`、`@Version` 定义通用操作行为；`publisher`、`detail`、`authors` 是非表字段。 |
| [Publisher.java](../src/main/java/com/demo2/entity/Publisher.java) | 出版社及其书籍集合。 |
| [BookDetail.java](../src/main/java/com/demo2/entity/BookDetail.java) | 与书籍共享主键的详情，保证一本书至多一条详情。 |
| [Author.java](../src/main/java/com/demo2/entity/Author.java) | 作者及其书籍集合。 |
| [BookQuery.java](../src/main/java/com/demo2/entity/BookQuery.java) | 纯 MyBatis 动态查询条件；`SortColumn` 枚举是动态排序的列名白名单。 |
| [BookMapper.java](../src/main/java/com/demo2/mapper/BookMapper.java) | 同时声明 XML/注解方法并继承 `BaseMapper<Book>`。包含 10 条查询、CRUD、动态 SQL、批量操作和书籍侧关联查询。 |
| [PublisherMapper.java](../src/main/java/com/demo2/mapper/PublisherMapper.java) | 出版社到书籍的一对多查询。 |
| [AuthorMapper.java](../src/main/java/com/demo2/mapper/AuthorMapper.java) | 作者到书籍的多对多查询。 |

### 配置、XML 与建表数据

| 文件 | 功能 |
| --- | --- |
| [mybatis-config.xml](../src/main/resources/mybatis-config.xml) | 数据源、驼峰映射、SQL 日志及三个 Mapper XML 的注册。 |
| [db-defaults.properties](../src/main/resources/db-defaults.properties) | 可提交的连接占位配置，使 H2 测试在新检出的工程中也能构建。实际 MySQL 凭据放在被 Git 忽略的 `db.properties`，或使用环境变量。 |
| [log4j.properties](../src/main/resources/log4j.properties) | 生效的控制台日志配置。默认关闭 Mapper 的 DEBUG SQL 输出；教学观察 SQL 时将其级别从 INFO 改为 DEBUG。 |
| [BookMapper.xml](../src/main/resources/mapper/BookMapper.xml) | 10 条基础查询、结果映射、主键回填、CRUD、`where/trim/set/foreach/choose`、书籍到详情/出版社/作者的关联查询。 |
| [PublisherMapper.xml](../src/main/resources/mapper/PublisherMapper.xml) | 使用 `LEFT JOIN` 与 `<collection>` 映射出版社下的书籍。 |
| [AuthorMapper.xml](../src/main/resources/mapper/AuthorMapper.xml) | 通过 `book_author` 映射作者下的书籍。 |
| [reload_library.sql](../src/main/resources/sql/reload_library.sql) | 在 MySQL 中重建图书数据模型及样例；同时清理旧实验表。脚本会删除原有数据。 |

### 运行入口与测试

| 文件 | 功能 |
| --- | --- |
| [MyBatisUtil.java](../src/main/java/com/demo2/util/MyBatisUtil.java) | 构建纯 MyBatis 和 MyBatis-Plus `SqlSessionFactory`；后者配置乐观锁、最大 100 条的物理分页及自动填充。 |
| [AuditMetaObjectHandler.java](../src/main/java/com/demo2/handler/AuditMetaObjectHandler.java) | 插入时写入创建/修改时间，更新实体时刷新修改时间；可注入固定时钟供测试使用。 |
| [BookXmlDemo.java](../src/main/java/com/demo2/util/BookXmlDemo.java) | 顺序演示 10 条查询、动态 SQL、XML CRUD 与批量操作，演示结束回滚。 |
| [LibraryDemo.java](../src/main/java/com/demo2/util/LibraryDemo.java) | 交互式菜单：关联查询、独立条件/聚合/分页查询、四项生命周期演示与不存在编号反馈。 |
| [EmbeddedDatabase.java](../src/test/java/com/demo2/support/EmbeddedDatabase.java) | 创建独立内存数据库、执行 H2 建表和样例数据脚本。 |
| [schema-h2.sql](../src/test/resources/schema-h2.sql)、[data-h2.sql](../src/test/resources/data-h2.sql) | 测试用的五张图书表和与 MySQL 一致的样例。 |
| `BookBasicQueryTest` | 10 组参数化用例，逐条比较独立 SQL 文件与 Mapper 结果。 |
| `BookXmlMapperTest` | 验证主键回填、XML CRUD、动态 SQL、安全排序、部分更新、批量操作及控制台示例。 |
| `LibraryMapperTest` | 验证一对多、一对一、多对一、双向多对多和空关联。 |
| `BookAdvancedQueryTest` | 验证 QueryWrapper/Lambda、OR 嵌套、计数、单条、投影、Map、聚合与字段策略。 |
| `BookLifecycleTest` | 验证分页、自动填充、逻辑删除、乐观锁冲突和更新包装器边界。 |
| `LibraryDemoTest` | 验证菜单拆分、中文机制解释、不存在编号提示及每项演示事务回滚。 |
| `BookFaultDiagnosisTest` | 独立复制并故意破坏 Mapper XML，复现属性名、参数名、结果列和主键回填故障，再验证正确配置。 |
| `LocalMySqlIT` | 显式运行的本机 MySQL 集成测试：检查五张表、直接执行 10 条 SQL、对照 Mapper，并以回滚事务验证 XML、菜单和 MyBatis-Plus。 |

## 教学步骤对照

| 要求 | 对应实现 |
| --- | --- |
| 全局配置、日志、JUnit | `mybatis-config.xml`、`log4j.properties`、`src/test/java` 下的 JUnit 4 用例 |
| 3.1 多条件模糊查询（`if/where`） | `BookMapper.xml` 的 `bookConditions` 与 `selectByCondition`；`BookXmlDemo` 按书名、分类、出版社、最低价格查询 |
| 3.2 动态更新（`set`） | `BookMapper.xml#updatePartial` |
| 3.3 批量插入/删除（`foreach`） | `BookMapper.xml#insertBatch`、`deleteBatch`，集合参数显式 `@Param` |
| 3.4 分支选择（`choose/when/otherwise`） | `BookMapper.xml#selectPreferred`、`selectSorted` 与空集合保护 |
| 4.1–4.3 引入 MyBatis-Plus、实体映射、通用 CRUD | `pom.xml`、`MyBatisUtil.build`、`Book` 注解、`BookMapper extends BaseMapper<Book>` |
| 4.4 条件构造器与高级查询 | `LibraryDemo` 菜单 6、7；`BookAdvancedQueryTest` |
| 4.5 分页插件与分页查询 | `MyBatisUtil` 的 `PaginationInnerInterceptor`、`LibraryDemo` 菜单 8 |
| 时间填充、字段策略、逻辑删除、乐观锁 | `LibraryDemo` 菜单 9–12；`BookLifecycleTest` |

## 从入口到 SQL

纯 MyBatis 路径：`BookXmlDemo` → `MyBatisUtil.openMyBatisSession()` → `BookMapper` → `BookMapper.xml` → `book` 表。MyBatis-Plus 路径：`LibraryDemo` → `MyBatisUtil.openSession()` → `BookMapper` 的 `BaseMapper` 方法 → 插件链 → SQL。关联查询仍由三个 XML 映射控制，因此 XML 查询需显式过滤 `deleted = 0`。

## 执行顺序

1. 运行 `mvn clean test`，默认测试均用 H2，不需要 MySQL。
2. 若要连接 MySQL，先检查备份，再执行 `reload_library.sql`；脚本会重建图书表并删除旧实验表。
3. 在本地配置 `db.properties`，然后运行 `BookXmlDemo` 或 `LibraryDemo`。
4. 若已建立本机图书样例库，运行 `mvn "-Dtest=LocalMySqlIT" test`，核对真实 MySQL 的 SQL、Mapper 和交互菜单；测试写入均回滚。

更具体的表关系见 [图书系统说明](library-demo.md)，条件构造器见 [8.2](08-02-query-wrapper.md)，生命周期机制见 [8.3](08-03-pagination-lifecycle.md)。
