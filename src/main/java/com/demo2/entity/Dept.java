package com.demo2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.List;

@TableName("dept")
public class Dept {
    @TableId("deptno")
    private Integer deptno;

    @TableField("dname")
    private String dname;

    @TableField("loc")
    private String loc;

    @TableField(exist = false)
    private List<Emp> emps;

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

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public List<Emp> getEmps() {
        return emps;
    }

    public void setEmps(List<Emp> emps) {
        this.emps = emps;
    }

    @Override
    public String toString() {
        return "Dept{deptno=" + deptno + ", dname='" + dname + "', loc='" + loc + "'}";
    }
}
