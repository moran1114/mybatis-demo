# MyBatis 注解开发与 SQL 注入防御笔记

> 本笔记配套项目 `mybatis-demo`，代码位于 `com.example.mapper.UserMapperAnnotation` 与
> 测试类 `com.example.AnnotationDemoTest`。所有示例均可直接运行验证。

---

## 〇、环境准备：建表 SQL

对应实体类 `com.example.entity.User`（字段：id / username / password / email / createdAt / updatedAt）。

```sql
CREATE DATABASE IF NOT EXISTS mybatisdb DEFAULT CHARSET utf8mb4;

USE mybatisdb;

CREATE TABLE IF NOT EXISTS `user` (
    `id`         INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `password`   VARCHAR(100) NOT NULL                COMMENT '密码',
    `email`      VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 初始测试数据
INSERT INTO `user` (`username`, `password`, `email`) VALUES
('admin', 'admin123', 'admin@example.com'),
('tom',   'tom123',   'tom@example.com');
```

> 说明：`created_at` / `updated_at` 由数据库自动填充；项目在 `mybatis-config.xml` 中开启了
> `mapUnderscoreToCamelCase`，所以 `created_at` 列会自动映射到 `User.createdAt` 属性。

---

## 一、注解方式完成 CRUD

注解方式把 SQL 直接写在 Mapper 接口的方法上，省去 XML 文件，适合简单 SQL。

```java
public interface UserMapperAnnotation {

    @Select("SELECT * FROM user")
    List<User> findAll();

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    // 主键自增后回填到 user.id
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username = #{username}, password = #{password}, email = #{email} WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}
```

**关键点**
- `@Select` / `@Insert` / `@Update` / `@Delete` 与 XML 中的四个标签一一对应。
- `@Options(useGeneratedKeys = true, keyProperty = "id")` 让自增主键回填到实体，等价于 XML 的
  `useGeneratedKeys="true" keyProperty="id"`。
- 需要在 `mybatis-config.xml` 的 `<mappers>` 中注册接口：`<mapper class="com.example.mapper.UserMapperAnnotation"/>`。

---

## 二、多参数传递的三种方式

同一个查询条件（username + email），三种传参写法：

| 方式 | 写法 | 适用场景 |
|------|------|----------|
| `@Param` | `findByParam(@Param("username") String u, @Param("email") String e)` | 参数明确、可读性好，**推荐** |
| `Map`   | `findByMap(Map<String, Object> params)` | 参数动态、不确定 |
| 实体类  | `findByEntity(User user)` | 参数恰好是一个对象 |

```java
// 方式一：@Param 注解
@Select("SELECT * FROM user WHERE username = #{username} AND email = #{email}")
List<User> findByParam(@Param("username") String username,
                       @Param("email") String email);

// 方式二：Map 传参（key 即参数名）
@Select("SELECT * FROM user WHERE username = #{username} AND email = #{email}")
List<User> findByMap(Map<String, Object> params);

// 方式三：实体类传参（SQL 里直接写属性名，底层通过 getter 取值）
@Select("SELECT * FROM user WHERE username = #{username} AND email = #{email}")
List<User> findByEntity(User user);
```

> 注意：当只有一个「非基本类型」参数且没有 `@Param` 时，MyBatis 会把它当作对象，
> 直接用属性名取值（方式三）；多个参数则必须用 `@Param` 或 Map，否则参数名会变成
> `arg0/arg1/param1/param2`，导致 `#{}` 找不到值。

---

## 三、`#{}` 与 `${}` 的区别

| | `#{}` | `${}` |
|---|-------|-------|
| 底层实现 | `PreparedStatement` 的 `?` 占位符 | 字符串拼接（Statement） |
| 类型处理 | 自动做类型转换与转义 | 无，原样拼进 SQL |
| 是否防注入 | ✅ 安全 | ❌ 存在注入风险 |
| 适用场景 | 值（WHERE 条件等） | 列名、表名、`ORDER BY` 排序字段 |

**日志对比**（`log4j` 打印的实际 SQL）：

```
#{} → ==>  Preparing: SELECT * FROM user WHERE id = ?
      ==> Parameters: 1(Integer)

${} → ==>  Preparing: SELECT * FROM user ORDER BY id DESC      ← 值直接拼进 SQL
```

```java
// #{}：安全
@Select("SELECT * FROM user WHERE id = #{id}")
User selectByIdSafe(@Param("id") Integer id);

// ${}：只用于「列名/排序方向」等代码内写死的可信值，绝不接用户输入
@Select("SELECT * FROM user ORDER BY ${column} ${direction}")
List<User> selectOrderBy(@Param("column") String column,
                         @Param("direction") String direction);
```

---

## 四、SQL 注入攻击模拟与防御

### 4.1 漏洞原理

`${}` 会把用户输入**原样拼进 SQL 文本**，攻击者通过构造特殊输入改写 SQL 结构。

### 4.2 攻击演示（危险写法 `${}`）

```java
@Select("SELECT * FROM user WHERE username = '${username}' AND password = '${password}'")
List<User> loginInsecure(@Param("username") String username,
                         @Param("password") String password);
```

攻击载荷：`password = ' OR '1'='1`，拼接后：

```sql
SELECT * FROM user WHERE username = 'admin' AND password = '' OR '1'='1'
```

由于 `AND` 优先级高于 `OR`，实际是 `(username='admin' AND password='') OR ('1'='1')`，
`'1'='1'` 恒为真，**绕过密码校验，查出全部用户**。实测返回 13 条用户记录。

其它常见载荷：
- `username = 'admin' -- ` → `--` 注释掉后面的密码条件。
- `username = 'x' OR 1=1 -- ` → 永远返回真。

### 4.3 防御方案（安全写法 `#{}`）

```java
@Select("SELECT * FROM user WHERE username = #{username} AND password = #{password}")
List<User> loginSafe(@Param("username") String username,
                     @Param("password") String password);
```

实际执行：

```sql
SELECT * FROM user WHERE username = ? AND password = ?
-- Parameters: admin(String), ' OR '1'='1(String)   ← 攻击串被当成普通密码字面量
```

攻击字符串被当作「一个普通的密码值」传入，SQL 结构不被改变，查询结果为空，**注入失效**。
实测返回 0 条。

### 4.4 防御原则总结

1. **用户输入一律用 `#{}`**（预编译占位符），这是第一道也是最有效的防线。
2. **`${}` 只用于 SQL 语法结构**（列名、表名、排序字段），且值必须来自**代码内可信白名单**
   （如 `switch` 枚举、`Set.contains()` 校验），绝不能直接取用户输入。
3. 数据库层最小权限、参数化查询、输入校验、Web 层转义等多层防护结合。

---

## 五、运行验证

```bash
mvn test -Dtest=AnnotationDemoTest
```

结果：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

四个测试分别对应：注解 CRUD、多参数三种方式、`#{}` vs `${}`、SQL 注入模拟与防御。

---

## 附：文件清单

| 文件 | 说明 |
|------|------|
| `src/main/java/com/example/mapper/UserMapperAnnotation.java` | 注解版 Mapper（CRUD + 多参数 + #{}与${} + 注入演示） |
| `src/test/java/com/example/AnnotationDemoTest.java` | 四个演示对应的 JUnit 测试 |
| `src/main/resources/mybatis-config.xml` | 注册了注解接口 `<mapper class="..."/>` |
