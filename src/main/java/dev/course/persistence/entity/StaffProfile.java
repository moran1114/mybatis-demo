package dev.course.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("staff_profile")
@SuppressWarnings("unused")
public class StaffProfile {

    @TableId(value = "staff_id", type = IdType.AUTO)
    private Long id;

    @TableField("full_name")
    private String name;

    @TableField("gender_mark")
    private String gender;

    @TableField("business_unit")
    private String unitName;

    @TableField("position_title")
    private String positionTitle;

    @TableField("monthly_pay")
    private BigDecimal monthlyPay;

    @TableField("entry_date")
    private LocalDate entryDate;

    @TableField("active_state")
    private Integer activeState;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public BigDecimal getMonthlyPay() {
        return monthlyPay;
    }

    public void setMonthlyPay(BigDecimal monthlyPay) {
        this.monthlyPay = monthlyPay;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public Integer getActiveState() {
        return activeState;
    }

    public void setActiveState(Integer activeState) {
        this.activeState = activeState;
    }

    @Override
    public String toString() {
        return "StaffProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", unitName='" + unitName + '\'' +
                ", positionTitle='" + positionTitle + '\'' +
                ", monthlyPay=" + monthlyPay +
                ", entryDate=" + entryDate +
                ", activeState=" + activeState +
                '}';
    }
}
