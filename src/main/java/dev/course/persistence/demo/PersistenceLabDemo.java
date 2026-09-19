package dev.course.persistence.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.course.persistence.entity.StaffProfile;
import dev.course.persistence.mapper.StaffProfileMapper;
import dev.course.persistence.query.StaffSearch;
import dev.course.persistence.util.MyBatisSessions;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class PersistenceLabDemo {

    private PersistenceLabDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisSessions.openSession()) {
            StaffProfileMapper mapper = session.getMapper(StaffProfileMapper.class);
            runClassicMyBatis(mapper);
            runMyBatisPlus(mapper);
            session.rollback();
            System.out.println("演示事务已回滚，数据库种子数据未被修改。");
        }
    }

    private static void runClassicMyBatis(StaffProfileMapper mapper) {
        System.out.println("原始记录数：" + mapper.selectAllProfiles().size());

        StaffProfile newcomer = profile("演示成员", "U", "质量保障组",
                "自动化测试", "9800.00");
        mapper.insertProfile(newcomer);
        StaffProfile stored = mapper.selectProfileById(newcomer.getId());
        stored.setPositionTitle("测试开发");
        mapper.updateProfile(stored);

        StaffProfile patch = new StaffProfile();
        patch.setId(newcomer.getId());
        patch.setMonthlyPay(new BigDecimal("10300.00"));
        mapper.updatePartial(patch);

        StaffSearch search = new StaffSearch();
        search.setUnitName("云平台组");
        search.setMinimumPay(new BigDecimal("14000.00"));
        mapper.selectBySearch(search).forEach(System.out::println);

        List<StaffProfile> batch = Arrays.asList(
                profile("批量样例甲", "F", "现场支援组", "服务协调", "8200.00"),
                profile("批量样例乙", "M", "现场支援组", "技术支持", "8600.00"));
        mapper.insertBatch(batch);
        List<Long> batchIds = mapper.selectPreferred("", "现场支援组").stream()
                .map(StaffProfile::getId)
                .collect(Collectors.toList());
        mapper.deleteBatch(batchIds);
        mapper.deleteProfileById(newcomer.getId());
    }

    private static void runMyBatisPlus(StaffProfileMapper mapper) {
        StaffProfile record = profile("增强演示", "U", "数据质量组",
                "数据校验", "10800.00");
        mapper.insert(record);
        record.setPositionTitle("高级数据校验");
        mapper.updateById(record);

        LambdaQueryWrapper<StaffProfile> wrapper = Wrappers.lambdaQuery(StaffProfile.class)
                .eq(StaffProfile::getActiveState, 1)
                .ge(StaffProfile::getMonthlyPay, new BigDecimal("10000.00"))
                .orderByDesc(StaffProfile::getMonthlyPay);
        mapper.selectList(wrapper).forEach(System.out::println);

        Page<StaffProfile> page = mapper.selectPage(Page.of(1, 3), wrapper);
        System.out.println("分页总记录数：" + page.getTotal() + "，总页数：" + page.getPages());
        mapper.deleteById(record.getId());
    }

    private static StaffProfile profile(String name, String gender, String unitName,
                                        String positionTitle, String pay) {
        StaffProfile profile = new StaffProfile();
        profile.setName(name);
        profile.setGender(gender);
        profile.setUnitName(unitName);
        profile.setPositionTitle(positionTitle);
        profile.setMonthlyPay(new BigDecimal(pay));
        profile.setEntryDate(LocalDate.now());
        profile.setActiveState(1);
        return profile;
    }
}
