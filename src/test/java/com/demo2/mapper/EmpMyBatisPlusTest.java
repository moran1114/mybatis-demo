package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Emp;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EmpMyBatisPlusTest {
    private static SqlSessionFactory sessionFactory;

    private SqlSession session;
    private EmpMapper mapper;

    @BeforeClass
    public static void createDatabase() {
        String databaseName = "mybatis_plus";
        EmbeddedDatabase.initialize(databaseName);
        sessionFactory = MyBatisUtil.build(EmbeddedDatabase.properties(databaseName), DbType.H2);
    }

    @Before
    public void openSession() {
        session = sessionFactory.openSession(false);
        mapper = session.getMapper(EmpMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void performsBaseMapperCrud() {
        Emp emp = emp();
        Assert.assertEquals(1, mapper.insert(emp));
        Assert.assertNotNull(emp.getEmpno());
        Assert.assertEquals("Plus Test", mapper.selectById(emp.getEmpno()).getEname());

        Assert.assertEquals(0, mapper.selectById(emp.getEmpno()).getComm().compareTo(BigDecimal.ZERO));
        emp.setJob("Senior Tool Developer");
        Assert.assertEquals(1, mapper.updateById(emp));
        Assert.assertEquals("Senior Tool Developer", mapper.selectById(emp.getEmpno()).getJob());

        Assert.assertEquals(1, mapper.deleteById(emp.getEmpno()));
        Assert.assertNull(mapper.selectById(emp.getEmpno()));
    }

    @Test
    public void appliesLambdaConditionsAndPagination() {
        LambdaQueryWrapper<Emp> wrapper = Wrappers.lambdaQuery(Emp.class)
                .eq(Emp::getDeptno, 20)
                .ge(Emp::getSal, new BigDecimal("14000.00"))
                .orderByDesc(Emp::getSal);

        List<Emp> records = mapper.selectList(wrapper);
        Assert.assertEquals(2, records.size());
        Assert.assertEquals("Shen Zhi", records.get(0).getEname());

        Page<Emp> result = mapper.selectPage(Page.of(1, 1), wrapper);
        Assert.assertEquals(2L, result.getTotal());
        Assert.assertEquals(2L, result.getPages());
        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals("Shen Zhi", result.getRecords().get(0).getEname());

        Page<Emp> secondPage = mapper.selectPage(Page.of(2, 1), wrapper);
        Assert.assertEquals(2L, secondPage.getTotal());
        Assert.assertEquals(1, secondPage.getRecords().size());
        Assert.assertEquals("Lin Han", secondPage.getRecords().get(0).getEname());

        Page<Emp> beyondLastPage = mapper.selectPage(Page.of(3, 1), wrapper);
        Assert.assertEquals(2L, beyondLastPage.getTotal());
        Assert.assertTrue(beyondLastPage.getRecords().isEmpty());
    }

    private static Emp emp() {
        Emp emp = new Emp();
        emp.setEname("Plus Test");
        emp.setDeptno(40);
        emp.setJob("Tool Developer");
        emp.setSal(new BigDecimal("12500.00"));
        emp.setHiredate(LocalDate.of(2025, 2, 17));
        emp.setMgr(6L);
        emp.setComm(BigDecimal.ZERO);
        return emp;
    }
}
