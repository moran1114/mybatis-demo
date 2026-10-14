package com.demo2.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 第三节专用实体：用独立表观察审计字段、逻辑删除与乐观锁。 */
@TableName("emp_lifecycle")
public class EmpLifecycle {
    @TableId(value = "empno", type = IdType.AUTO)
    private Long empno;

    @TableField(value = "ename", updateStrategy = FieldStrategy.NOT_NULL)
    private String ename;

    @TableField(value = "sal", updateStrategy = FieldStrategy.NOT_NULL)
    private BigDecimal sal;

    @TableField("deptno")
    private Integer deptno;

    @TableField(value = "create_time", fill = FieldFill.INSERT,
            updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;

    @Version
    @TableField("version")
    private Integer version;

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

    public BigDecimal getSal() {
        return sal;
    }

    public void setSal(BigDecimal sal) {
        this.sal = sal;
    }

    public Integer getDeptno() {
        return deptno;
    }

    public void setDeptno(Integer deptno) {
        this.deptno = deptno;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "EmpLifecycle{empno=" + empno + ", ename='" + ename + "', sal=" + sal
                + ", deptno=" + deptno + ", createTime=" + createTime
                + ", updateTime=" + updateTime + ", deleted=" + deleted
                + ", version=" + version + '}';
    }
}
