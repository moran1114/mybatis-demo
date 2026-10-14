package com.demo2.mapper;

import com.demo2.entity.Dept;
import com.demo2.entity.Emp;
import com.demo2.entity.Project;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RelationQueryTest {
    private static SqlSessionFactory sessionFactory;

    private SqlSession session;
    private EmpMapper empMapper;
    private DeptMapper deptMapper;
    private ProjectMapper projectMapper;

    @BeforeClass
    public static void createDatabase() {
        String databaseName = "relation_query";
        EmbeddedDatabase.initialize(databaseName);
        sessionFactory = MyBatisUtil.buildMyBatis(EmbeddedDatabase.properties(databaseName));
    }

    @Before
    public void openSession() {
        session = sessionFactory.openSession(false);
        empMapper = session.getMapper(EmpMapper.class);
        deptMapper = session.getMapper(DeptMapper.class);
        projectMapper = session.getMapper(ProjectMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void deptWithEmpsCollectsEmployeesOfDept20() {
        Dept dept = deptMapper.selectDeptWithEmps(20);
        Assert.assertNotNull(dept);
        Assert.assertEquals("Technology", dept.getDname());
        Assert.assertEquals(3, dept.getEmps().size());
        Assert.assertEquals(Arrays.asList(1L, 3L, 6L),
                dept.getEmps().stream().map(Emp::getEmpno).collect(Collectors.toList()));
        for (Emp emp : dept.getEmps()) {
            Assert.assertEquals(Integer.valueOf(20), emp.getDeptno());
        }
    }

    @Test
    public void deptWithoutEmployeesReturnsEmptyCollection() {
        Dept dept = deptMapper.selectDeptWithEmps(40);
        Assert.assertNotNull(dept);
        Assert.assertEquals("QA", dept.getDname());
        Assert.assertTrue(dept.getEmps().isEmpty());
        Assert.assertNull(deptMapper.selectDeptWithEmps(99));
    }

    @Test
    public void empWithDetailMapsOneToOne() {
        Emp emp = empMapper.selectEmpWithDetail(1L);
        Assert.assertNotNull(emp);
        Assert.assertEquals("Lin Han", emp.getEname());
        Assert.assertEquals(Integer.valueOf(20), emp.getDeptno());
        Assert.assertNotNull(emp.getDetail());
        Assert.assertEquals(Long.valueOf(1), emp.getDetail().getEmpno());
        Assert.assertEquals("13800000001", emp.getDetail().getPhone());
        Assert.assertEquals("lin.han@example.com", emp.getDetail().getEmail());
        Assert.assertEquals("Shanghai, Pudong", emp.getDetail().getAddress());
        Assert.assertEquals("Wang Lan", emp.getDetail().getEmergencyContact());
    }

    @Test
    public void empWithoutDetailLeavesDetailNull() {
        Emp emp = empMapper.selectEmpWithDetail(8L);
        Assert.assertNotNull(emp);
        Assert.assertEquals("Wu Tong", emp.getEname());
        Assert.assertNull(emp.getDetail());
        Assert.assertNull(empMapper.selectEmpWithDetail(99L));
    }

    @Test
    public void empWithProjectsCollectsProjects() {
        Emp emp = empMapper.selectEmpWithProjects(1L);
        Assert.assertNotNull(emp);
        Assert.assertEquals(2, emp.getProjects().size());
        Assert.assertEquals(Arrays.asList(101, 102),
                emp.getProjects().stream().map(Project::getProjectId).collect(Collectors.toList()));
        Assert.assertEquals("Payment Gateway Refactor", emp.getProjects().get(0).getName());
        Assert.assertEquals("Data Platform Construction", emp.getProjects().get(1).getName());

        Emp withoutProjects = empMapper.selectEmpWithProjects(5L);
        Assert.assertNotNull(withoutProjects);
        Assert.assertTrue(withoutProjects.getProjects().isEmpty());
        Assert.assertNull(empMapper.selectEmpWithProjects(99L));
    }

    @Test
    public void projectWithMembersAndCrossCheck() {
        Project project = projectMapper.selectProjectWithMembers(102);
        Assert.assertNotNull(project);
        Assert.assertEquals("Data Platform Construction", project.getName());
        Assert.assertEquals(3, project.getMembers().size());
        Assert.assertEquals(Arrays.asList(1L, 3L, 6L),
                project.getMembers().stream().map(Emp::getEmpno).collect(Collectors.toList()));

        Assert.assertEquals(Arrays.asList(1L, 6L), projectMapper.selectProjectWithMembers(101)
                .getMembers().stream().map(Emp::getEmpno).collect(Collectors.toList()));
        Assert.assertNull(projectMapper.selectProjectWithMembers(999));

        List<Integer> emp1ProjectIds = empMapper.selectEmpWithProjects(1L)
                .getProjects().stream().map(Project::getProjectId).collect(Collectors.toList());
        Assert.assertTrue(emp1ProjectIds.contains(101));
        List<Long> project101MemberNos = projectMapper.selectProjectWithMembers(101)
                .getMembers().stream().map(Emp::getEmpno).collect(Collectors.toList());
        Assert.assertTrue(project101MemberNos.contains(1L));
    }
}
