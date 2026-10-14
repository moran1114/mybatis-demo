package com.demo2.entity;

import java.math.BigDecimal;

public class EmpQuery {
    private String ename;
    private String job;
    private Integer deptno;
    private BigDecimal minSal;

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

    public Integer getDeptno() {
        return deptno;
    }

    public void setDeptno(Integer deptno) {
        this.deptno = deptno;
    }

    public BigDecimal getMinSal() {
        return minSal;
    }

    public void setMinSal(BigDecimal minSal) {
        this.minSal = minSal;
    }

    public enum SortColumn {
        EMPNO("empno"), SAL("sal"), HIREDATE("hiredate");

        private final String column;

        SortColumn(String column) {
            this.column = column;
        }

        public String getColumn() {
            return column;
        }
    }
}
