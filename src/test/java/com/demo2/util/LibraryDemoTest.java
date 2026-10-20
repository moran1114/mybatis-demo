package com.demo2.util;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo2.entity.Book;
import com.demo2.mapper.BookMapper;
import com.demo2.support.EmbeddedDatabase;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LibraryDemoTest {
    private static SqlSessionFactory factory;

    @BeforeClass
    public static void setup() {
        EmbeddedDatabase.initialize("library_demo");
        factory = MyBatisUtil.build(EmbeddedDatabase.properties("library_demo"), DbType.H2);
    }

    @Test
    public void missingIdsAndInvalidInputHaveFriendlyFeedback() throws Exception {
        String output = run("2\n999\n1\n99\n5\n999\n4\n999\n2\nabc\n1\n0\n");
        Assert.assertTrue(output.contains("编号 999 的书籍不存在，请检查 book 表的 book_id"));
        Assert.assertTrue(output.contains("编号 99 的出版社不存在，请检查 publisher 表的 publisher_id"));
        Assert.assertTrue(output.contains("编号 999 的作者不存在，请检查 author 表的 author_id"));
        Assert.assertTrue(output.contains("编号必须是正整数，请重新输入"));
        Assert.assertTrue(output.contains("Java Foundations"));
    }

    @Test
    public void conditionAggregationAndPaginationHaveSeparateMenus() throws Exception {
        String output = run("6\n7\n8\n0\n");
        Assert.assertTrue(output.contains("【条件查询】QueryWrapper"));
        Assert.assertTrue(output.contains("LambdaQueryWrapper"));
        Assert.assertTrue(output.contains("select() 只取编号和书名"));
        Assert.assertTrue(output.contains("【聚合查询】"));
        Assert.assertTrue(output.contains("selectCount"));
        Assert.assertTrue(output.contains("selectMaps"));
        Assert.assertTrue(output.contains("selectByMap"));
        Assert.assertTrue(output.contains("【物理分页】每页 3 本"));
        Assert.assertTrue(output.contains("第 2 页：总数=8"));
        Assert.assertTrue(output.contains("菜单 6–8 使用预设参数展示查询效果，不接收自定义输入"));
        Assert.assertEquals(3, output.split("【仅作演示】本项参数已写在示例代码中", -1).length - 1);
        Assert.assertFalse(Logger.getLogger("com.demo2.mapper.BookMapper").isDebugEnabled());
        Assert.assertEquals(Level.INFO, Logger.getLogger("com.demo2.mapper.BookMapper").getEffectiveLevel());
    }

    @Test
    public void lifecycleMenusExplainResultsAndRollbackEachDemo() throws Exception {
        String output = run("9\n10\n11\n12\n0\n");
        Assert.assertTrue(output.contains("MetaObjectHandler 在 INSERT 填 createTime/updateTime"));
        Assert.assertTrue(output.contains("实体部分更新，discount=null 后数据库折扣仍为 5.00"));
        Assert.assertTrue(output.contains("LambdaUpdateWrapper.set(discount, null) 后折扣为 null"));
        Assert.assertTrue(output.contains("数据库原始行仍在，deleted=1"));
        Assert.assertTrue(output.contains("旧版本再更新：影响行数=0"));
        Assert.assertTrue(output.contains("重新读取后重试：影响行数=1"));
        Assert.assertEquals(4, output.split("本次演示事务已回滚", -1).length - 1);
        try (SqlSession check = factory.openSession()) {
            Assert.assertEquals(Long.valueOf(0), check.getMapper(BookMapper.class)
                    .selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getCategory, "Demo")));
        }
    }

    private static String run(String input) throws Exception {
        try (SqlSession session = factory.openSession(false);
             Scanner scanner = new Scanner(input);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8.name())) {
            LibraryDemo.run(session, scanner, out);
            session.rollback();
            return bytes.toString(StandardCharsets.UTF_8.name());
        }
    }
}
