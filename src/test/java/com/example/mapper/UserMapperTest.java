package com.example.mapper;

import com.example.entity.User;
import com.example.util.SqlSessionUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * UserMapper 测试类
 */
public class UserMapperTest {

    private SqlSession sqlSession;
    private UserMapper userMapper;

    @Before
    public void setUp() {
        sqlSession = SqlSessionUtil.openSession();
        userMapper = sqlSession.getMapper(UserMapper.class);
    }

    @After
    public void tearDown() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    /** 查询所有用户 */
    @Test
    public void testFindAll() {
        List<User> users = userMapper.findAll();
        System.out.println("用户总数：" + users.size());
        users.forEach(System.out::println);
        Assert.assertNotNull(users);
    }

    /** 根据 id 查询用户 */
    @Test
    public void testFindById() {
        List<User> users = userMapper.findAll();
        if (!users.isEmpty()) {
            User first = users.get(0);
            User user = userMapper.findById(first.getId());
            Assert.assertNotNull(user);
            System.out.println(user);
        } else {
            System.out.println("user 表为空，跳过 findById 测试");
        }
    }

    /** 新增用户（username/email 唯一，用时间戳保证不重复） */
    @Test
    public void testInsert() {
        String unique = String.valueOf(System.currentTimeMillis());
        User user = new User();
        user.setUsername("test_" + unique);
        user.setPassword("123456");
        user.setEmail("test_" + unique + "@example.com");

        int rows = userMapper.insert(user);
        sqlSession.commit();

        System.out.println("插入影响行数：" + rows + "，回填的自增 id = " + user.getId());
        Assert.assertEquals(1, rows);
        Assert.assertNotNull(user.getId());
    }

    /** 修改用户 */
    @Test
    public void testUpdate() {
        List<User> users = userMapper.findAll();
        if (users.isEmpty()) {
            System.out.println("user 表为空，跳过 update 测试");
            return;
        }
        User user = users.get(0);
        user.setEmail("updated_" + System.currentTimeMillis() + "@example.com");
        int rows = userMapper.update(user);
        sqlSession.commit();
        System.out.println("更新影响行数：" + rows);
        Assert.assertEquals(1, rows);
    }

    /** 删除用户：先插入一条再删除 */
    @Test
    public void testDeleteById() {
        String unique = String.valueOf(System.currentTimeMillis());
        User user = new User();
        user.setUsername("del_" + unique);
        user.setPassword("123456");
        user.setEmail("del_" + unique + "@example.com");
        userMapper.insert(user);
        sqlSession.commit();

        int rows = userMapper.deleteById(user.getId());
        sqlSession.commit();
        System.out.println("删除影响行数：" + rows + "，删除的 id = " + user.getId());
        Assert.assertEquals(1, rows);
    }
}
