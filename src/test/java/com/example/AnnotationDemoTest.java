package com.example;

import com.example.entity.User;
import com.example.mapper.UserMapperAnnotation;
import com.example.util.SqlSessionUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解方式 CRUD + 多参数三种方式 + #{}与${} 区别 + SQL 注入演示 测试类
 *
 * 运行时请打开控制台日志，重点观察 MyBatis 打印出的 SQL：
 *   - #{} 会打印成 ? 占位符，参数单独列出（PreparedStatement）
 *   - ${} 会直接把值拼进 SQL 文本（Statement）
 */
public class AnnotationDemoTest {

    private SqlSession session;
    private UserMapperAnnotation mapper;

    @Before
    public void init() {
        // 复用工具类开启会话（true：自动提交）
        session = SqlSessionUtil.openSession(true);
        mapper = session.getMapper(UserMapperAnnotation.class);
    }

    @After
    public void destroy() {
        if (session != null) {
            session.close();
        }
    }


    // ============================================================
    // 一、注解方式 CRUD
    // ============================================================
    @Test
    public void testAnnotationCrud() {
        System.out.println("\n========== 一、注解方式 CRUD ==========");

        // 1. 新增
        User user = new User();
        user.setUsername("annotation_user");
        user.setPassword("123456");
        user.setEmail("annotation@example.com");
        int insertRows = mapper.insert(user);
        System.out.println("【新增】影响行数=" + insertRows + "，回填主键 id=" + user.getId());
        Assert.assertEquals(1, insertRows);
        Assert.assertNotNull("主键应已回填", user.getId());

        // 2. 按 id 查询
        User found = mapper.findById(user.getId());
        System.out.println("【查询】" + found);
        Assert.assertNotNull(found);
        Assert.assertEquals(user.getId(), found.getId());

        // 3. 修改
        found.setEmail("annotation_new@example.com");
        int updateRows = mapper.update(found);
        System.out.println("【修改】影响行数=" + updateRows);
        Assert.assertEquals(1, updateRows);

        // 4. 删除
        int deleteRows = mapper.deleteById(user.getId());
        System.out.println("【删除】影响行数=" + deleteRows);
        Assert.assertEquals(1, deleteRows);
    }


    // ============================================================
    // 二、多参数传递的三种方式
    // ============================================================
    @Test
    public void testMultiParam() {
        System.out.println("\n========== 二、多参数传递三种方式 ==========");

        // 先造一条测试数据
        User u = new User();
        u.setUsername("multi_param_user");
        u.setPassword("123456");
        u.setEmail("multi@example.com");
        mapper.insert(u);

        // 方式一：@Param 注解
        List<User> byParam = mapper.findByParam("multi_param_user", "multi@example.com");
        System.out.println("【@Param 方式】结果条数=" + byParam.size());

        // 方式二：Map 传参
        Map<String, Object> map = new HashMap<>();
        map.put("username", "multi_param_user");
        map.put("email", "multi@example.com");
        List<User> byMap = mapper.findByMap(map);
        System.out.println("【Map 方式】结果条数=" + byMap.size());

        // 方式三：实体类传参
        User condition = new User();
        condition.setUsername("multi_param_user");
        condition.setEmail("multi@example.com");
        List<User> byEntity = mapper.findByEntity(condition);
        System.out.println("【实体类方式】结果条数=" + byEntity.size());

        // 三种方式结果应一致
        Assert.assertEquals(1, byParam.size());
        Assert.assertEquals(1, byMap.size());
        Assert.assertEquals(1, byEntity.size());

        // 清理测试数据
        mapper.deleteById(u.getId());
    }


    // ============================================================
    // 三、#{} 与 ${} 的区别
    // ============================================================
    @Test
    public void testHashVsDollar() {
        System.out.println("\n========== 三、#{} 与 ${} 的区别 ==========");

        // #{}：预编译占位符，日志里会打印 ? 和参数
        User user = mapper.selectByIdSafe(1);
        System.out.println("【#{} 查询】" + user);

        // ${}：字符串拼接，日志里直接打印拼好的 SQL（这里传可信的列名/方向）
        List<User> ordered = mapper.selectOrderBy("id", "DESC");
        System.out.println("【${} 排序】结果条数=" + ordered.size() + "，第一条 id=" +
                (ordered.isEmpty() ? "无" : ordered.get(0).getId()));

        Assert.assertNotNull(user);
        Assert.assertNotNull(ordered);
    }


    // ============================================================
    // 四、SQL 注入攻击模拟与防御
    // ============================================================
    @Test
    public void testSqlInjection() {
        System.out.println("\n========== 四、SQL 注入模拟与防御 ==========");

        // 攻击载荷：把 password 条件改写成恒真表达式，从而绕过密码校验
        String attackPassword = "' OR '1'='1";

        // --- 危险写法（${}）：注入成功 ---
        System.out.println("----- 危险写法（${}）-----");
        System.out.println("拼接后 SQL: SELECT * FROM user WHERE username = 'admin' AND password = '' OR '1'='1'");
        List<User> hacked = mapper.loginInsecure("admin", attackPassword);
        System.out.println("【注入结果】查到用户数=" + hacked.size() + "（>0 说明已被注入绕过）");
        for (User u : hacked) {
            System.out.println("    -> " + u);
        }
        Assert.assertTrue("危险写法应被注入绕过、查到数据", hacked.size() > 0);

        // --- 安全写法（#{}）：注入失效 ---
        System.out.println("----- 安全写法（#{}）-----");
        System.out.println("实际 SQL 仍是: SELECT * FROM user WHERE username = ? AND password = ?（攻击串被当作密码字面量）");
        List<User> safe = mapper.loginSafe("admin", attackPassword);
        System.out.println("【注入结果】查到用户数=" + safe.size() + "（0 说明注入被防御）");
        Assert.assertEquals("安全写法应查不到数据、注入被防御", 0, safe.size());
    }
}
