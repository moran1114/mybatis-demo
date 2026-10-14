package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.EmpLifecycle;
import com.demo2.handler.AuditMetaObjectHandler;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisPlusLifecycleDemo;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmpLifecycleTest {
    private static final Instant INSERT_TIME = Instant.parse("2025-01-01T10:00:00Z");
    private static SqlSessionFactory factory;
    private SqlSession session;
    private EmpLifecycleMapper mapper;

    @BeforeClass
    public static void createDatabase() {
        EmbeddedDatabase.initialize("emp_lifecycle");
        factory = MyBatisUtil.build(EmbeddedDatabase.properties("emp_lifecycle"), DbType.H2);
    }

    @Before
    public void openSession() {
        useClock(INSERT_TIME);
        session = factory.openSession(false);
        mapper = session.getMapper(EmpLifecycleMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void fillsInsertTimesAndUsesDatabaseDefaultsForLifecycleFlags() {
        EmpLifecycle employee = insertEmployee("Created");
        LocalDateTime expected = LocalDateTime.ofInstant(INSERT_TIME, ZoneOffset.UTC);
        Assert.assertEquals(expected, employee.getCreateTime());
        Assert.assertEquals(expected, employee.getUpdateTime());
        EmpLifecycle stored = mapper.selectById(employee.getEmpno());
        Assert.assertEquals(expected, stored.getCreateTime());
        Assert.assertEquals(expected, stored.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(0), stored.getDeleted());
        Assert.assertEquals(Integer.valueOf(0), stored.getVersion());
    }

    @Test
    public void refreshesExistingUpdateTimeAndKeepsCreationTimeImmutable() {
        EmpLifecycle employee = mapper.selectById(insertEmployee("Updated").getEmpno());
        LocalDateTime originalCreation = employee.getCreateTime();
        useClock(INSERT_TIME.plusSeconds(60));
        employee.setCreateTime(originalCreation.minusYears(1));
        employee.setSal(new BigDecimal("13000"));
        Assert.assertEquals(1, mapper.updateById(employee));
        EmpLifecycle stored = mapper.selectById(employee.getEmpno());
        Assert.assertEquals(originalCreation, stored.getCreateTime());
        Assert.assertEquals(originalCreation.plusSeconds(60), stored.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
    }

    @Test
    public void logicalDeleteKeepsPhysicalRowButExcludesItFromGeneratedSql() throws Exception {
        Long id = insertEmployee("Deleted").getEmpno();
        EmpLifecycle beforeDelete = mapper.selectById(id);
        Assert.assertEquals(1, mapper.deleteById(id));
        Assert.assertNull(mapper.selectById(id));
        Assert.assertTrue(mapper.selectList(null).isEmpty());
        Assert.assertTrue(mapper.selectByMap(Map.of("empno", id)).isEmpty());
        Assert.assertEquals(Long.valueOf(0), mapper.selectCount(null));
        Assert.assertEquals(0, mapper.deleteById(id));
        beforeDelete.setEname("Cannot resurrect");
        Assert.assertEquals(0, mapper.updateById(beforeDelete));
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT ename, deleted, version, update_time FROM emp_lifecycle WHERE empno = ?")) {
            statement.setLong(1, id);
            try (ResultSet row = statement.executeQuery()) {
                Assert.assertTrue(row.next());
                Assert.assertEquals("Deleted", row.getString("ename"));
                Assert.assertEquals(1, row.getInt("deleted"));
                // 当前版本的 deleteById(id) 不做版本递增，也不填充更新时间。
                Assert.assertEquals(0, row.getInt("version"));
                Assert.assertEquals(beforeDelete.getCreateTime(), row.getTimestamp("update_time").toLocalDateTime());
            }
        }
    }

    @Test
    public void rejectsStaleSnapshotAndAllowsAnExplicitReloadedUpdate() {
        Long id = insertEmployee("Concurrent").getEmpno();
        EmpLifecycle first = mapper.selectById(id);
        session.clearCache();
        EmpLifecycle stale = mapper.selectById(id);
        Assert.assertNotSame(first, stale);
        first.setSal(new BigDecimal("13000"));
        Assert.assertEquals(1, mapper.updateById(first));
        Assert.assertEquals(Integer.valueOf(1), first.getVersion());
        stale.setSal(new BigDecimal("14000"));
        Assert.assertEquals(0, mapper.updateById(stale));
        EmpLifecycle current = mapper.selectById(id);
        Assert.assertEquals(0, current.getSal().compareTo(new BigDecimal("13000")));
        Assert.assertEquals(Integer.valueOf(1), current.getVersion());
        // 冲突后重新读取；业务确认后只合并所需字段，不直接重用旧实体。
        current.setEname("Confirmed name");
        Assert.assertEquals(1, mapper.updateById(current));
        EmpLifecycle confirmed = mapper.selectById(id);
        Assert.assertEquals(Integer.valueOf(2), confirmed.getVersion());
        Assert.assertEquals(0, confirmed.getSal().compareTo(new BigDecimal("13000")));
    }

    @Test
    public void entityAndFreshWrapperEnableVersionCheckAndAutoFill() {
        Long id = insertEmployee("Wrapper entity").getEmpno();
        EmpLifecycle patch = new EmpLifecycle();
        patch.setVersion(0);
        patch.setSal(new BigDecimal("13500"));
        useClock(INSERT_TIME.plusSeconds(120));
        Assert.assertEquals(1, mapper.update(patch, Wrappers.lambdaUpdate(EmpLifecycle.class)
                .eq(EmpLifecycle::getEmpno, id)));
        EmpLifecycle stored = mapper.selectById(id);
        Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
        Assert.assertEquals(stored.getCreateTime().plusSeconds(120), stored.getUpdateTime());
        EmpLifecycle stale = new EmpLifecycle();
        stale.setVersion(0);
        stale.setSal(new BigDecimal("99999"));
        Assert.assertEquals(0, mapper.update(stale, Wrappers.lambdaUpdate(EmpLifecycle.class)
                .eq(EmpLifecycle::getEmpno, id)));
        Assert.assertEquals(0, mapper.selectById(id).getSal().compareTo(new BigDecimal("13500")));
    }

    @Test
    public void wrapperOnlyUpdateDoesNotAutomaticallyFillTimeOrIncrementVersion() {
        Long id = insertEmployee("Wrapper only").getEmpno();
        useClock(INSERT_TIME.plusSeconds(180));
        Assert.assertEquals(1, mapper.update(null, Wrappers.lambdaUpdate(EmpLifecycle.class)
                .eq(EmpLifecycle::getEmpno, id).set(EmpLifecycle::getSal, new BigDecimal("15000"))));
        EmpLifecycle stored = mapper.selectById(id);
        Assert.assertEquals(stored.getCreateTime(), stored.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(0), stored.getVersion());
        Assert.assertEquals(0, stored.getSal().compareTo(new BigDecimal("15000")));
    }

    @Test
    public void paginationCountsOnlyActiveRowsAndHandlesOverflowAndSizeLimit() {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ids.add(insertEmployee("Page " + i).getEmpno());
        }
        mapper.deleteById(ids.remove(1));
        Page<EmpLifecycle> first = page(1, 2);
        Page<EmpLifecycle> second = page(2, 2);
        Assert.assertEquals(4L, first.getTotal());
        Assert.assertEquals(2L, first.getPages());
        Assert.assertEquals(ids.subList(0, 2), employeeIds(first));
        Assert.assertEquals(ids.subList(2, 4), employeeIds(second));
        Page<EmpLifecycle> beyond = page(3, 2);
        Assert.assertTrue(beyond.getRecords().isEmpty());
        Assert.assertEquals(4L, beyond.getTotal());
        Assert.assertEquals(100L, page(1, 1000).getSize());
    }

    @Test
    public void runsAllThirdChapterExamples() throws Exception {
        MyBatisPlusLifecycleDemo.run(session);
        Assert.assertEquals(Long.valueOf(1), mapper.selectCount(null));
    }

    private EmpLifecycle insertEmployee(String name) {
        EmpLifecycle employee = new EmpLifecycle();
        employee.setEname(name);
        employee.setDeptno(20);
        employee.setSal(new BigDecimal("12000"));
        Assert.assertEquals(1, mapper.insert(employee));
        return employee;
    }

    private Page<EmpLifecycle> page(long current, long size) {
        return mapper.selectPage(Page.of(current, size), Wrappers.lambdaQuery(EmpLifecycle.class)
                .eq(EmpLifecycle::getDeptno, 20).orderByAsc(EmpLifecycle::getEmpno));
    }

    private static List<Long> employeeIds(Page<EmpLifecycle> page) {
        return page.getRecords().stream().map(EmpLifecycle::getEmpno).collect(Collectors.toList());
    }

    private static void useClock(Instant instant) {
        GlobalConfigUtils.getGlobalConfig(factory.getConfiguration())
                .setMetaObjectHandler(new AuditMetaObjectHandler(Clock.fixed(instant, ZoneOffset.UTC)));
    }
}
