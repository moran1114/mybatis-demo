package dev.course.persistence.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.course.persistence.entity.StaffProfile;
import dev.course.persistence.support.EmbeddedDatabase;
import dev.course.persistence.util.MyBatisSessions;
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

public class MyBatisPlusFeatureTest {

    private static SqlSessionFactory sessionFactory;

    private SqlSession session;
    private StaffProfileMapper mapper;

    @BeforeClass
    public static void createDatabase() {
        String databaseName = "mybatis_plus";
        EmbeddedDatabase.initialize(databaseName);
        sessionFactory = MyBatisSessions.build(
                EmbeddedDatabase.properties(databaseName), DbType.H2);
    }

    @Before
    public void openSession() {
        session = sessionFactory.openSession(false);
        mapper = session.getMapper(StaffProfileMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void performsBaseMapperCrud() {
        StaffProfile profile = profile();
        Assert.assertEquals(1, mapper.insert(profile));
        Assert.assertNotNull(profile.getId());
        Assert.assertEquals("增强测试", mapper.selectById(profile.getId()).getName());

        profile.setPositionTitle("高级工具开发");
        Assert.assertEquals(1, mapper.updateById(profile));
        Assert.assertEquals("高级工具开发",
                mapper.selectById(profile.getId()).getPositionTitle());

        Assert.assertEquals(1, mapper.deleteById(profile.getId()));
        Assert.assertNull(mapper.selectById(profile.getId()));
    }

    @Test
    public void appliesLambdaConditionsAndPagination() {
        LambdaQueryWrapper<StaffProfile> wrapper = Wrappers.lambdaQuery(StaffProfile.class)
                .eq(StaffProfile::getUnitName, "云平台组")
                .ge(StaffProfile::getMonthlyPay, new BigDecimal("14000.00"))
                .orderByDesc(StaffProfile::getMonthlyPay);

        List<StaffProfile> records = mapper.selectList(wrapper);
        Assert.assertEquals(2, records.size());
        Assert.assertEquals("沈知", records.get(0).getName());

        Page<StaffProfile> result = mapper.selectPage(Page.of(1, 1), wrapper);
        Assert.assertEquals(2L, result.getTotal());
        Assert.assertEquals(2L, result.getPages());
        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals("沈知", result.getRecords().get(0).getName());
    }

    private static StaffProfile profile() {
        StaffProfile profile = new StaffProfile();
        profile.setName("增强测试");
        profile.setGender("U");
        profile.setUnitName("研发效能组");
        profile.setPositionTitle("工具开发");
        profile.setMonthlyPay(new BigDecimal("12500.00"));
        profile.setEntryDate(LocalDate.of(2025, 2, 17));
        profile.setActiveState(1);
        return profile;
    }
}
