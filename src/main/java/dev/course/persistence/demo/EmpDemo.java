package dev.course.persistence.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.course.persistence.entity.Emp;
import dev.course.persistence.query.EmpSort;
import dev.course.persistence.mapper.EmpMapper;
import dev.course.persistence.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class EmpDemo {

    private EmpDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.openMyBatisSession()) {
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            runClassicMyBatis(mapper);
            session.rollback();
        }
        try (SqlSession session = MyBatisUtil.openSession()) {
            runMyBatisPlus(session.getMapper(EmpMapper.class));
            session.rollback();
        }
        System.out.println("两组演示事务均已回滚。实际保存新增、修改、删除时需调用 session.commit()。");
    }

    private static void runClassicMyBatis(EmpMapper mapper) {
        System.out.println("1. 全部员工：");
        mapper.selectAllEmps().forEach(System.out::println);
        System.out.println("2. 姓名、工资、部门：");
        mapper.selectNameSalDeptno().forEach(emp -> System.out.println(
                emp.getEname() + " / " + emp.getSal() + " / " + emp.getDeptno()));
        System.out.println("3. 工资大于 10000：");
        mapper.selectSalAbove10000().forEach(System.out::println);
        System.out.println("4. 部门为 20 或 30：");
        mapper.selectDeptno20Or30().forEach(System.out::println);
        System.out.println("5. 入职日期晚于 2001-01-01：");
        mapper.selectHiredAfter2001().forEach(System.out::println);
        System.out.println("6. 奖金非 NULL（包括零奖金）：");
        mapper.selectWithComm().forEach(System.out::println);
        System.out.println("7. 销售员：");
        mapper.selectSalespeople().forEach(System.out::println);
        System.out.println("8. 工资在 8000 到 20000 之间：");
        mapper.selectSalBetween8000And20000().forEach(System.out::println);
        System.out.println("9. 奖金为 NULL 且工资小于 15000：");
        mapper.selectWithoutCommAndSalBelow15000().forEach(System.out::println);
        System.out.println("10. 按工资降序：");
        mapper.selectOrderBySalDesc().forEach(System.out::println);

        Emp newcomer = emp("演示成员", 40,
                "自动化测试", "9800.00");
        mapper.insertEmp(newcomer);
        Emp stored = mapper.selectEmpById(newcomer.getEmpno());
        stored.setJob("测试开发");
        mapper.updateEmp(stored);

        Emp patch = new Emp();
        patch.setEmpno(newcomer.getEmpno());
        patch.setSal(new BigDecimal("10300.00"));
        mapper.updatePartial(patch);

        Emp search = new Emp();
        search.setDeptno(20);
        search.setSal(new BigDecimal("14000.00"));
        mapper.selectByCondition(search).forEach(System.out::println);
        System.out.println("trim 等效查询：");
        mapper.selectByConditionWithTrim(search).forEach(System.out::println);
        System.out.println("注解查询部门 20 员工数：" + mapper.countByDeptno(20));
        System.out.println("枚举白名单排序（工资降序）：");
        mapper.selectSorted(EmpSort.SAL).forEach(System.out::println);

        System.out.println("choose：姓名优先，忽略不匹配的部门：");
        mapper.selectPreferred("林澈", 30).forEach(System.out::println);
        System.out.println("choose：没有姓名时按部门查询：");
        mapper.selectPreferred(null, 20).forEach(System.out::println);
        System.out.println("choose：没有姓名和部门时查询全部："
                + mapper.selectPreferred(null, null).size());
        search.setEname("林澈");
        search.setDeptno(30);
        System.out.println("if：姓名和部门叠加，结果数：" + mapper.selectByCondition(search).size());

        List<Emp> batch = Arrays.asList(
                emp("批量样例甲", 50, "服务协调", "8200.00"),
                emp("批量样例乙", 50, "技术支持", "8600.00"));
        mapper.insertBatch(batch);
        List<Long> batchIds = mapper.selectPreferred("", 50).stream()
                .map(Emp::getEmpno)
                .collect(Collectors.toList());
        mapper.deleteBatch(batchIds);
        mapper.deleteEmpById(newcomer.getEmpno());
    }

    private static void runMyBatisPlus(EmpMapper mapper) {
        Emp record = emp("增强演示", 20,
                "数据校验", "10800.00");
        mapper.insert(record);
        System.out.println("BaseMapper 按主键查询：" + mapper.selectById(record.getEmpno()));
        record.setJob("高级数据校验");
        mapper.updateById(record);

        LambdaQueryWrapper<Emp> wrapper = Wrappers.lambdaQuery(Emp.class)
                .eq(Emp::getDeptno, 20)
                .ge(Emp::getSal, new BigDecimal("10000.00"))
                .orderByDesc(Emp::getSal);
        mapper.selectList(wrapper).forEach(System.out::println);

        Page<Emp> page = mapper.selectPage(Page.of(1, 3), wrapper);
        System.out.println("分页总记录数：" + page.getTotal() + "，总页数：" + page.getPages());
        page.getRecords().forEach(System.out::println);
        mapper.deleteById(record.getEmpno());
    }

    private static Emp emp(String ename, Integer deptno,
                           String job, String pay) {
        Emp emp = new Emp();
        emp.setEname(ename);
        emp.setDeptno(deptno);
        emp.setJob(job);
        emp.setSal(new BigDecimal(pay));
        emp.setHiredate(LocalDate.now());
        emp.setMgr(6L);
        return emp;
    }
}
