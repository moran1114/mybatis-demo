package dev.course.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("emp")
public class Emp {
    @TableId(value = "empno", type = IdType.AUTO)
    private Long empno;

    @TableField("ename")
    private String ename;

    @TableField("job")
    private String job;

    @TableField("mgr")
    private Long mgr;

    @TableField("hiredate")
    private LocalDate hiredate;

    @TableField("sal")
    private BigDecimal sal;

    @TableField("comm")
    private BigDecimal comm;

    @TableField("deptno")
    private Integer deptno;

    public Long getEmpno() {
        return empno;
    }

    public void setEmpno(Long empno) {
        this.empno = empno;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String ename) {
        this.ename = ename;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public Long getMgr() {
        return mgr;
    }

    public void setMgr(Long mgr) {
        this.mgr = mgr;
    }

    public LocalDate getHiredate() {
        return hiredate;
    }

    public void setHiredate(LocalDate hiredate) {
        this.hiredate = hiredate;
    }

    public BigDecimal getSal() {
        return sal;
    }

    public void setSal(BigDecimal sal) {
        this.sal = sal;
    }

    public BigDecimal getComm() {
        return comm;
    }

    public void setComm(BigDecimal comm) {
        this.comm = comm;
    }

    public Integer getDeptno() {
        return deptno;
    }

    public void setDeptno(Integer deptno) {
        this.deptno = deptno;
    }

    @Override
    public String toString() {
        return "Emp{empno=" + empno + ", ename='" + ename + "', job='" + job
                + "', mgr=" + mgr + ", hiredate=" + hiredate + ", sal=" + sal
                + ", comm=" + comm + ", deptno=" + deptno + '}';
    }
}