package com.example;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 用户 Mapper 测试类
 * 测试 UserMapper 接口的所有方法
 */
public class UserMapperTest {

    private InputStream is;
    private SqlSession session;
    private UserMapper userMapper;

    /**
     * 初始化方法（在每个 @Test 方法执行前执行）
     */
    @Before
    public void init() throws IOException {
        System.out.println("初始化方法执行...");

        // 1. 加载配置文件
        is = Resources.getResourceAsStream("mybatis-config.xml");

        // 2. 创建 SqlSessionFactory
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(is);

        // 3. 创建 SqlSession（true：自动提交事务）
        session = factory.openSession(true);

        // 4. 获取 Mapper 代理对象
        userMapper = session.getMapper(UserMapper.class);
    }

    /**
     * 销毁方法（在每个 @Test 方法执行后执行）
     */
    @After
    public void destroy() throws IOException {
        System.out.println("销毁方法执行...");

        // 关闭资源
        if (session != null) {
            session.close();
        }
        if (is != null) {
            is.close();
        }
    }

    /**
     * 测试查询所有用户
     */
    @Test
    public void testFindAll() {
        System.out.println("testFindAll 方法执行...");

        // 执行查询（使用 List<User> 而非裸 List，否则增强 for 循环无法编译）
        List<User> users = userMapper.findAll();

        // 遍历结果
        for (User user : users) {
            System.out.println(user);
        }

        // 断言：用户列表不能为空
        Assert.assertNotNull("用户列表不能为空", users);
        Assert.assertTrue("用户列表必须有数据", users.size() > 0);
    }

    /**
     * 测试根据 ID 查询用户
     */
    @Test
    public void testFindById() {
        System.out.println("testFindById 方法执行...");

        // 执行查询
        User user = userMapper.findById(1);

        // 打印结果
        System.out.println(user);

        // 断言
        Assert.assertNotNull("用户不能为空", user);
        Assert.assertEquals("用户ID必须是1", Integer.valueOf(1), user.getId());
    }
}
