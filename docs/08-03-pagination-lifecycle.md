# 第三节：分页、自动填充、逻辑删除与乐观锁 - 详细版

本节对应 **8.3.1—8.3.5**，基于本项目的 Java 控制台工程与 MyBatis-Plus **3.5.3.1**。分页继续查询 `emp`；自动填充、逻辑删除与乐观锁使用独立的 `emp_lifecycle` 表。

完整入口：[MyBatisPlusLifecycleDemo.java](../src/main/java/com/demo2/util/MyBatisPlusLifecycleDemo.java)。配套测试：[EmpLifecycleTest.java](../src/test/java/com/demo2/mapper/EmpLifecycleTest.java)。本文对版本行为的说明均有本项目代码或测试对应。

## 一、学习目标

- 配置 `MybatisPlusInterceptor` 与 `PaginationInnerInterceptor` 实现物理分页。
- 实现 `MetaObjectHandler` 自动填充 `createTime` / `updateTime`。
- 理解 `@TableLogic` 生成逻辑删除 SQL 的原理、查询过滤和适用边界。
- 理解 `@Version` 乐观锁的 WHERE 校验、版本递增与冲突处理。

## 二、核心知识点

### 8.3.1 插件链：MybatisPlusInterceptor

本项目由 [MyBatisUtil.build](../src/main/java/com/demo2/util/MyBatisUtil.java) 构建 MyBatis-Plus 会话工厂。核心配置如下：

```java
SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder()
        .build(reader, "local", databaseProperties);

GlobalConfigUtils.getGlobalConfig(factory.getConfiguration())
        .setMetaObjectHandler(new AuditMetaObjectHandler());
factory.getConfiguration().addMapper(EmpLifecycleMapper.class);

PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(databaseType);
pagination.setOverflow(false);
pagination.setMaxLimit(100L);

MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
interceptor.addInnerInterceptor(pagination);
factory.getConfiguration().addInterceptor(interceptor);
```

这里的 `reader` 和连接属性由工具类读取，`databaseType` 在本机运行时为 `DbType.MYSQL`，测试时为 `DbType.H2`。项目原有的 JDK 泛型解析适配也保留在该方法中。

| 组件 | 职责 |
| --- | --- |
| `MybatisSqlSessionFactoryBuilder` | 构建 MyBatis-Plus 工厂，支持注入 BaseMapper SQL |
| `MybatisPlusInterceptor` | 持有并执行内部插件链 |
| `OptimisticLockerInnerInterceptor` | 更新时读取实体版本，加入版本校验并计算新版本 |
| `PaginationInnerInterceptor` | 根据 Page 与数据库方言生成分页 SQL，并按需统计总数 |
| `MetaObjectHandler` | 在写入参数处理阶段填充实体属性，不是分页插件 |
| `@TableLogic` | 让通用 SQL 注入识别删除标记，不需要另外添加“逻辑删除拦截器” |

分页放在内部插件链最后，有助于让它基于前面处理后的 SQL 统计并分页。项目无需 Spring 容器，所以直接把填充处理器设置到 `GlobalConfig`；仅在类上写 `@Component` 并不能让这个控制台工程自动发现它。

本节保持原依赖版本。官方当前文档中的独立 `mybatis-plus-jsqlparser` 依赖要求从 **3.5.9** 才开始，不能把新版本的依赖说明直接套到本项目的 3.5.3.1。见 [官方分页插件说明](https://baomidou.com/plugins/pagination/)。

### 8.3.2 物理分页：Page

```java
Page<Emp> request = Page.of(2, 3); // 第 2 页，每页 3 条；页码从 1 开始
Page<Emp> result = empMapper.selectPage(request,
        Wrappers.lambdaQuery(Emp.class).orderByAsc(Emp::getEmpno));

long total = result.getTotal();
long pages = result.getPages();
List<Emp> records = result.getRecords();
```

原始 8 条员工数据下：

| 属性 | 含义 | 本例结果 |
| --- | --- | --- |
| `current` | 当前页号 | 2 |
| `size` | 每页大小 | 3 |
| `total` | 满足条件的总记录数 | 8 |
| `pages` | 总页数 | 3 |
| `records` | 当前页的记录集合 | 员工编号 4、5、6 |

MySQL 方言的 SQL 结构是：

```sql
SELECT COUNT(*) AS total FROM emp;
SELECT * FROM emp ORDER BY empno ASC LIMIT ?, ?;
-- offset = (2 - 1) * 3 = 3，size = 3。
```

H2 日志可能表现为 `LIMIT ? OFFSET ?`，参数顺序随方言不同。两者都由数据库只返回所需记录，属于物理分页；不会先把全部员工加载到 Java 再截取列表。

使用要点：

- **先注册插件。** 单独构造 `Page` 不能替代分页插件配置。
- **指定稳定排序。** 建议包含主键作为最后一个排序字段。没有 `ORDER BY` 时不能依赖每一页的行顺序。
- **校验页号与大小。** 面向用户的入口应要求 `current >= 1`、`size > 0`。本项目 `maxLimit=100` 对正向大页请求限为 100，不代替参数校验。
- **处理越界。** 当前 `overflow=false`，超过末页时记录为空，不自动跳回第一页；默认总数查询仍会返回总数。
- **按需统计。** `request.setSearchCount(false)` 可跳过 COUNT，此时不要把 `total/pages` 当成已计算结果。
- **理解边界。** 深分页的较大 offset 会增加扫描成本；多次请求之间若发生插入删除，offset 分页也不保证跨请求快照一致。

对 `EmpLifecycleMapper` 分页时，插件使用的查询包含 `deleted=0`，记录与总数都排除已删除员工。配套测试验证了两页不重叠、逻辑删除后的总数、越界和单页上限。

### 8.3.3 自动填充：MetaObjectHandler

审计字段的含义：`createTime` 记录创建时间，`updateTime` 记录最近一次成功写入时携带的更新时间。数据库列使用 `create_time`、`update_time`，Java 使用 `LocalDateTime`。

**第一步：标记填充时机。**

在 [EmpLifecycle.java](../src/main/java/com/demo2/entity/EmpLifecycle.java) 中：

```java
@TableField(value = "create_time", fill = FieldFill.INSERT,
        updateStrategy = FieldStrategy.NEVER)
private LocalDateTime createTime;

@TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

`INSERT` 表示插入填充，`INSERT_UPDATE` 表示插入和更新均填充。`createTime` 的 `NEVER` 更新策略让实体生成的普通 UPDATE 不修改创建时间；这不是数据库层的不可变约束。

**第二步：实现处理器。**

[AuditMetaObjectHandler.java](../src/main/java/com/demo2/handler/AuditMetaObjectHandler.java) 的主要逻辑为：

```java
@Override
public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now(clock);
    strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
    strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
}

@Override
public void updateFill(MetaObject metaObject) {
    if (metaObject.hasSetter("updateTime")) {
        setFieldValByName("updateTime", LocalDateTime.now(clock), metaObject);
    }
}
```

`clock` 默认取系统时区时钟，测试注入固定时钟，无需暂停线程等待时间变化。字段名必须使用 Java 属性名，类型也必须与实体声明一致。

这里在更新时显式覆盖 `updateTime`，原因是：**`strictUpdateFill` 默认跳过已有非 null 值**。从数据库读取的实体已带着旧更新时间，直接严格填充会保留它。当前业务规则要求每次实体更新刷新时间，因此使用 `setFieldValByName`。插入仍采用严格填充，显式提供的非 null 创建时间会被保留。相关默认行为见 [官方自动填充说明](https://baomidou.com/guides/auto-fill-field/)。

**第三步：注册并验证。**

8.3.1 中的 `GlobalConfigUtils` 注册必须在执行 Mapper 操作前完成。随后无需在业务代码设置时间：

```java
EmpLifecycle employee = new EmpLifecycle();
employee.setEname("Lifecycle Demo");
employee.setDeptno(20);
employee.setSal(new BigDecimal("12000"));
employee.setVersion(0);
mapper.insert(employee);
// createTime / updateTime 由处理器填充，empno 由数据库回填。

EmpLifecycle stored = mapper.selectById(employee.getEmpno());
stored.setSal(new BigDecimal("13000"));
int affected = mapper.updateById(stored);
// affected == 1 时，数据库保留原 createTime 并写入新的 updateTime。
```

本项目对 `emp_lifecycle` 没有配置数据库时间默认值，用于明确观察 Java 填充效果。`LocalDateTime` / DATETIME 本身不保存时区，部署时需要统一时间约定。

**有实体与无实体的更新不同：**

```java
// 无实体：本项目配置下不会触发实体自动填充，也不会自动启用版本校验。
mapper.update(null, Wrappers.lambdaUpdate(EmpLifecycle.class)
        .eq(EmpLifecycle::getEmpno, id)
        .set(EmpLifecycle::getSal, new BigDecimal("14000")));

// 有实体：提供用于填充及乐观锁的实体，并携带读取时的版本。
EmpLifecycle patch = new EmpLifecycle();
patch.setSal(new BigDecimal("14000"));
patch.setVersion(oldVersion);
int affected = mapper.update(patch, Wrappers.lambdaUpdate(EmpLifecycle.class)
        .eq(EmpLifecycle::getEmpno, id));
```

自动填充值先写进 Java 对象，再尝试执行 SQL。因此 SQL 失败、乐观锁冲突或事务回滚后，不能仅凭对象里的新时间认定数据库已更新。应检查影响行数，并以提交结果和重新读取的记录为准。

### 8.3.4 逻辑删除：@TableLogic

实体声明：

```java
@TableLogic(value = "0", delval = "1")
@TableField("deleted")
private Integer deleted;
```

建表脚本中对应 `deleted INT NOT NULL DEFAULT 0`，0 表示有效，1 表示已删除。插入时不指定该属性，数据库提供默认值；它不是自动把所有插入值都强制改成 0。

在本项目中，逻辑删除是 MyBatis-Plus 根据实体元数据生成通用 SQL 的能力，不是任意 SQL 的全局文本替换：

| 操作 | 生成 SQL 的核心变化 |
| --- | --- |
| `selectById(id)` | `WHERE empno = ? AND deleted = 0` |
| `selectList` / `selectCount` / `selectPage` | 查询条件包含 `deleted = 0` |
| `updateById(entity)` | WHERE 包含 `deleted = 0`，不更新已删除记录 |
| `deleteById(id)` | 生成 UPDATE 标记删除，并要求旧值为 0 |

```java
int affected = mapper.deleteById(id);
EmpLifecycle invisible = mapper.selectById(id); // null
```

```sql
UPDATE emp_lifecycle SET deleted = 1
WHERE empno = ? AND deleted = 0;
```

再次删除同一行返回 0。用 JDBC 执行下面的诊断查询，仍可以看到物理行和 `deleted=1`：

```sql
SELECT empno, ename, deleted FROM emp_lifecycle WHERE empno = ?;
```

这解释了为什么“业务查不到”不等于“数据库中不存在”。示例与测试都直接验证了物理行仍存在。通用查询与删除的规则可对照 [官方逻辑删除说明](https://baomidou.com/guides/logic-delete/)。

适用边界：

- **自定义 XML、注解 SQL、JDBC 查询不会仅因实体带注解就自动添加删除条件。** JOIN 中各表的删除条件也应按需要显式写出。
- **逻辑删除保留的是当前行。** 它不是历史版本表或完整审计日志；被覆盖前的字段值不会自动保存。
- **数据库约束仍然有效。** 唯一键不会因为 `deleted=1` 自动释放；外键引用、表大小和索引占用仍需考虑，也不会触发物理删除的级联动作。
- **恢复和清理需专门设计。** 普通 `updateById` 会过滤已删除行，不能靠把 deleted 改回 0 直接恢复。恢复、查询已删数据和物理清理需要明确的业务入口与 SQL。
- **删除接口的行为要按版本确认。** 当前 3.5.3.1 的 `deleteById(id)` 不自动刷新 `updateTime`，也不自动携带版本校验或递增版本。配套测试对此有断言。需要记录删除人、删除时间或防止并发删除时，应单独实现相应更新语义。

示例使用独立 `emp_lifecycle` 表，原来的 `emp` 表继续用于基础 CRUD 和关联查询练习。学习时可以比较两张表的删除 SQL。

### 8.3.5 乐观锁：@Version

实体声明：

```java
@Version
@TableField("version")
private Integer version;
```

数据库列为 `version INT NOT NULL DEFAULT 0`，插件链注册 `OptimisticLockerInnerInterceptor`。整数版本从旧值递增为新值，并在 WHERE 中验证读取时的旧版本。

**完整流程：**

1. A、B 都读取到同一行的 `version=0`。
2. A 携带版本 0 更新，SQL 匹配成功，写入版本 1，影响行数为 1。
3. B 仍携带版本 0 更新，数据库已经是版本 1，WHERE 不匹配，影响行数为 0。
4. B 的业务代码识别冲突，重新读取并决定是否合并修改或提示用户。

```sql
-- A：成功，假设旧版本为 0。
UPDATE emp_lifecycle
SET sal = ?, update_time = ?, version = 1
WHERE empno = ? AND version = 0 AND deleted = 0;

-- B：仍然要求 version = 0，因此更新 0 行。
UPDATE emp_lifecycle
SET sal = ?, update_time = ?, version = 1
WHERE empno = ? AND version = 0 AND deleted = 0;
```

实际实体包含其他非 null 字段时，SET 也会包含它们；这里保留与原理有关的部分。数据库是在执行 UPDATE 时原子地检查条件，应用无需先执行 `SELECT FOR UPDATE`；数据库执行 UPDATE 时仍可能使用行锁。

**本项目用两个独立快照确定性地演示冲突：**

```java
EmpLifecycle first = mapper.selectById(id);
session.clearCache();
EmpLifecycle stale = mapper.selectById(id);

first.setSal(new BigDecimal("13000"));
int firstRows = mapper.updateById(first); // 1，first.version 回填为 1

stale.setSal(new BigDecimal("14000"));
int staleRows = mapper.updateById(stale); // 0，数据库工资仍为 13000
```

`clearCache()` 是为了避免同一 SqlSession 的一级缓存返回同一个对象。这是一个事务中的旧快照模拟，用来验证版本条件，并非多线程并发测试；真实业务的两个请求通常来自各自会话。不要在线程间共享 SqlSession。

需要牢记的前提与限制：

- **注解和插件都要配置。** 只写 `@Version` 不会自动启用拦截器。
- **更新实体必须携带读取时的非 null 版本。** 此版本下 null 版本会跳过插件校验；新建补丁对象时不要遗漏版本，也不要凭空填一个“最新版本”。
- **插件不负责创建初始版本。** 可以插入时显式设置 0，或使用数据库默认值。本项目省略 version 插入时，数据库为 0，但该默认值不会像自增主键一样自动回填到 Java 对象，应重新查询。
- **不要手动提前加一。** 传旧版本，插件计算新版本。成功时新版本会回填实体；即使失败，实体的版本也可能已在执行 SQL 前被修改，不能据此判断成功。
- **优先使用本章验证的 `updateById(entity)` 或 `update(entity, wrapper)`。** 后者每次创建新 Wrapper；插件会向 Wrapper 添加版本条件，不能复用上次的实例。
- **当前默认插件配置下，`update(null, wrapper)` 没有实体版本可校验。** 自定义 SQL、删除操作也不能直接假定受同样保护。
- **版本冲突通常不抛异常。** 影响行数为 0 也可能是记录不存在、已删除或其他 WHERE 条件不满足，需要按业务区分。

以上配置和版本递增机制可对照 [官方乐观锁插件文档](https://baomidou.com/plugins/optimistic-locker/)。本文使用 3.5.3.1 已验证的调用方式，不依赖较新版本的附加接口。

**冲突处理思路：**

```java
int affected = mapper.updateById(employee);
if (affected == 0) {
    // 在合适的新事务/读取上下文获取当前数据，避免继续使用旧快照。
    // 提示“记录已变更，请刷新”，或重新校验后只合并仍然有效的修改。
}
```

不要只把旧实体的版本改成新版本再直接提交，那可能覆盖他人的修改。若业务允许重试，应重新读取、重新计算业务结果、限制重试次数，并考虑重复调用的副作用。实体更新时间、版本回填和数据库事务提交是不同阶段；只有成功提交后变更才持久化。

## 三、表结构、运行与验证

关键文件：

| 文件 | 用途 |
| --- | --- |
| [EmpLifecycle.java](../src/main/java/com/demo2/entity/EmpLifecycle.java) | 员工基本字段、时间字段、逻辑删除字段和版本字段 |
| [EmpLifecycleMapper.java](../src/main/java/com/demo2/mapper/EmpLifecycleMapper.java) | 继承 `BaseMapper<EmpLifecycle>` |
| [AuditMetaObjectHandler.java](../src/main/java/com/demo2/handler/AuditMetaObjectHandler.java) | 插入填充时间，更新刷新时间 |
| [MyBatisUtil.java](../src/main/java/com/demo2/util/MyBatisUtil.java) | 注册 Mapper、填充处理器、乐观锁与分页插件 |
| [init_emp_lifecycle.sql](../src/main/resources/sql/init_emp_lifecycle.sql) | MySQL 创建独立实验表，不清空已有数据 |
| [MyBatisPlusLifecycleDemo.java](../src/main/java/com/demo2/util/MyBatisPlusLifecycleDemo.java) | 展示分页、填充、逻辑删除及版本冲突 |

MySQL 运行步骤：

1. 使用已有的 `persistence_lab` 实验库与 `emp` 表；首次初始化可执行 `reload_emp_dept.sql`，注意该旧脚本会重建基础表。
2. 执行 `src/main/resources/sql/init_emp_lifecycle.sql`。
3. 按项目要求配置 `src/main/resources/db.properties`。
4. 在 IDEA 运行 `com.demo2.util.MyBatisPlusLifecycleDemo`。

该程序自行插入生命周期示例记录，最终回滚，已有实验数据不需要额外种子数据。自增序列可能推进；要保存实际业务修改时才调用 `session.commit()`。

无需本机 MySQL，在 PowerShell / CMD 终端执行：

```powershell
mvn -Dtest=EmpLifecycleTest test
mvn test
```

在 IDEA 的 Maven 运行配置参数栏中，只填写参数和目标，不填写 `mvn`，也不要加单引号：

```text
-Dtest=EmpLifecycleTest test
```

运行全部测试时，该参数栏只需填写 `test`。

本章 8 个测试覆盖自动填充、创建时间保护、逻辑删除物理行保留、版本冲突与重新读取、有实体/无实体更新边界、分页边界和完整演示入口。测试使用 H2 MySQL 兼容模式；MySQL 方言 SQL 的最终表现以本机 MySQL 运行日志为准。

## 四、本节要点

- 会配置 `MybatisPlusInterceptor` 插件链，明确乐观锁与分页插件的职责及顺序。
- 会使用 `Page` 实现物理分页，理解总数、页数、排序和越界行为。
- 会实现并注册 `MetaObjectHandler`，正确维护创建时间和更新时间。
- 说得清逻辑删除如何生成 UPDATE 和查询过滤，知道自定义 SQL、恢复、约束和删除审计的边界。
- 说得清乐观锁的旧版本 WHERE 校验、整数版本递增与影响行数判断，能提出合理的冲突处理方式。

上一节：[第二节：条件构造器与高级查询](08-02-query-wrapper.md)。
