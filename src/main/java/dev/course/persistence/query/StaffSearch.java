package dev.course.persistence.query;

import java.math.BigDecimal;

@SuppressWarnings("unused")
public class StaffSearch {

    private String nameKeyword;
    private String gender;
    private String unitName;
    private Integer activeState;
    private BigDecimal minimumPay;

    public String getNameKeyword() {
        return nameKeyword;
    }

    public void setNameKeyword(String nameKeyword) {
        this.nameKeyword = nameKeyword;
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

    public Integer getActiveState() {
        return activeState;
    }

    public void setActiveState(Integer activeState) {
        this.activeState = activeState;
    }

    public BigDecimal getMinimumPay() {
        return minimumPay;
    }

    public void setMinimumPay(BigDecimal minimumPay) {
        this.minimumPay = minimumPay;
    }
}
