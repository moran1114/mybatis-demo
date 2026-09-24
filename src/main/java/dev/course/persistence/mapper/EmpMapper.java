package dev.course.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.course.persistence.entity.Emp;
import dev.course.persistence.query.EmpSort;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Collection;
import java.util.List;

public interface EmpMapper extends BaseMapper<Emp> {
    // 1. 查询所有员工的所有信息（empno, ename, job, mgr, hiredate, sal, comm, deptno）。
    List<Emp> selectAllEmps();

    // 2. 查询所有员工的姓名 (ename)、工资 (sal) 和部门编号 (deptno)。
    List<Emp> selectNameSalDeptno();

    // 3. 查询工资 (sal) 大于 10000 的所有员工信息。
    List<Emp> selectSalAbove10000();

    // 4. 查询部门编号 (deptno) 为 20 或 30 的所有员工信息。
    List<Emp> selectDeptno20Or30();

    // 5. 查询入职日期 (hiredate) 在 2001-01-01 之后的所有员工信息。
    List<Emp> selectHiredAfter2001();

    // 6. 查询奖金 (comm) 不为 NULL 的所有员工信息。
    List<Emp> selectWithComm();

    // 7. 查询职位 (job) 为 '销售员' 的所有员工信息。
    List<Emp> selectSalespeople();

    // 8. 查询工资 (sal) 在 8000 到 20000 之间的所有员工信息。
    List<Emp> selectSalBetween8000And20000();

    // 9. 查询没有奖金 (comm 为 NULL) 且工资 (sal) 小于 15000 的所有员工信息。
    List<Emp> selectWithoutCommAndSalBelow15000();

    // 10. 查询所有员工信息，按工资 (sal) 降序排序。
    List<Emp> selectOrderBySalDesc();

    Emp selectEmpById(@Param("empno") Long empno);
    int insertEmp(Emp emp);
    int updateEmp(Emp emp);
    int deleteEmpById(@Param("empno") Long empno);
    List<Emp> selectByCondition(@Param("emp") Emp emp);
    List<Emp> selectByConditionWithTrim(@Param("emp") Emp emp);

    // 简单查询演示注解映射；复杂动态 SQL 保留在 XML 中。
    @Select("SELECT COUNT(*) FROM emp WHERE deptno = #{deptno}")
    int countByDeptno(@Param("deptno") Integer deptno);

    List<Emp> selectSorted(@Param("sort") EmpSort sort);

    // 必须提供 empno 和至少一个非 null 的待更新字段；清空 mgr/comm 请用 updateEmp。
    int updatePartial(Emp emp);

    // items 必须非 null 且非空。
    int insertBatch(@Param("items") Collection<Emp> items);
    int deleteBatch(@Param("ids") Collection<Long> ids);
    List<Emp> selectPreferred(@Param("exactName") String exactName,
                              @Param("deptno") Integer deptno);
}
