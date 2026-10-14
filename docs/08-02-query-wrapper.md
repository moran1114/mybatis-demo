# 第二节：条件构造器与高级查询 - 详细版

本节对应 **8.2.1—8.2.8**，使用项目现有的 `Emp`、`EmpMapper` 和 `emp` 表。环境为 Java 11+、MyBatis-Plus **3.5.3.1**，无需 Spring Boot。

完整可运行示例：[MyBatisPlusQueryDemo.java](../src/main/java/com/demo2/util/MyBatisPlusQueryDemo.java)。结果与边界验证：[EmpAdvancedQueryTest.java](../src/test/java/com/demo2/mapper/EmpAdvancedQueryTest.java)。文中的结果基于项目初始化脚本的 8 条员工数据；各片段按初始数据独立说明。

## 一、学习目标

- 掌握 `QueryWrapper` 的链式条件拼装与 OR 嵌套。
- 优先使用类型安全的 `LambdaQueryWrapper` / `LambdaUpdateWrapper`。
- 掌握 `selectCount`、`selectOne`、只查部分字段的 `select()`、`selectMaps`、分组聚合与 `selectByMap`。
- 理解 `@TableField(updateStrategy = FieldStrategy.NOT_NULL)` 对实体更新的作用及边界。

## 二、核心知识点

### 8.2.1 QueryWrapper：链式拼 WHERE

`QueryWrapper<Emp>` 描述查询条件，`mapper.selectList(wrapper)` 才执行查询。普通 Wrapper 的字符串参数使用**数据库列名**。

```java
QueryWrapper<Emp> wrapper = new QueryWrapper<Emp>()
        .eq("deptno", 20)
        .ge("sal", new BigDecimal("10000"))
        .orderByDesc("sal")
        .orderByAsc("empno");
List<Emp> employees = mapper.selectList(wrapper);
```

SQL 结构如下，实际日志会列出实体的所有表字段：

```sql
SELECT * FROM emp
WHERE deptno = ? AND sal >= ?
ORDER BY sal DESC, empno ASC;
-- 参数：20、10000
```

结果依次为 `Shen Zhi(22000)`、`Lin Han(14500)`、`Xu Shu(10000)`。连续添加的普通条件默认以 `AND` 连接；第二个排序字段让工资相同时的顺序确定。

| 方法 | SQL 含义 | 本项目示例 |
| --- | --- | --- |
| `eq` / `ne` | `=` / `<>` | `.eq("deptno", 20)` |
| `gt` / `ge` | `>` / `>=` | `.ge("sal", new BigDecimal("10000"))` |
| `lt` / `le` | `<` / `<=` | `.lt("sal", new BigDecimal("15000"))` |
| `between` | 闭区间，两端均包含 | `.between("sal", 8000, 20000)` |
| `like` | 包含匹配，自动在两端加 `%` | `.like("ename", "Lin")` |
| `in` | 集合中的任一值 | `.in("deptno", 20, 30)` |
| `isNull` / `isNotNull` | 判断 SQL NULL | `.isNull("comm")` |
| `orderByAsc` / `orderByDesc` | 升序 / 降序 | `.orderByDesc("sal")` |

金额代码优先使用 `new BigDecimal("10000")`；不使用浮点数构造金额。判断空值应写 `isNull`，不能用 `eq("comm", null)` 代替。`comm = 0` 与 `comm IS NULL` 是不同含义。

Wrapper 会绑定条件值，但列名和 SQL 表达式仍由程序控制。普通列名、聚合表达式不能直接接收用户输入。各 Wrapper API 的定义见 [官方条件构造器文档](https://baomidou.com/guides/wrapper/)。

### 8.2.2 OR 嵌套

需求：**部门为 20，并且满足“姓名包含 Lin 或工资至少 20000”**。

```java
QueryWrapper<Emp> wrapper = new QueryWrapper<Emp>()
        .eq("deptno", 20)
        .and(w -> w.like("ename", "Lin")
                .or()
                .ge("sal", new BigDecimal("20000")))
        .orderByAsc("empno");
List<Emp> employees = mapper.selectList(wrapper);
```

```sql
WHERE deptno = 20 AND (ename LIKE '%Lin%' OR sal >= 20000)
-- 为便于阅读展开了参数；实际执行使用参数绑定。
```

结果是编号 `1、6`。括号限定了 OR 的范围，部门 30 的 `Zhao Ning(20000)` 不会混入结果。

下面的写法会得到不同的业务含义：

```java
new QueryWrapper<Emp>()
        .eq("deptno", 20).like("ename", "Lin")
        .or().ge("sal", new BigDecimal("20000"));
// (deptno = 20 AND ename LIKE '%Lin%') OR sal >= 20000
// 结果包含编号 7，因为 SQL 中 AND 优先于 OR。
```

如果需求是两组条件择一，使用带消费者参数的 `or`：

```java
LambdaQueryWrapper<Emp> wrapper = Wrappers.lambdaQuery(Emp.class)
        .and(w -> w.eq(Emp::getDeptno, 20)
                .ge(Emp::getSal, new BigDecimal("14000")))
        .or(w -> w.eq(Emp::getDeptno, 30)
                .isNotNull(Emp::getComm));
// (deptno = 20 AND sal >= 14000)
// OR (deptno = 30 AND comm IS NOT NULL)
```

记忆方法：`and(w -> ...)` 表示 `AND (...)`，`or(w -> ...)` 表示 `OR (...)`；无参 `or()` 只切换下一个连接符，不会替你把前面的条件整体括起来。

### 8.2.3 LambdaQueryWrapper：推荐，类型安全

Lambda 使用实体 getter 引用。属性重命名时 IDE 可以同步修改引用，方法名错误也会在编译阶段暴露；字段到列名的映射来自实体注解。

```java
Integer deptno = 20;
String name = "Lin";
BigDecimal minSal = new BigDecimal("10000");

LambdaQueryWrapper<Emp> wrapper = Wrappers.lambdaQuery(Emp.class)
        .eq(deptno != null, Emp::getDeptno, deptno)
        .like(name != null && !name.isBlank(), Emp::getEname, name)
        .ge(minSal != null, Emp::getSal, minSal)
        .orderByAsc(Emp::getEmpno);
List<Emp> employees = mapper.selectList(wrapper);
```

三个筛选条件均存在时，返回 `Lin Han`。首个布尔参数为 `false` 时，该条件不会加入 SQL。若所有条件均省略，查询范围就是全表；分页或必填筛选条件应由调用方决定。

“类型安全”主要指**字段引用可由编译器检查**，不代表值域、权限、业务合法性也会自动校验。例如负工资、无效部门仍需要业务验证。

每次操作新建 Wrapper；Wrapper 是可变对象，不要保存成共享静态变量，也不要跨线程复用。查询和更新分别使用 `LambdaQueryWrapper` 与 `LambdaUpdateWrapper`。

### 8.2.4 selectCount 与 selectOne

```java
Long count = mapper.selectCount(
        Wrappers.lambdaQuery(Emp.class).eq(Emp::getDeptno, 20));
// 3，对应 SELECT COUNT(*) FROM emp WHERE deptno = 20。

Emp employee = mapper.selectOne(
        Wrappers.lambdaQuery(Emp.class).eq(Emp::getEmpno, 1L));
// Lin Han。

Emp absent = mapper.selectOne(
        Wrappers.lambdaQuery(Emp.class).eq(Emp::getEmpno, -1L));
// null。
```

| 匹配行数 | `selectOne` 在本项目版本中的行为 |
| --- | --- |
| 0 | 返回 `null` |
| 1 | 返回该实体 |
| 大于 1 | 抛出 `TooManyResultsException` |

`selectOne` 表达“业务预期至多一条”，不会自动把任意查询限制成一条。按部门 20 查询会匹配 3 行并抛异常。应使用主键、唯一键或足够明确的条件；若要“排序后的第一条”，使用明确排序的分页查询。不要通过随意添加 `LIMIT 1` 隐藏本应唯一的数据问题。

`selectCount` 用于计数，不需要先取出全部实体再调用 `size()`。分组统计则使用下一小节介绍的 `selectMaps`，不要把多组结果当作单个总数。

### 8.2.5 只查部分字段与 selectMaps

**只查部分字段，仍返回实体：**

```java
List<Emp> summaries = mapper.selectList(Wrappers.lambdaQuery(Emp.class)
        .select(Emp::getEname, Emp::getSal, Emp::getDeptno)
        .orderByAsc(Emp::getEmpno));
```

```sql
SELECT ename, sal, deptno FROM emp ORDER BY empno ASC;
```

这不是查出整行再丢弃字段，数据库实际只返回三列。`empno`、`job` 等未查询的属性为 `null`，并不说明数据库中的值为 NULL。若后续需要定位实体，应把主键一起选出来；用于展示的投影实体不宜直接当完整实体更新。

**返回 Map，保留列名或别名：**

```java
List<Map<String, Object>> rows = mapper.selectMaps(new QueryWrapper<Emp>()
        .select("ename AS employee_name", "sal", "deptno")
        .eq("deptno", 20)
        .orderByAsc("empno"));
Object name = rows.get(0).get("employee_name");
```

Map 适合临时投影和统计结果。实体查询的 `select(...)` 和返回类型不同的 `selectMaps(...)` 可以独立选择，并不是同一个概念。稳定的业务接口也可以为统计结果定义 DTO，例如本项目已有的 `DeptPayrollSummary`。

别名大小写可能受 JDBC 驱动影响，本项目 H2 配置使用小写标识符。计数等数值优先转成 `Number` 再取 `longValue()`，避免假设所有驱动都返回同一种数值类。默认配置下，SQL NULL 对应的 Map 键也可能被省略。

### 8.2.6 分组聚合

需求：按部门统计人数、工资总额和平均工资，只保留至少 3 人的部门。

```java
List<Map<String, Object>> statistics = mapper.selectMaps(new QueryWrapper<Emp>()
        .select("deptno", "COUNT(*) AS emp_count",
                "SUM(sal) AS total_sal", "AVG(sal) AS avg_sal")
        .groupBy("deptno")
        .having("COUNT(*) >= {0}", 3)
        .orderByAsc("deptno"));
```

```sql
SELECT deptno, COUNT(*) AS emp_count,
       SUM(sal) AS total_sal, AVG(sal) AS avg_sal
FROM emp
GROUP BY deptno
HAVING COUNT(*) >= ?
ORDER BY deptno ASC;
```

| deptno | emp_count | total_sal | avg_sal |
| --- | --- | --- | --- |
| 20 | 3 | 46500.00 | 15500.00 |
| 30 | 3 | 35000.00 | 约 11666.67，展示时再按需要舍入 |

`WHERE` 在分组前筛选员工行，`HAVING` 在分组后筛选统计结果。选出的非聚合列应出现在 `GROUP BY` 中，不要写 `select("*").groupBy("deptno")`。

`having` 中的 `{0}` 会把值参数化；聚合表达式由程序固定提供。不能把用户字符串直接连接成 `"COUNT(*) >= " + input`。`COUNT(*)` 统计行数，而 `COUNT(comm)` 只统计奖金不为 NULL 的行。

### 8.2.7 selectByMap 与条件更新

**多个等值条件：**

```java
Map<String, Object> columns = new LinkedHashMap<>();
columns.put("deptno", 20);
columns.put("comm", null);
List<Emp> employees = mapper.selectByMap(columns);
```

```sql
SELECT * FROM emp WHERE deptno = 20 AND comm IS NULL;
```

匹配编号 `1、3`，没有显式排序时不依赖返回顺序。Map 的键是数据库列名，多项之间用 `AND` 连接；本项目版本对 Map 中的 null 值生成 `IS NULL`。不要用不允许 null 的 `Map.of(...)` 创建这个例子。

`selectByMap` 适合简单等值筛选。需要区间、模糊查询、OR、分组或排序时使用 Wrapper。空 Map 没有业务筛选条件；Map 的键也不能直接来自未经校验的外部输入。

**LambdaUpdateWrapper：把符合条件的员工奖金设为 0。**

```java
LambdaUpdateWrapper<Emp> update = Wrappers.lambdaUpdate(Emp.class)
        .eq(Emp::getDeptno, 30)
        .lt(Emp::getSal, new BigDecimal("10000"))
        .set(Emp::getComm, BigDecimal.ZERO);
int affected = mapper.update(null, update);
```

```sql
UPDATE emp SET comm = 0 WHERE deptno = 30 AND sal < 10000;
```

初始化数据下匹配编号 `2、4`。H2 测试返回影响行数 2；实际 MySQL 对“值原本相同”的行如何计数还受连接配置影响。

也可以把 SET 值放进实体，Wrapper 只负责 WHERE：

```java
Emp patch = new Emp();
patch.setJob("Senior Salesperson");
int affected = mapper.update(patch,
        Wrappers.lambdaUpdate(Emp.class).eq(Emp::getEmpno, 2L));
```

更新前明确限定范围。空 Wrapper 可能更新全表，`NOT_NULL` 不会防止这种情况。同一个字段不要同时在实体和 `.set(...)` 中赋值，否则可能生成重复的 SET 项。第三节涉及自动填充和乐观锁时，还需要区分有实体与 `entity = null` 的更新方式。

### 8.2.8 字段策略：NOT_NULL 保护

项目在 [Emp.java](../src/main/java/com/demo2/entity/Emp.java) 的奖金字段上显式声明：

```java
@TableField(value = "comm", updateStrategy = FieldStrategy.NOT_NULL)
private BigDecimal comm;
```

执行实体更新时，MyBatis-Plus 根据字段策略决定是否把属性加入 `SET`：

```java
Emp patch = new Emp();
patch.setEmpno(2L);
patch.setJob("Senior Salesperson");
patch.setComm(null);
mapper.updateById(patch);
// 更新 job，SQL 的 SET 中没有 comm；原奖金 1500 得以保留。
```

| 赋值方式 | `comm` 的效果 |
| --- | --- |
| 实体 `setComm(null)`，策略为 NOT_NULL | 不加入 SET，保留原奖金 |
| 实体 `setComm(BigDecimal.ZERO)` | 写入 0，0 并非 null |
| Wrapper `.set(Emp::getComm, null)` | 显式生成 `comm = NULL`，清空奖金 |

业务确认需要清空奖金时，可以精确指定：

```java
mapper.update(null, Wrappers.lambdaUpdate(Emp.class)
        .eq(Emp::getEmpno, 2L)
        .set(Emp::getComm, null));
```

字段策略控制的是 MyBatis-Plus **从实体自动生成 SET** 的过程。它不改变数据库约束，不会重写自定义 XML 中的 SET，也不会阻止 Wrapper 显式指定 null；字符串的空串同样不等于 null。它能避免“未提供值被写成 NULL”，不能解决并发覆盖，后者见第三节的乐观锁。策略定义可对照 [官方注解配置](https://baomidou.com/reference/annotation/)。

## 三、运行与练习

在 IDEA 中运行 `com.demo2.util.MyBatisPlusQueryDemo`。它依次输出八个知识点，SQL 由 Log4j2 输出；整个事务最后回滚。该入口使用 `db.properties` 中的本机连接，需要已有 `emp` 表。

无需 MySQL 也可以执行本章 H2 测试。在 PowerShell / CMD 终端中执行：

```powershell
mvn -Dtest=EmpAdvancedQueryTest test
```

在 IDEA 的 Maven 运行配置参数栏中填写：

```text
-Dtest=EmpAdvancedQueryTest test
```

IDEA 会调用 Maven，因此参数栏不需要填写 `mvn`。不要在 `-Dtest=EmpAdvancedQueryTest` 两侧添加单引号，否则单引号可能原样传给 Maven，导致 `Unknown lifecycle phase`，测试尚未开始就会失败。

建议练习：

1. 查询部门 20 或 30、工资在 8000 到 20000 之间的员工，按工资降序；检查两端值均被包含。
2. 把 8.2.2 的括号去掉，观察编号 7 为什么会出现，再恢复正确括号。
3. 只查询员工编号和姓名，观察其他属性，并解释它们的 null 与数据库 NULL 的区别。
4. 将 HAVING 门槛改成 2，解释部门 10 为什么进入结果。
5. 依次用实体 null、实体 0、Wrapper null 更新奖金，结合 SQL 解释区别。

## 四、本节要点

- 会用 `QueryWrapper` / `LambdaQueryWrapper` 链式拼装条件，并在可选参数缺省时省略条件。
- 会用 `and` / `or` 嵌套表达复合逻辑，能解释括号如何改变结果。
- 掌握 `selectCount`、`selectOne`、部分字段、`selectMaps`、分组聚合和 `selectByMap`。
- 优先使用 Lambda 字段引用，并明确条件更新的 WHERE 范围。
- 理解 NOT_NULL 如何避免空值误覆盖，以及显式清空、XML 更新和并发更新的边界。

下一节：[第三节：分页、自动填充、逻辑删除与乐观锁](08-03-pagination-lifecycle.md)。
