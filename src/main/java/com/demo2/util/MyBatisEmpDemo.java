package com.demo2.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Emp;
import com.demo2.entity.EmpQuery;
import com.demo2.mapper.EmpMapper;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class MyBatisEmpDemo {

    private MyBatisEmpDemo() {
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

        System.out.println("Demo transactions were rolled back. Call session.commit() to save changes.");
    }

    private static void runClassicMyBatis(EmpMapper mapper) {
        mapper.selectAllEmps().forEach(System.out::println);
        mapper.selectNameSalDeptno().forEach(emp -> System.out.println(
                emp.getEname() + " / " + emp.getSal() + " / " + emp.getDeptno()));
        mapper.selectSalAbove10000().forEach(System.out::println);
        mapper.selectDeptno20Or30().forEach(System.out::println);
        mapper.selectHiredAfter2001().forEach(System.out::println);
        mapper.selectWithComm().forEach(System.out::println);
        mapper.selectSalespeople().forEach(System.out::println);
        mapper.selectSalBetween8000And20000().forEach(System.out::println);
        mapper.selectWithoutCommAndSalBelow15000().forEach(System.out::println);
        mapper.selectOrderBySalDesc().forEach(System.out::println);

        Emp newcomer = emp("Demo Member", 40, "QA", "9800.00");
        mapper.insertEmp(newcomer);
        Emp stored = mapper.selectEmpById(newcomer.getEmpno());
        stored.setJob("Test Developer");
        mapper.updateEmp(stored);

        Emp patch = new Emp();
        patch.setEmpno(newcomer.getEmpno());
        patch.setSal(new BigDecimal("10300.00"));
        mapper.updatePartial(patch);

        Emp search = new Emp();
        search.setDeptno(20);
        search.setSal(new BigDecimal("14000.00"));
        mapper.selectByCondition(search).forEach(System.out::println);
        mapper.selectByConditionWithTrim(search).forEach(System.out::println);
        System.out.println("Dept 20 count: " + mapper.countByDeptno(20));
        mapper.selectSorted(EmpQuery.SortColumn.SAL).forEach(System.out::println);

        mapper.selectPreferred(null, 20).forEach(System.out::println);

        List<Emp> batch = Arrays.asList(
                emp("Batch A", 50, "Coordinator", "8200.00"),
                emp("Batch B", 50, "Support", "8600.00"));
        mapper.insertBatch(batch);
        List<Long> batchIds = mapper.selectPreferred("", 50).stream()
                .map(Emp::getEmpno)
                .collect(Collectors.toList());
        mapper.deleteBatch(batchIds);
        mapper.deleteEmpById(newcomer.getEmpno());
    }

    private static void runMyBatisPlus(EmpMapper mapper) {
        Emp record = emp("Plus Demo", 20, "Data QA", "10800.00");
        mapper.insert(record);
        System.out.println("BaseMapper selectById: " + mapper.selectById(record.getEmpno()));
        record.setJob("Senior Data QA");
        mapper.updateById(record);

        LambdaQueryWrapper<Emp> wrapper = Wrappers.lambdaQuery(Emp.class)
                .eq(Emp::getDeptno, 20)
                .ge(Emp::getSal, new BigDecimal("10000.00"))
                .orderByDesc(Emp::getSal);
        mapper.selectList(wrapper).forEach(System.out::println);

        Page<Emp> page = mapper.selectPage(Page.of(1, 3), wrapper);
        System.out.println("Total: " + page.getTotal() + ", pages: " + page.getPages());
        page.getRecords().forEach(System.out::println);
        mapper.deleteById(record.getEmpno());
    }

    private static Emp emp(String ename, Integer deptno, String job, String pay) {
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
