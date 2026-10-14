package com.demo2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@TableName("project")
public class Project {
    @TableId(value = "project_id", type = IdType.AUTO)
    private Integer projectId;

    @TableField("name")
    private String name;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("budget")
    private BigDecimal budget;

    @TableField(exist = false)
    private List<Emp> members;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public List<Emp> getMembers() {
        return members;
    }

    public void setMembers(List<Emp> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "Project{projectId=" + projectId + ", name='" + name + "', startDate=" + startDate
                + ", budget=" + budget + '}';
    }
}
