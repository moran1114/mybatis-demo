package com.demo2.entity;

public class EmpManagerView {
    private Long empno;
    private String ename;
    private Long managerNo;
    private String managerName;

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

    public Long getManagerNo() {
        return managerNo;
    }

    public void setManagerNo(Long managerNo) {
        this.managerNo = managerNo;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }
}
