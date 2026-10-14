package com.demo2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("emp_detail")
public class EmpDetail {
    @TableId(value = "empno", type = IdType.INPUT)
    private Long empno;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("address")
    private String address;

    @TableField("emergency_contact")
    private String emergencyContact;

    public Long getEmpno() {
        return empno;
    }

    public void setEmpno(Long empno) {
        this.empno = empno;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    @Override
    public String toString() {
        return "EmpDetail{empno=" + empno + ", phone='" + phone + "', email='" + email
                + "', address='" + address + "', emergencyContact='" + emergencyContact + "'}";
    }
}
