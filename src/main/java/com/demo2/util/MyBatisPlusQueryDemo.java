package com.demo2.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo2.entity.Emp;
import com.demo2.mapper.EmpMapper;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** 第二节：与 docs/08-02-query-wrapper.md 按编号对应。 */
public final class MyBatisPlusQueryDemo {
    private MyBatisPlusQueryDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.openSession()) {
            run(session.getMapper(EmpMapper.class));
            session.rollback();
        }
        System.out.println("演示事务已回滚，条件更新不会保存。");
    }

    public static void run(EmpMapper mapper) {
        System.out.println("8.2.1 QueryWrapper：部门 20、工资至少 10000，按工资降序");
        mapper.selectList(new QueryWrapper<Emp>()
                .eq("deptno", 20).ge("sal", new BigDecimal("10000"))
                .orderByDesc("sal").orderByAsc("empno")).forEach(System.out::println);

        System.out.println("8.2.2 OR 嵌套：部门 20 AND（姓名含 Lin OR 工资至少 20000）");
        mapper.selectList(new QueryWrapper<Emp>().eq("deptno", 20)
                .and(w -> w.like("ename", "Lin").or().ge("sal", new BigDecimal("20000")))
                .orderByAsc("empno")).forEach(System.out::println);

        System.out.println("8.2.3 Lambda：按可选部门、姓名和最低工资拼接条件");
        Integer deptno = 20;
        String name = "Lin";
        BigDecimal minSal = new BigDecimal("10000");
        mapper.selectList(Wrappers.lambdaQuery(Emp.class)
                .eq(deptno != null, Emp::getDeptno, deptno)
                .like(name != null && !name.isBlank(), Emp::getEname, name)
                .ge(minSal != null, Emp::getSal, minSal)
                .orderByAsc(Emp::getEmpno)).forEach(System.out::println);

        System.out.println("8.2.4 selectCount / selectOne");
        System.out.println("部门 20 人数：" + mapper.selectCount(
                Wrappers.lambdaQuery(Emp.class).eq(Emp::getDeptno, 20)));
        System.out.println("编号 1：" + mapper.selectOne(
                Wrappers.lambdaQuery(Emp.class).eq(Emp::getEmpno, 1L)));
        try {
            mapper.selectOne(Wrappers.lambdaQuery(Emp.class).eq(Emp::getDeptno, 20));
        } catch (TooManyResultsException expected) {
            System.out.println("预期异常：selectOne 匹配多行，应使用 selectList 或补充唯一条件。");
        }

        System.out.println("8.2.5 只查部分字段 / selectMaps");
        mapper.selectList(Wrappers.lambdaQuery(Emp.class)
                .select(Emp::getEname, Emp::getSal, Emp::getDeptno)
                .orderByAsc(Emp::getEmpno)).forEach(System.out::println);
        mapper.selectMaps(new QueryWrapper<Emp>()
                .select("ename AS employee_name", "sal", "deptno")
                .eq("deptno", 20).orderByAsc("empno")).forEach(System.out::println);

        System.out.println("8.2.6 分组聚合：至少 3 人的部门");
        mapper.selectMaps(new QueryWrapper<Emp>()
                .select("deptno", "COUNT(*) AS emp_count", "SUM(sal) AS total_sal",
                        "AVG(sal) AS avg_sal")
                .groupBy("deptno").having("COUNT(*) >= {0}", 3)
                .orderByAsc("deptno")).forEach(System.out::println);

        System.out.println("8.2.7 selectByMap：部门 20 且奖金为 NULL");
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("deptno", 20);
        columns.put("comm", null);
        mapper.selectByMap(columns).forEach(System.out::println);
        int changed = mapper.update(null, Wrappers.lambdaUpdate(Emp.class)
                .eq(Emp::getDeptno, 30).lt(Emp::getSal, new BigDecimal("10000"))
                .set(Emp::getComm, BigDecimal.ZERO));
        System.out.println("部门 30 工资低于 10000 的员工奖金设为 0，影响行数：" + changed);

        System.out.println("8.2.8 NOT_NULL：实体的 null 跳过更新，Wrapper 可显式清空");
        Emp sample = mapper.selectById(2L);
        if (sample != null) {
            Emp patch = new Emp();
            patch.setEmpno(sample.getEmpno());
            patch.setJob("Query Demo");
            patch.setComm(null);
            mapper.updateById(patch);
            System.out.println("null 实体更新后奖金（保留原值）：" + mapper.selectById(2L).getComm());
            mapper.update(null, Wrappers.lambdaUpdate(Emp.class)
                    .eq(Emp::getEmpno, sample.getEmpno()).set(Emp::getComm, null));
            System.out.println("显式 set(null) 后奖金：" + mapper.selectById(2L).getComm());
        }
    }
}
