# MyBatis Demo 项目文件功能说明

## 1. 项目定位

本项目用于演示 MyBatis 和 MyBatis-Plus 的基本用法，内容包括：

- 普通 MyBatis XML Mapper 查询
- 员工表的增删改查
- 动态 SQL
- 批量插入和批量删除
- `choose` 分支查询
- MyBatis-Plus `BaseMapper`
- Lambda 条件构造器
- 分页查询
- H2 内存数据库测试
- Mapper 错误诊断测试

项目的主要调用关系如下：

```text
MyBatisEmpDemo 或 JUnit 测试
            |
            v
       MyBatisUtil
            |
            v
 SqlSessionFactory / SqlSession
            |
            v
         EmpMapper
        /         \
EmpMapper.xml   BaseMapper<Emp>
        \         /
            v
       emp / dept 数据表
```

## 2. 主代码目录

主代码位于 `src/main/java/com/demo2` 下。

### 2.1 `entity` 实体类

实体类负责表示数据库记录、查询条件或查询结果。它们本身通常不负责执行 SQL。

#### `Dept.java`

部门实体，对应数据库中的 `dept` 表。

主要字段：

- `deptno`：部门编号，主键
- `dname`：部门名称
- `loc`：部门所在地

类上的 `@TableName("dept")`、`@TableId` 和 `@TableField` 注解用于 MyBatis-Plus 的表名和字段映射。

#### `Emp.java`

员工实体，对应数据库中的 `emp` 表，是项目中最主要的实体类。

主要字段：

- `empno`：员工编号，使用自增主键
- `ename`：员工姓名
- `job`：职位
- `mgr`：上级经理编号
- `hiredate`：入职日期
- `sal`：工资
- `comm`：奖金
- `deptno`：部门编号

类上的 MyBatis-Plus 注解用于完成 `Emp` 与 `emp` 表之间的映射，其中 `empno` 使用 `IdType.AUTO` 表示由数据库生成主键。

#### `EmpBrief.java`

员工简要信息对象，只包含：

- 员工姓名
- 工资
- 部门编号

它适合接收只查询部分字段的结果，避免每次都返回完整的 `Emp` 对象。目前 `selectNameSalDeptno` 的 XML 映射仍使用 `Emp` 的 `empSummaryMap`，因此 `EmpBrief` 是可复用的结果 DTO，但暂未被当前 Mapper 方法直接使用。

#### `EmpManagerView.java`

员工与经理关联查询的结果对象，包含：

- 员工编号和姓名
- 经理编号和姓名

它适合处理 `emp` 表自关联查询，例如查询“员工对应的经理”。当前项目暂未配置对应的 Mapper 查询，属于预留的查询结果对象。

#### `DeptPayrollSummary.java`

部门工资统计结果对象，包含：

- 部门编号
- 部门名称
- 员工数量
- 工资总额
- 平均工资

它适合接收 `GROUP BY deptno`、`COUNT`、`SUM`、`AVG` 等统计查询结果。当前项目暂未配置对应的统计 Mapper 方法。

#### `EmpQuery.java`

员工查询条件对象，字段包括：

- `ename`：姓名条件
- `job`：职位条件
- `deptno`：部门条件
- `minSal`：最低工资条件

该类还定义了 `SortColumn` 枚举：

- `EMPNO`
- `SAL`
- `HIREDATE`

枚举用于限制动态排序字段，避免把用户输入直接拼接到 `ORDER BY` 中。当前 `EmpMapper.selectSorted` 使用了这个枚举；动态多条件查询目前主要使用 `Emp` 作为条件对象。

### 2.2 `mapper/EmpMapper.java`

`EmpMapper` 是数据库操作接口，负责声明可以执行的数据库方法。

它继承 `BaseMapper<Emp>`，因此同时拥有两类能力：

#### 普通 MyBatis 方法

- 基础查询：查询全部员工、指定字段、工资范围、部门、日期和奖金条件
- 单条查询：根据员工编号查询
- 基础增删改：插入、更新和删除员工
- 动态条件查询：按姓名、职位、部门和工资等条件组合查询
- 动态排序：按员工编号、工资或入职日期排序
- 动态更新：只更新不为空的字段
- 批量插入和批量删除
- `choose` 分支查询

#### MyBatis-Plus 方法

继承 `BaseMapper<Emp>` 后，可以直接使用：

- `insert`
- `selectById`
- `updateById`
- `deleteById`
- `selectList`
- `selectPage`

接口中的 `countByDeptno` 使用注解 `@Select` 直接声明 SQL，其他普通方法主要由 `EmpMapper.xml` 提供 SQL 实现。

### 2.3 `util/MyBatisUtil.java`

MyBatis 配置和会话工厂工具类，负责把配置文件、数据库连接和 Mapper 组合起来。

主要功能：

- 读取 `mybatis-config.xml`
- 读取 `db.properties`
- 创建普通 MyBatis 的 `SqlSessionFactory`
- 创建 MyBatis-Plus 的 `SqlSessionFactory`
- 打开数据库会话 `SqlSession`
- 根据当前环境覆盖数据库连接参数
- 配置 MyBatis-Plus 分页拦截器
- 支持 H2 测试数据库和 MySQL 运行环境

常用方法：

- `openSession()`：打开默认数据库会话
- `openMyBatisSession()`：打开普通 MyBatis 会话
- `buildMyBatis(...)`：构建普通 MyBatis 工厂
- `build(...)`：构建带 MyBatis-Plus 配置的工厂
- `localDatabaseProperties()`：读取本地数据库配置

### 2.4 `util/MyBatisEmpDemo.java`

控制台演示程序，包含 `main` 方法，可以直接观察查询结果。

它分为两个演示部分：

#### 普通 MyBatis 演示

- 基础查询
- 单条查询
- 员工增删改查
- 动态条件查询
- 动态更新
- 批量插入
- 批量删除
- 部门数量统计
- 动态排序

#### MyBatis-Plus 演示

- `BaseMapper` 通用 CRUD
- Lambda 条件构造器
- 分页查询

## 3. Mapper XML 和其他主资源

### `src/main/resources/mapper/EmpMapper.xml`

这是普通 MyBatis 的核心 SQL 文件，命名空间对应 `com.demo2.mapper.EmpMapper`。

主要内容：

- `empMap`：完整员工字段映射
- `empSummaryMap`：姓名、工资、部门编号的部分字段映射
- `empColumns`：员工字段 SQL 片段
- 10 条基础查询
- 员工增删改查
- `<if>` 和 `<where>` 多条件查询
- `<trim>` 清理动态 SQL 的多余 `AND`
- `<set>` 动态更新
- `<foreach>` 批量插入和批量删除
- `<choose>`、`<when>`、`<otherwise>` 分支查询

### `src/main/resources/mapper/UserMapper.xml`

当前是一个预留的 Mapper XML 文件，声明了 `com.demo2.mapper.UserMapper` 命名空间，但项目中暂时没有对应的 `UserMapper.java` 和具体 SQL。

### `src/main/resources/mybatis-config.xml`

MyBatis 全局配置文件，负责：

- 加载数据库属性
- 配置实体类别名
- 配置驼峰命名转换
- 注册 `EmpMapper.xml`
- 设置数据库环境

### `src/main/resources/db.properties`

保存数据库连接配置，例如：

- JDBC 驱动
- 数据库 URL
- 用户名
- 密码

### `src/main/resources/sql/reload_emp_dept.sql`

用于 MySQL 环境初始化或重新加载 `dept`、`emp` 表结构和示例数据。

### `sql/basic-queries.sql`

保存 10 条经典基础查询语句，包括：

1. 查询所有员工
2. 查询指定字段
3. 按工资筛选
4. 按部门筛选
5. 按入职日期筛选
6. 查询有奖金的员工
7. 按职位筛选
8. 工资范围查询
9. 奖金为空且工资较低的员工查询
10. 按工资降序排序

`EmpBasicQueryTest` 会读取这个文件，并把原始 SQL 的结果和 Mapper 查询结果进行对比。

### `src/main/resources/log4j2.xml`

Log4j2 日志配置文件，用于输出 MyBatis 执行的 SQL、参数和查询结果数量，方便观察 SQL 是否正确。

## 4. 测试代码

测试代码位于 `src/test/java/com/demo2` 下。

### `support/EmbeddedDatabase.java`

测试数据库辅助类，使用 H2 创建内存数据库。

主要工作：

- 创建独立的 H2 数据库
- 执行 `schema-h2.sql` 创建表
- 执行 `data-h2.sql` 插入测试数据
- 生成测试用数据库连接属性

不同测试类使用不同数据库名称，避免测试数据相互影响。

### `mapper/EmpBasicQueryTest.java`

基础 SQL 查询测试，使用参数化测试方式运行 10 组查询。

每组测试会：

1. 从 `sql/basic-queries.sql` 读取一条原始 SQL
2. 直接执行原始 SQL
3. 调用 `EmpMapper` 中对应的查询方法
4. 比较两种方式返回的员工结果

它主要验证基础 SQL 和 Mapper SQL 是否保持一致。

### `mapper/EmpMapperTest.java`

普通 MyBatis 功能测试，共 10 个测试方法，覆盖：

- 员工完整 CRUD
- 生成数据库自增主键
- `if / where` 多条件查询
- 空条件和空字符串处理
- `trim` 动态 SQL
- 参数绑定和 SQL 注入防护
- 动态排序字段限制
- `<set>` 动态更新
- `<foreach>` 批量插入
- `<foreach>` 批量删除
- 空集合删除保护
- `<choose>` 分支查询
- 下划线字段到驼峰属性的映射

这是动态 SQL 课程内容最主要的测试类。

### `mapper/EmpMyBatisPlusTest.java`

MyBatis-Plus 功能测试，共 2 个测试方法。

主要验证：

- `BaseMapper` 的新增、查询、修改和删除
- Lambda 条件构造器
- 按部门和工资组合查询
- 分页查询
- 总记录数、总页数和当前页数据
- 超出最后一页时返回空结果

### `mapper/FaultDiagnosisTest.java`

Mapper 错误诊断测试，共 4 个测试方法。

它会临时修改 Mapper XML，模拟常见错误，并检查 MyBatis 是否抛出正确异常：

- 实体属性名称错误：测试参数绑定异常
- 命名参数名称错误：测试 `BindingException`
- 结果字段映射错误：测试属性是否保持为空
- 缺少自动生成主键配置：测试数据虽然插入，但实体主键不会自动回填

## 5. 测试资源

### `src/test/resources/schema-h2.sql`

创建 H2 测试数据库的表结构，包括：

- `dept` 部门表
- `emp` 员工表
- 主键约束
- 员工到部门的外键约束
- 工资和奖金检查约束
- 员工姓名和部门编号索引

### `src/test/resources/data-h2.sql`

插入测试使用的部门和员工数据。

测试数据包含不同的：

- 部门编号
- 职位
- 工资
- 奖金为空和不为空的情况
- 不同入职日期
- 经理关系

这些差异保证各种查询条件都能得到有意义的测试结果。

## 6. 课程步骤对应关系

| 课程内容 | 主要实现位置 | 主要测试 |
| --- | --- | --- |
| `if / where` 多条件查询 | `EmpMapper.xml` 的 `empConditions`、`selectByCondition` | `EmpMapperTest` |
| `set` 动态更新 | `EmpMapper.xml` 的 `updatePartial` | `EmpMapperTest` |
| `foreach` 批量插入和删除 | `insertBatch`、`deleteBatch` | `EmpMapperTest` |
| `choose / when / otherwise` | `selectPreferred`、`selectSorted` | `EmpMapperTest` |
| MyBatis-Plus 配置 | `MyBatisUtil`、`mybatis-config.xml` | `EmpMyBatisPlusTest` |
| 实体注解映射 | `Emp`、`Dept` | `EmpMyBatisPlusTest` |
| `BaseMapper` 通用 CRUD | `EmpMapper extends BaseMapper<Emp>` | `EmpMyBatisPlusTest` |
| Lambda 条件构造器 | `LambdaQueryWrapper` | `EmpMyBatisPlusTest` |
| 分页插件 | `PaginationInnerInterceptor` | `EmpMyBatisPlusTest` |

## 7. 测试统计

当前 Maven 测试结果为 26 个测试：

- `EmpBasicQueryTest`：10 个
- `EmpMapperTest`：10 个
- `EmpMyBatisPlusTest`：2 个
- `FaultDiagnosisTest`：4 个

运行命令：

```bash
mvn test
```

测试成功时应显示：

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 8. 当前项目的使用特点

1. 普通 MyBatis 主要通过 `EmpMapper.xml` 编写 SQL。
2. MyBatis-Plus 主要通过实体注解和 `BaseMapper` 自动生成通用 SQL。
3. H2 只用于自动化测试，不会修改真实 MySQL 数据库。
4. `EmpBrief`、`EmpManagerView` 和 `DeptPayrollSummary` 是为部分字段查询、关联查询和统计查询准备的 DTO，目前还没有对应的 Mapper 方法。
5. `EmpBasicQueryTest` 验证经典 SQL，`EmpMapperTest` 验证动态 SQL，`EmpMyBatisPlusTest` 验证 MyBatis-Plus，`FaultDiagnosisTest` 验证错误配置。
