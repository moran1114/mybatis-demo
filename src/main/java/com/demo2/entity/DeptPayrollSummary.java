package com.demo2.entity;

import java.math.BigDecimal;

public class DeptPayrollSummary {
    private Integer deptno;
    private String dname;
    private Integer empCount;
    private BigDecimal totalSal;
    private BigDecimal avgSal;

    public Integer getDeptno() {
        return deptno;
    }

    public void setDeptno(Integer deptno) {
        this.deptno = deptno;
    }

    public String getDname() {
        return dname;
    }

    public void setDname(String dname) {
        this.dname = dname;
    }

    public Integer getEmpCount() {
        return empCount;
    }

    public void setEmpCount(Integer empCount) {
        this.empCount = empCount;
    }

    public BigDecimal getTotalSal() {
        return totalSal;
    }

    public void setTotalSal(BigDecimal totalSal) {
        this.totalSal = totalSal;
    }

    public BigDecimal getAvgSal() {
        return avgSal;
    }

    public void setAvgSal(BigDecimal avgSal) {
        this.avgSal = avgSal;
    }
}
