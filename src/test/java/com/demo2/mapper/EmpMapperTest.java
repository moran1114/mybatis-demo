package com.demo2.mapper;

import com.demo2.entity.Emp;
import com.demo2.entity.EmpQuery;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EmpMapperTest {
    private static SqlSessionFactory sessionFactory;

    private SqlSession session;
    private EmpMapper mapper;

    @BeforeClass
    public static void createDatabase() {
        String databaseName = "classic_mybatis";
        EmbeddedDatabase.initialize(databaseName);
        sessionFactory = MyBatisUtil.buildMyBatis(EmbeddedDatabase.properties(databaseName));
        sessionFactory.getConfiguration().addMapper(CamelCaseMapper.class);
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
    public void completesCrudAndReturnsGeneratedKey() {
        Assert.assertEquals(8, mapper.selectAllEmps().size());

        Emp emp = emp("Test Member", 40, "Test Engineer", "8900.00");
        Assert.assertEquals(1, mapper.insertEmp(emp));
        Assert.assertNotNull(emp.getEmpno());

        Emp stored = mapper.selectEmpById(emp.getEmpno());
        Assert.assertEquals("Test Member", stored.getEname());
        Assert.assertEquals(Long.valueOf(6), stored.getMgr());
        Assert.assertEquals(Integer.valueOf(40), stored.getDeptno());
        Assert.assertEquals(LocalDate.of(2025, 1, 6), stored.getHiredate());
        Assert.assertNull(stored.getComm());

        stored.setJob("Test Developer");
        stored.setSal(new BigDecimal("9900.00"));
        Assert.assertEquals(1, mapper.updateEmp(stored));
        Assert.assertEquals("Test Developer", mapper.selectEmpById(emp.getEmpno()).getJob());

        Assert.assertEquals(1, mapper.deleteEmpById(emp.getEmpno()));
        Assert.assertNull(mapper.selectEmpById(emp.getEmpno()));
    }

    @Test
    public void combinesFuzzyNameDepartmentAndOtherConditions() {
        Emp search = new Emp();
        search.setDeptno(20);
        search.setSal(new BigDecimal("14000.00"));
        Assert.assertEquals(2, mapper.selectByCondition(search).size());

        Emp narrowSearch = new Emp();
        narrowSearch.setEname("Lin");
        narrowSearch.setJob("Engineer");
        narrowSearch.setDeptno(20);
        List<Emp> narrowResult = mapper.selectByCondition(narrowSearch);
        Assert.assertEquals(1, narrowResult.size());
        Assert.assertEquals("Lin Han", narrowResult.get(0).getEname());

        narrowSearch.setDeptno(30);
        Assert.assertTrue(mapper.selectByCondition(narrowSearch).isEmpty());
    }

    @Test
    public void handlesMissingConditions() {
        Assert.assertEquals(8, mapper.selectByCondition(null).size());
        Emp query = new Emp();
        query.setEname("");
        query.setJob("");
        Assert.assertEquals(8, mapper.selectByCondition(query).size());
        query.setDeptno(10);
        Assert.assertEquals(2, mapper.selectByCondition(query).size());
    }

    @Test
    public void createsMapperProxyAndMapsUnderscoreColumnToCamelCase() {
        Assert.assertTrue(Proxy.isProxyClass(mapper.getClass()));
        Assert.assertTrue(sessionFactory.getConfiguration().isMapUnderscoreToCamelCase());
        NameView result = session.getMapper(CamelCaseMapper.class).selectName(1L);
        Assert.assertEquals("Lin Han", result.getEmpName());
    }

    @Test
    public void trimMatchesWhereForCombinedConditions() {
        Emp departmentOnly = new Emp();
        departmentOnly.setDeptno(20);

        Emp combined = new Emp();
        combined.setEname("Lin");
        combined.setJob("Engineer");
        combined.setDeptno(20);
        combined.setSal(new BigDecimal("14000"));

        for (Emp condition : Arrays.asList(null, new Emp(), departmentOnly, combined)) {
            List<Long> whereIds = mapper.selectByCondition(condition).stream()
                    .map(Emp::getEmpno)
                    .collect(Collectors.toList());
            List<Long> trimIds = mapper.selectByConditionWithTrim(condition).stream()
                    .map(Emp::getEmpno)
                    .collect(Collectors.toList());
            Assert.assertEquals(whereIds, trimIds);
        }
    }

    @Test
    public void bindsInjectionTextAsAValueAndRestrictsSortColumns() {
        Emp condition = new Emp();
        condition.setEname("' OR 1=1 --");
        Assert.assertTrue(mapper.selectByCondition(condition).isEmpty());
        Assert.assertTrue(mapper.selectPreferred("' OR 1=1 --", 20).isEmpty());
        Assert.assertEquals(3, mapper.countByDeptno(20));
        Assert.assertEquals(0, mapper.countByDeptno(99));
        Assert.assertEquals(Long.valueOf(6), mapper.selectSorted(EmpQuery.SortColumn.SAL).get(0).getEmpno());
        Assert.assertEquals(Long.valueOf(8), mapper.selectSorted(EmpQuery.SortColumn.HIREDATE).get(0).getEmpno());
        Assert.assertEquals(Long.valueOf(8), mapper.selectSorted(null).get(0).getEmpno());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> EmpQuery.SortColumn.valueOf("sal; DELETE FROM emp"));
    }

    @Test
    public void updatesOnlySuppliedFieldsWithSet() {
        Emp patch = new Emp();
        patch.setEmpno(1L);
        patch.setSal(new BigDecimal("15100.00"));
        patch.setComm(BigDecimal.ZERO);
        Assert.assertEquals(1, mapper.updatePartial(patch));

        Emp changed = mapper.selectEmpById(1L);
        Assert.assertEquals(0, changed.getSal().compareTo(new BigDecimal("15100.00")));
        Assert.assertEquals(0, changed.getComm().compareTo(BigDecimal.ZERO));
        Assert.assertEquals("Lin Han", changed.getEname());
        Assert.assertEquals(Integer.valueOf(20), changed.getDeptno());
        Assert.assertEquals("Engineer", changed.getJob());
        Assert.assertEquals(LocalDate.of(2023, 10, 9), changed.getHiredate());
    }

    @Test
    public void insertsAndDeletesBatchesWithForeach() {
        List<Emp> emps = Arrays.asList(
                emp("Batch A", 50, "Coordinator", "8200.00"),
                emp("Batch B", 50, "Support", "8600.00"));
        Assert.assertEquals(2, mapper.insertBatch(emps));

        Assert.assertEquals(1, mapper.selectPreferred("Batch A", 20).size());
        List<Emp> unitResult = mapper.selectPreferred("", 50);
        Assert.assertEquals(2, unitResult.size());
        Assert.assertEquals(10, mapper.selectPreferred("", null).size());

        List<Long> ids = unitResult.stream().map(Emp::getEmpno).collect(Collectors.toList());
        Assert.assertEquals(2, mapper.deleteBatch(ids));
        Assert.assertTrue(mapper.selectPreferred("", 50).isEmpty());
        Assert.assertEquals(8, mapper.selectAllEmps().size());
    }

    @Test
    public void emptyBatchDeleteDoesNotDeleteEmployees() {
        Assert.assertEquals(0, mapper.deleteBatch(Collections.emptyList()));
        Assert.assertEquals(0, mapper.deleteBatch(null));
        Assert.assertEquals(8, mapper.selectAllEmps().size());
    }

    @Test
    public void chooseUsesNameThenDepartmentThenAllWithoutCombiningConditions() {
        List<Emp> byName = mapper.selectPreferred("Lin Han", 30);
        Assert.assertEquals(1, byName.size());
        Assert.assertEquals("Lin Han", byName.get(0).getEname());
        Assert.assertTrue(mapper.selectPreferred("Lin", 20).isEmpty());
        Assert.assertEquals(3, mapper.selectPreferred(null, 20).size());
        Assert.assertEquals(3, mapper.selectPreferred("", 20).size());
        Assert.assertEquals(8, mapper.selectPreferred(null, null).size());
        Assert.assertEquals(8, mapper.selectPreferred("", null).size());

        Emp query = new Emp();
        query.setEname("Lin Han");
        query.setDeptno(30);
        Assert.assertTrue(mapper.selectByCondition(query).isEmpty());
    }

    public interface CamelCaseMapper {
        @Select("SELECT ename AS emp_name FROM emp WHERE empno = #{empno}")
        NameView selectName(@Param("empno") Long empno);
    }

    public static class NameView {
        private String empName;

        public String getEmpName() {
            return empName;
        }

        public void setEmpName(String empName) {
            this.empName = empName;
        }
    }

    private static Emp emp(String ename, Integer deptno, String job, String pay) {
        Emp emp = new Emp();
        emp.setEname(ename);
        emp.setDeptno(deptno);
        emp.setJob(job);
        emp.setSal(new BigDecimal(pay));
        emp.setHiredate(LocalDate.of(2025, 1, 6));
        emp.setMgr(6L);
        return emp;
    }
}
