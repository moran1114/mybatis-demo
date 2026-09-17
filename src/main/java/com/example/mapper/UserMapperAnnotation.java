package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 使用注解方式完成 user 表的 CRUD 操作
 *
 * 对比：
 *   - UserMapper.java + UserMapper.xml 是 XML 方式（SQL 写在 XML 里）
 *   - 本接口是纯注解方式（SQL 直接写在方法上的注解里），省去 XML 文件，适合简单 SQL
 *
 * 注意：项目已在 mybatis-config.xml 中开启 mapUnderscoreToCamelCase，
 *       所以 created_at 列会自动映射到 User.createdAt 属性，无需 @Results。
 */
public interface UserMapperAnnotation {

    // ============================================================
    // 一、注解方式 CRUD
    // ============================================================

    /** 查询所有用户（@Select） */
    @Select("SELECT * FROM user")
    List<User> findAll();

    /** 根据主键 id 查询用户（#{id} 是预编译占位符） */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    /** 新增用户（@Insert），主键自增后回填到 user.id */
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /** 修改用户（@Update） */
    @Update("UPDATE user SET username = #{username}, password = #{password}, email = #{email} WHERE id = #{id}")
    int update(User user);

    /** 删除用户（@Delete） */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);


    // ============================================================
    // 二、多参数传递的三种方式（同样的 SQL，三种传参写法）
    // ============================================================

    // 方式一：@Param 注解（推荐）—— 明确指定参数名，可读性好，支持任意多个参数
    @Select("SELECT * FROM user WHERE username = #{username} AND email = #{email}")
    List<User> findByParam(@Param("username") String username,
                           @Param("email") String email);

    // 方式二：Map 传参 —— 适合参数动态、不确定的场景，key 即参数名
    @Select("SELECT * FROM user WHERE username = #{username} AND email = #{email}")
    List<User> findByMap(Map<String, Object> params);

    // 方式三：实体类传参 —— 传入一个 POJO，SQL 中直接写属性名（底层通过 getter 取值）
    @Select("SELECT * FROM user WHERE username = #{username} AND email = #{email}")
    List<User> findByEntity(User user);


    // ============================================================
    // 三、#{} 与 ${} 的区别
    // ============================================================

    /**
     * #{}：预编译占位符（PreparedStatement 的 ?）
     * 最终 SQL 形如：SELECT * FROM user WHERE id = ?
     * 值作为参数单独传入，由 JDBC 做类型转换和转义，永远安全。
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectByIdSafe(@Param("id") Integer id);

    /**
     * ${}：字符串拼接（Statement 方式，直接替换进 SQL 文本）
     * 最终 SQL 形如：SELECT * FROM user ORDER BY username DESC
     *
     * 注意：${} 无法使用占位符，只能用于「列名、表名、ORDER BY 排序字段」等
     *      必须由开发者在代码里写死的场景，且值必须来自可信白名单，
     *      绝对不能接收用户输入，否则会引发 SQL 注入（见第四部分）。
     */
    @Select("SELECT * FROM user ORDER BY ${column} ${direction}")
    List<User> selectOrderBy(@Param("column") String column,
                             @Param("direction") String direction);


    // ============================================================
    // 四、SQL 注入攻击模拟与防御
    // ============================================================

    /**
     * 危险写法：用 ${} 拼接用户名和密码
     *
     * 攻击载荷：password = ' OR '1'='1
     * 拼接后的 SQL：
     *   SELECT * FROM user WHERE username = 'admin' AND password = '' OR '1'='1'
     * 由于 '1'='1' 恒为真，WHERE 条件整体成立，从而绕过密码校验、查出所有用户。
     *
     * 同理，username = 'admin' --  会用 -- 注释掉后面的密码条件。
     */
    @Select("SELECT * FROM user WHERE username = '${username}' AND password = '${password}'")
    List<User> loginInsecure(@Param("username") String username,
                             @Param("password") String password);

    /**
     * 安全写法：用 #{} 预编译占位符
     *
     * 攻击载荷：password = ' OR '1'='1
     * 最终 SQL 仍是：
     *   SELECT * FROM user WHERE username = ? AND password = ?
     * 攻击字符串被当作「一个普通的密码字面量」传入，SQL 结构不会被改变，
     * 所以查不到任何结果，注入攻击失效。
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND password = #{password}")
    List<User> loginSafe(@Param("username") String username,
                         @Param("password") String password);
}
