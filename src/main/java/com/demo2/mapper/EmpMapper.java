package com.demo2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo2.entity.Emp;
import com.demo2.entity.EmpQuery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface EmpMapper extends BaseMapper<Emp> {
    List<Emp> selectAllEmps();

    List<Emp> selectNameSalDeptno();

    List<Emp> selectSalAbove10000();

    List<Emp> selectDeptno20Or30();

    List<Emp> selectHiredAfter2001();

    List<Emp> selectWithComm();

    List<Emp> selectSalespeople();

    List<Emp> selectSalBetween8000And20000();

    List<Emp> selectWithoutCommAndSalBelow15000();

    List<Emp> selectOrderBySalDesc();

    Emp selectEmpById(@Param("empno") Long empno);

    int insertEmp(Emp emp);

    int updateEmp(Emp emp);

    int deleteEmpById(@Param("empno") Long empno);

    List<Emp> selectByCondition(@Param("emp") Emp emp);

    List<Emp> selectByConditionWithTrim(@Param("emp") Emp emp);

    @Select("SELECT COUNT(*) FROM emp WHERE deptno = #{deptno}")
    int countByDeptno(@Param("deptno") Integer deptno);

    List<Emp> selectSorted(@Param("sort") EmpQuery.SortColumn sort);

    int updatePartial(Emp emp);

    int insertBatch(@Param("items") Collection<Emp> items);

    int deleteBatch(@Param("ids") Collection<Long> ids);

    List<Emp> selectPreferred(@Param("exactName") String exactName,
                              @Param("deptno") Integer deptno);
}
