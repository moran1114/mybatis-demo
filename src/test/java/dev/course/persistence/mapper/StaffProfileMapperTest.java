package dev.course.persistence.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import dev.course.persistence.entity.StaffProfile;
import dev.course.persistence.query.StaffSearch;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StaffProfileMapperTest {

    private static SqlSessionFactory sessionFactory;

    private SqlSession session;
    private StaffProfileMapper mapper;

    @BeforeClass
    public static void createDatabase() {
        String databaseName = "classic_mybatis";
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
    public void completesCrudAndReturnsGeneratedKey() {
        Assert.assertEquals(6, mapper.selectAllProfiles().size());

        StaffProfile profile = profile("测试成员", "U", "质量保障组",
                "测试工程师", "8900.00");
        Assert.assertEquals(1, mapper.insertProfile(profile));
        Assert.assertNotNull(profile.getId());

        StaffProfile stored = mapper.selectProfileById(profile.getId());
        Assert.assertEquals("测试成员", stored.getName());
        stored.setPositionTitle("测试开发工程师");
        stored.setMonthlyPay(new BigDecimal("9900.00"));
        Assert.assertEquals(1, mapper.updateProfile(stored));
        Assert.assertEquals("测试开发工程师",
                mapper.selectProfileById(profile.getId()).getPositionTitle());

        Assert.assertEquals(1, mapper.deleteProfileById(profile.getId()));
        Assert.assertNull(mapper.selectProfileById(profile.getId()));
    }

    @Test
    public void supportsWhereTrimAndSetTags() {
        StaffSearch search = new StaffSearch();
        search.setUnitName("云平台组");
        search.setMinimumPay(new BigDecimal("14000.00"));

        List<StaffProfile> whereResult = mapper.selectBySearch(search);
        List<StaffProfile> trimResult = mapper.selectBySearchWithTrim(search);
        Assert.assertEquals(2, whereResult.size());
        Assert.assertEquals(
                whereResult.stream().map(StaffProfile::getId).collect(Collectors.toList()),
                trimResult.stream().map(StaffProfile::getId).collect(Collectors.toList()));

        StaffSearch narrowSearch = new StaffSearch();
        narrowSearch.setNameKeyword("林");
        narrowSearch.setGender("M");
        narrowSearch.setActiveState(1);
        List<StaffProfile> narrowResult = mapper.selectBySearch(narrowSearch);
        Assert.assertEquals(1, narrowResult.size());
        Assert.assertEquals("林澈", narrowResult.get(0).getName());

        StaffProfile patch = new StaffProfile();
        patch.setId(1L);
        patch.setMonthlyPay(new BigDecimal("15100.00"));
        patch.setActiveState(0);
        Assert.assertEquals(1, mapper.updatePartial(patch));

        StaffProfile changed = mapper.selectProfileById(1L);
        Assert.assertEquals(0, changed.getMonthlyPay().compareTo(new BigDecimal("15100.00")));
        Assert.assertEquals(Integer.valueOf(0), changed.getActiveState());
        Assert.assertEquals("林澈", changed.getName());
    }

    @Test
    public void supportsForeachAndChooseTags() {
        List<StaffProfile> profiles = Arrays.asList(
                profile("批量甲", "F", "现场支援组", "服务协调", "8200.00"),
                profile("批量乙", "M", "现场支援组", "技术支持", "8600.00"));
        Assert.assertEquals(2, mapper.insertBatch(profiles));

        Assert.assertEquals(1, mapper.selectPreferred("批量甲", "云平台组").size());
        List<StaffProfile> unitResult = mapper.selectPreferred("", "现场支援组");
        Assert.assertEquals(2, unitResult.size());
        Assert.assertEquals(8, mapper.selectPreferred("", "").size());

        List<Long> ids = unitResult.stream()
                .map(StaffProfile::getId)
                .collect(Collectors.toList());
        Assert.assertEquals(2, mapper.deleteBatch(ids));
        Assert.assertTrue(mapper.selectPreferred("", "现场支援组").isEmpty());
    }

    private static StaffProfile profile(String name, String gender, String unitName,
                                        String positionTitle, String pay) {
        StaffProfile profile = new StaffProfile();
        profile.setName(name);
        profile.setGender(gender);
        profile.setUnitName(unitName);
        profile.setPositionTitle(positionTitle);
        profile.setMonthlyPay(new BigDecimal(pay));
        profile.setEntryDate(LocalDate.of(2025, 1, 6));
        profile.setActiveState(1);
        return profile;
    }
}
