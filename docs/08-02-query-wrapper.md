# 8.2 条件构造器与高级查询：图书系统详细版

## 学习目标

- 掌握 `QueryWrapper` 链式拼装 WHERE 条件，以及 `and` / `or` 嵌套。
- 优先使用类型安全的 `LambdaQueryWrapper`、`LambdaUpdateWrapper`。
- 掌握 `selectCount`、`selectOne`、部分字段查询、`selectMaps`、分组聚合和 `selectByMap`。
- 理解 `@TableField(updateStrategy = FieldStrategy.NOT_NULL)` 的字段更新规则。

样例数据来自 `reload_library.sql`，主要练习表是 `book`。书籍 1、3、6 属于出版社 20；书籍 2、4、7 属于出版社 30。`discount = NULL` 表示尚未设置折扣，`discount = 0` 表示明确设置为零。可运行 `BookAdvancedQueryTest` 验证本节示例。

## 8.2.1 QueryWrapper：链式拼 WHERE

```java
List<Book> books = mapper.selectList(new QueryWrapper<Book>()
    .in("publisher_id", 20, 30)
    .between("price", new BigDecimal("80"), new BigDecimal("200"))
    .orderByDesc("price").orderByAsc("book_id"));
```

它表达 `publisher_id IN (20, 30) AND price BETWEEN 80 AND 200`。`between` 包含两端，本例按价格返回书籍 7、1、3、2。`QueryWrapper` 的列名是字符串，拼错要到运行时才会发现。

## 8.2.2 OR 嵌套

```java
mapper.selectList(new QueryWrapper<Book>()
    .eq("publisher_id", 20)
    .and(q -> q.like("title", "Java")
               .or().ge("price", new BigDecimal("200"))));
```

实际条件是 `publisher_id = 20 AND (title LIKE '%Java%' OR price >= 200)`，返回书籍 1、6。括号很关键：若在外层直接追加 `.or()`，出版社范围可能被绕过。

要表达两个完整分支的并集，可写：

```java
Wrappers.lambdaQuery(Book.class)
    .and(q -> q.eq(Book::getPublisherId, 20)
               .ge(Book::getPrice, new BigDecimal("140")))
    .or(q -> q.eq(Book::getPublisherId, 30)
              .isNotNull(Book::getDiscount));
```

## 8.2.3 LambdaQueryWrapper：类型安全

```java
Integer publisherId = null;
String title = "Java";
BigDecimal minPrice = new BigDecimal("100");
mapper.selectList(Wrappers.lambdaQuery(Book.class)
    .eq(publisherId != null, Book::getPublisherId, publisherId)
    .like(title != null && !title.isBlank(), Book::getTitle, title)
    .ge(minPrice != null, Book::getPrice, minPrice)
    .orderByAsc(Book::getBookId));
```

每个条件的第一个布尔参数决定是否加入 SQL。方法引用在 Java 字段重命名时更容易被编译器发现，比散落的列名字符串适合一般业务查询。聚合表达式如 `COUNT(*)` 仍需要使用原生 SQL 字符串。

## 8.2.4 selectCount 与 selectOne

```java
Long count = mapper.selectCount(Wrappers.lambdaQuery(Book.class)
    .eq(Book::getPublisherId, 20));
Book one = mapper.selectOne(Wrappers.lambdaQuery(Book.class)
    .eq(Book::getBookId, 1L));
```

`selectCount` 返回匹配行数。`selectOne` 适用于业务上确定最多一行的条件；无结果时为 `null`，多于一行时抛出 `TooManyResultsException`。查询出版社 20 时应使用 `selectList`，因为它有三本书。

## 8.2.5 部分字段与 selectMaps

```java
Book partial = mapper.selectOne(Wrappers.lambdaQuery(Book.class)
    .select(Book::getTitle, Book::getPrice, Book::getPublisherId)
    .eq(Book::getBookId, 1L));
List<Map<String, Object>> rows = mapper.selectMaps(new QueryWrapper<Book>()
    .select("title AS book_title", "price", "publisher_id")
    .eq("book_id", 1L));
```

部分字段查询得到的实体，其余 Java 属性是 `null`，不能误当成完整书籍去做全量更新。`selectMaps` 用列名或别名作键，适合报表与非实体结构数据。本例可用 `rows.get(0).get("book_title")` 取得书名。

## 8.2.6 分组聚合

```java
mapper.selectMaps(new QueryWrapper<Book>()
    .select("publisher_id", "COUNT(*) AS book_count", "SUM(price) AS total_price")
    .groupBy("publisher_id")
    .having("COUNT(*) >= {0}", 3)
    .orderByAsc("publisher_id"));
```

`WHERE` 在分组前筛行，`HAVING` 在分组后筛组。样例中出版社 20、30 都有三本书，价格合计分别是 465、350。`{0}` 由 MyBatis-Plus 参数化，不要把用户输入直接拼进 SQL 表达式。实体上的 `@TableLogic` 会让通用查询仅聚合未逻辑删除的书籍。

## 8.2.7 selectByMap 与条件更新

```java
Map<String, Object> columns = new LinkedHashMap<>();
columns.put("publisher_id", 20);
columns.put("discount", null);
List<Book> books = mapper.selectByMap(columns);

int changed = mapper.update(null, Wrappers.lambdaUpdate(Book.class)
    .eq(Book::getPublisherId, 30)
    .lt(Book::getPrice, new BigDecimal("100"))
    .set(Book::getDiscount, BigDecimal.ZERO));
```

`selectByMap` 的键是**数据库列名**，多项条件以 AND 连接；此处得到出版社 20 中折扣为 NULL 的书籍 1、3。`LambdaUpdateWrapper` 将符合条件的两本低价书籍折扣设为零。条件更新应检查影响行数，避免以为所有目标都已更新。

## 8.2.8 字段策略：NOT_NULL 保护

`Book.discount` 标注了 `@TableField(updateStrategy = FieldStrategy.NOT_NULL)`：

```java
Book patch = new Book();
patch.setBookId(2L);
patch.setTitle("Revised City");
patch.setDiscount(null);
mapper.updateById(patch); // 修改书名，跳过 discount

mapper.update(null, Wrappers.lambdaUpdate(Book.class)
    .eq(Book::getBookId, 2L)
    .set(Book::getDiscount, null)); // 明确写入 SQL NULL
```

这样可避免稀疏实体误清空已有折扣，但也意味着实体更新无法直接把折扣设为 NULL。显式清空时要用更新包装器；若同时需要乐观锁和自动填充，应提供带版本号的实体并检查影响行数，不能把仅有 Wrapper 的更新误当成乐观锁更新。

本节完整可运行用例在 `BookAdvancedQueryTest` 和 `LibraryMapperTest` 中。控制台 `LibraryDemo` 菜单 6 演示条件与投影，菜单 7 单独演示计数、Map 和分组聚合。若需查看 SQL 日志，可临时把 `log4j.properties` 中的 Mapper 日志级别调为 `DEBUG`。
