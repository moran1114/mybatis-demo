package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo2.entity.Emp;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisPlusQueryDemo;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmpAdvancedQueryTest {
    private static SqlSessionFactory factory;
    private SqlSession session;
    private EmpMapper mapper;

    @BeforeClass
    public static void createDatabase() {
        EmbeddedDatabase.initialize("advanced_query");
        factory = MyBatisUtil.build(EmbeddedDatabase.properties("advanced_query"), DbType.H2);
    }

    @Before
    public void openSession() {
        session = factory.openSession(false);
        mapper = session.getMapper(EmpMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void chainsConditionsAndIncludesBothBetweenBoundaries() {
        List<Emp> records = mapper.selectList(new QueryWrapper<Emp>()
                .in("deptno", 20, 30).between("sal", new BigDecimal("8000"), new BigDecimal("20000"))
                .orderByDesc("sal").orderByAsc("empno"));
        Assert.assertEquals(Arrays.asList(7L, 1L, 3L, 2L), ids(records));
    }

    @Test
    public void nestsOrWithoutLeakingEmployeesFromOtherDepartments() {
        List<Emp> records = mapper.selectList(new QueryWrapper<Emp>().eq("deptno", 20)
                .and(w -> w.like("ename", "Lin").or().ge("sal", new BigDecimal("20000")))
                .orderByAsc("empno"));
        Assert.assertEquals(Arrays.asList(1L, 6L), ids(records));

        List<Emp> alternatives = mapper.selectList(Wrappers.lambdaQuery(Emp.class)
                .and(w -> w.eq(Emp::getDeptno, 20).ge(Emp::getSal, new BigDecimal("14000")))
                .or(w -> w.eq(Emp::getDeptno, 30).isNotNull(Emp::getComm))
                .orderByAsc(Emp::getEmpno));
        Assert.assertEquals(Arrays.asList(1L, 2L, 4L, 6L, 7L), ids(alternatives));
    }

    @Test
    public void omitsAbsentOptionalLambdaConditions() {
        Integer deptno = null;
        String name = " ";
        BigDecimal minSal = new BigDecimal("20000");
        List<Emp> records = mapper.selectList(Wrappers.lambdaQuery(Emp.class)
                .eq(deptno != null, Emp::getDeptno, deptno)
                .like(name != null && !name.isBlank(), Emp::getEname, name)
                .ge(minSal != null, Emp::getSal, minSal).orderByAsc(Emp::getEmpno));
        Assert.assertEquals(Arrays.asList(6L, 7L), ids(records));
        Assert.assertEquals(Long.valueOf(8), mapper.selectCount(Wrappers.lambdaQuery(Emp.class)));
    }

    @Test
    public void distinguishesCountAndSelectOneCardinality() {
        Assert.assertEquals(Long.valueOf(3), mapper.selectCount(
                Wrappers.lambdaQuery(Emp.class).eq(Emp::getDeptno, 20)));
        Assert.assertEquals("Lin Han", mapper.selectOne(
                Wrappers.lambdaQuery(Emp.class).eq(Emp::getEmpno, 1L)).getEname());
        Assert.assertNull(mapper.selectOne(Wrappers.lambdaQuery(Emp.class).eq(Emp::getEmpno, -1L)));
        Assert.assertThrows(TooManyResultsException.class, () -> mapper.selectOne(
                Wrappers.lambdaQuery(Emp.class).eq(Emp::getDeptno, 20)));
    }

    @Test
    public void projectsOnlyRequestedColumnsAndMapAliases() {
        Emp projection = mapper.selectOne(Wrappers.lambdaQuery(Emp.class)
                .select(Emp::getEname, Emp::getSal, Emp::getDeptno).eq(Emp::getEmpno, 1L));
        Assert.assertEquals("Lin Han", projection.getEname());
        Assert.assertNull(projection.getEmpno());
        Assert.assertNull(projection.getJob());
        Assert.assertNull(projection.getComm());
        Map<String, Object> row = mapper.selectMaps(new QueryWrapper<Emp>()
                .select("ename AS employee_name", "sal", "deptno").eq("empno", 1L)).get(0);
        Assert.assertEquals("Lin Han", row.get("employee_name"));
        Assert.assertEquals(3, row.size());
        Assert.assertEquals(20, ((Number) row.get("deptno")).intValue());
    }

    @Test
    public void groupsPayrollAndFiltersAggregatesWithHaving() {
        List<Map<String, Object>> groups = mapper.selectMaps(new QueryWrapper<Emp>()
                .select("deptno", "COUNT(*) AS emp_count", "SUM(sal) AS total_sal", "AVG(sal) AS avg_sal")
                .groupBy("deptno").having("COUNT(*) >= {0}", 3).orderByAsc("deptno"));
        Assert.assertEquals(2, groups.size());
        Assert.assertEquals(20, ((Number) groups.get(0).get("deptno")).intValue());
        Assert.assertEquals(3L, ((Number) groups.get(0).get("emp_count")).longValue());
        Assert.assertEquals(0, new BigDecimal("46500").compareTo((BigDecimal) groups.get(0).get("total_sal")));
        Assert.assertEquals(0, new BigDecimal("15500").compareTo((BigDecimal) groups.get(0).get("avg_sal")));
        Assert.assertEquals(30, ((Number) groups.get(1).get("deptno")).intValue());
        Assert.assertEquals(0, new BigDecimal("35000").compareTo((BigDecimal) groups.get(1).get("total_sal")));
    }

    @Test
    public void selectsByMapUsingAndAndIsNull() {
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("deptno", 20);
        columns.put("comm", null);
        Assert.assertEquals(Arrays.asList(1L, 3L), ids(mapper.selectByMap(columns)).stream()
                .sorted().collect(Collectors.toList()));
    }

    @Test
    public void lambdaUpdateChangesOnlyMatchingEmployees() {
        Assert.assertEquals(2, mapper.update(null, Wrappers.lambdaUpdate(Emp.class)
                .eq(Emp::getDeptno, 30).lt(Emp::getSal, new BigDecimal("10000"))
                .set(Emp::getComm, BigDecimal.ZERO)));
        Assert.assertEquals(0, mapper.selectById(2L).getComm().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, mapper.selectById(4L).getComm().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, mapper.selectById(7L).getComm().compareTo(new BigDecimal("2000")));
        Assert.assertNull(mapper.selectById(1L).getComm());
    }

    @Test
    public void nullEntityValuePreservesStoredCommission() {
        Emp patch = new Emp();
        patch.setEmpno(2L);
        patch.setJob("Senior Salesperson");
        patch.setComm(null);
        Assert.assertEquals(1, mapper.updateById(patch));
        Emp stored = mapper.selectById(2L);
        Assert.assertEquals("Senior Salesperson", stored.getJob());
        Assert.assertEquals("Tang Ke", stored.getEname());
        Assert.assertEquals(0, stored.getComm().compareTo(new BigDecimal("1500")));
    }

    @Test
    public void zeroIsUpdatedAndExplicitWrapperNullClearsCommission() {
        Emp patch = new Emp();
        patch.setEmpno(2L);
        patch.setComm(BigDecimal.ZERO);
        Assert.assertEquals(1, mapper.updateById(patch));
        Assert.assertEquals(0, mapper.selectById(2L).getComm().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(1, mapper.update(null, Wrappers.lambdaUpdate(Emp.class)
                .eq(Emp::getEmpno, 2L).set(Emp::getComm, null)));
        Assert.assertNull(mapper.selectById(2L).getComm());
    }

    @Test
    public void runsAllSecondChapterExamples() {
        MyBatisPlusQueryDemo.run(mapper);
        Assert.assertEquals(Long.valueOf(8), mapper.selectCount(null));
    }

    private static List<Long> ids(List<Emp> employees) {
        return employees.stream().map(Emp::getEmpno).collect(Collectors.toList());
    }
}
