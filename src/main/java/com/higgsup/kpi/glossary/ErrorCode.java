package com.higgsup.kpi.glossary;

public enum ErrorCode {
    NOT_FIND(900, "not find"),
    PARAMETERS_IS_NOT_VALID(901, "parameter is not valid"),
    PARAMETERS_ALREADY_EXIST(932, "parameters already exist"),
    NOT_NULL(903, "parameter cannot null"),
    JSON_PROCESSING_EXCEPTION(904, "json processing exception"),
    DATA_EXIST(902, "data exist"),
    NOT_FILLING_ALL_INFORMATION(905, "not filling all information"),
    ALREADY_CREATED(906, "already created"),
    NOT_FIND_ITEM(928, "item does not existed"),
    NOT_FIND_GROUP_TYPE(929, "group type does not existed"),
    ERROR_IO_EXCEPTION(930, "error IOException"),
    BEGIN_DATE_IS_NOT_AFTER_END_DATE(932, "begin date is not after end date"),
    TEAM_BUILDING_PRIZE_SCORE_CAN_NOT_NULL(931, "team building prize score can not be null");

    private Integer value;

    private String description;

    ErrorCode(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
