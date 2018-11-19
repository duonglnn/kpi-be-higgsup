package com.higgsup.kpi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.util.List;

public class EvaluationDTO extends BaseDTO {
    private Integer id;

    private String evaluationName;

    @JsonFormat(pattern = "dd-MM-yyy HH:mm")
    private Timestamp createdDate;

    private UserDTO manUser;

    private YearMonthDTO yearMonth;

    private EmployeeEvaluationDTO employeeEvaluationDTO;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEvaluationName() {
        return evaluationName;
    }

    public void setEvaluationName(String evaluationName) {
        this.evaluationName = evaluationName;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public UserDTO getManUser() {
        return manUser;
    }

    public void setManUser(UserDTO manUser) {
        this.manUser = manUser;
    }

    public YearMonthDTO getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonthDTO yearMonth) {
        this.yearMonth = yearMonth;
    }

    public EmployeeEvaluationDTO getEmployeeEvaluationDTO() {
        return employeeEvaluationDTO;
    }

    public void setEmployeeEvaluationDTO(EmployeeEvaluationDTO employeeEvaluationDTO) {
        this.employeeEvaluationDTO = employeeEvaluationDTO;
    }
}