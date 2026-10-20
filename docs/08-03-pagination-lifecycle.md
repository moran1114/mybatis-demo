# 8.3 分页、自动填充、逻辑删除与乐观锁：图书系统详细版

## 学习目标

- 配置 `MybatisPlusInterceptor` 与 `PaginationInnerInterceptor`，执行数据库级分页。
- 用 `MetaObjectHandler` 自动维护 `createTime` / `updateTime`。
- 解释 `@TableLogic` 对通用删除和查询 SQL 的改写，以及自定义 XML 的边界。
- 解释 `@Version` 的版本校验、版本递增与冲突后的重新读取。

相关代码在 `MyBatisUtil`、`AuditMetaObjectHandler` 和 `Book` 中。运行 `BookLifecycleTest` 可逐项验证。

## 8.3.1 插件链：MybatisPlusInterceptor

`MyBatisUtil.build(...)` 用 `MybatisSqlSessionFactoryBuilder` 构建工厂，并添加两个内部插件：

```java
MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(databaseType);
pagination.setOverflow(false);
pagination.setMaxLimit(100L);
interceptor.addInnerInterceptor(pagination);
factory.getConfiguration().addInterceptor(interceptor);
```

分页插件放在链尾。`DbType.MYSQL` 用于本地演示，`DbType.H2` 用于测试。普通 MyBatis 的工厂仍可执行 XML 映射，但不安装这条 MyBatis-Plus 插件链。

## 8.3.2 物理分页：Page

```java
Page<Book> page = mapper.selectPage(Page.of(2, 2),
    Wrappers.lambdaQuery(Book.class)
        .eq(Book::getCategory, "Technology")
        .orderByAsc(Book::getBookId));
```

插件先生成计数查询，再根据数据库方言改写查询，只取当前页所需行。样例技术类书籍有三本，第 2 页（每页 2 本）只返回书籍 6；`page.getTotal()` 为 3，`page.getPages()` 为 2。`overflow=false` 让超出末页的请求得到空记录，而不是跳回首页；单页大小上限为 100。排序须稳定，示例附加主键顺序，以免分页过程中顺序不确定。

## 8.3.3 自动填充：MetaObjectHandler

`Book.createTime` 标注 `fill = INSERT`，`Book.updateTime` 标注 `fill = INSERT_UPDATE`。`AuditMetaObjectHandler.insertFill` 插入时写入同一时刻；`updateFill` 在实体更新时刷新修改时间。处理器通过 MyBatis-Plus 的 `GlobalConfig` 注册，测试可注入固定 `Clock` 验证精确时间。

`createTime` 同时设置 `updateStrategy = NEVER`，使载入实体后即使误改创建时间，`updateById` 也不会更新数据库列。仅用 `update(null, LambdaUpdateWrapper)` 写字段时没有实体提供给自动填充处理器，`updateTime` 不会自动变化；需要时应显式 `.set(Book::getUpdateTime, ...)` 或传入实体。纯 XML CRUD 也不经过这套实体自动填充，建表脚本为时间列配置了默认值，XML 更新则显式写入 `update_time`。

## 8.3.4 逻辑删除：@TableLogic

`Book.deleted` 用 `@TableLogic(value = "0", delval = "1")`。调用通用方法时，SQL 语义类似：

```sql
UPDATE book SET deleted = 1 WHERE book_id = ? AND deleted = 0;
SELECT ... FROM book WHERE book_id = ? AND deleted = 0;
```

原记录留在表内，`selectById`、`selectList`、`selectCount`、`selectByMap` 只看未删除行。自定义 XML 不会自动获得这个过滤条件，`BookMapper.xml`、`PublisherMapper.xml` 和 `AuthorMapper.xml` 的关联查询已明确写入 `deleted = 0`。教学用的 XML `deleteBookById` / `deleteBatch` 是**物理删除**，仅对没有详情或作者关联的练习记录使用；需要保留历史时应调用 `BaseMapper.deleteById`。

逻辑删除并不解除外键关系，也不自动清理 `book_detail`、`book_author`。对需要永久移除或恢复的书籍，应单独设计数据库事务与业务规则。

## 8.3.5 乐观锁：@Version

`Book.version` 用 `@Version`。对带有旧版本号的实体调用 `updateById`，插件会产生类似：

```sql
UPDATE book SET price = ?, version = 1
WHERE book_id = ? AND version = 0 AND deleted = 0;
```

命中一行表示更新成功，版本从 0 递增至 1。若另一个请求已经更新，旧版本条件无法匹配，影响行数为 0。正确处理方式是重新读取最新书籍，核对业务意图，再选择合并、重试或提示用户；不要盲目重复提交旧实体。`LibraryDemo` 菜单 12 和 `BookLifecycleTest` 演示了旧快照失败、重新读取后成功的路径。

`update(entity, wrapper)` 若实体带有版本号，可以进行版本校验并填充时间；`update(null, wrapper)` 仅按 Wrapper 条件更新，不自动递增 `version`，也不自动刷新 `updateTime`。`deleteById(id)` 本例只修改删除标记，不校验旧版本。需要带版本的删除时，应设计显式的条件更新并检查影响行数。

本节要点是区分 MyBatis-Plus 自动生成 SQL 与手写 XML 的作用范围，观察执行日志和影响行数，避免把“调用成功”误当成“数据已更新”。

控制台的菜单 8 独立展示分页结果，9 展示创建/修改时间，10 展示 `NOT_NULL` 对折扣的保护，11 对比逻辑删除后的普通查询与原始表行，12 展示版本冲突和重试。默认不输出 SQL 调试信息；需要对照生成 SQL 时，再将 `log4j.properties` 的 Mapper 级别临时改为 `DEBUG`。
