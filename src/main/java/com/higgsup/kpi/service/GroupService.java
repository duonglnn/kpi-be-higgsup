package com.higgsup.kpi.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.higgsup.kpi.dto.GroupClubDetail;
import com.higgsup.kpi.dto.GroupDTO;

public interface GroupService {
    void createClub(GroupDTO<GroupClubDetail> groupDTO) throws JsonProcessingException;
    Boolean nameValidation(String name);
    Boolean minNumberOfSessionsValidation(Integer minNumberOfSessions);
    Boolean pointValidation(Float point);
}
