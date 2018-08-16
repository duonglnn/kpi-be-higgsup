package com.higgsup.kpi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higgsup.kpi.dto.GroupDTO;
import com.higgsup.kpi.dto.TeamBuildingDTO;
import com.higgsup.kpi.dto.GroupClubDetail;
import com.higgsup.kpi.entity.KpiGroup;
import com.higgsup.kpi.entity.KpiGroupType;
import com.higgsup.kpi.glossary.ErrorCode;
import com.higgsup.kpi.glossary.ErrorMessage;
import com.higgsup.kpi.repository.KpiGroupRepo;
import com.higgsup.kpi.repository.KpiGroupTypeRepo;
import com.higgsup.kpi.service.GroupService;
import com.higgsup.kpi.util.UtilsValidate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private KpiGroupRepo kpiGroupRepo;

    @Autowired
    KpiGroupTypeRepo kpiGroupTypeRepo;

    @Override
    public GroupDTO createConfigTeamBuilding(GroupDTO<TeamBuildingDTO> groupDTO)  throws JsonProcessingException {
        GroupDTO validateGroupDTO = new GroupDTO();
        Integer groupTypeId = groupDTO.getGroupTypeId().getId();

        KpiGroup checkGroupTypeId = kpiGroupRepo.findByGroupTypeId(groupTypeId);
        if(checkGroupTypeId == null){
            if (validateData(groupDTO, validateGroupDTO)) {
                KpiGroup kpiGroup = new KpiGroup();
                ObjectMapper mapper = new ObjectMapper();
                String jsonConfigTeamBuilding = mapper.writeValueAsString(groupDTO.getAdditionalConfig());
                BeanUtils.copyProperties(groupDTO, kpiGroup);
                kpiGroup.setAdditionalConfig(jsonConfigTeamBuilding);
                kpiGroup.setCreatedDate(new Timestamp(System.currentTimeMillis()));
                Optional<KpiGroupType> kpiGroupType = kpiGroupTypeRepo.findById(groupDTO.getGroupTypeId().getId());
                if(kpiGroupType.isPresent()){
                    kpiGroup.setGroupTypeId(kpiGroupType.get());
                    kpiGroupRepo.save(kpiGroup);
                }else {
                    validateGroupDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
                    validateGroupDTO.setMessage(ErrorMessage.NOT_FIND_GROUP_TYPE);
                }
            }
        } else {
            validateGroupDTO.setErrorCode(ErrorCode.CAN_NOT_CREATE.getValue());
            validateGroupDTO.setMessage(ErrorMessage.CAN_NOT_CREATE);
        }
        return validateGroupDTO;
    }

    public GroupDTO createClub(GroupDTO<GroupClubDetail> groupDTO) throws JsonProcessingException {
        GroupDTO groupDTO1 = new GroupDTO();
        Integer minNumberOfSessions = groupDTO.getAdditionalConfig().getMinNumberOfSessions();
        Float participationPoint = groupDTO.getAdditionalConfig().getParticipationPoint();
        Float effectivePoint = groupDTO.getAdditionalConfig().getEffectivePoint();
        if (kpiGroupRepo.findByName(groupDTO.getName()) == null) {
            KpiGroup kpiGroup = new KpiGroup();
            ObjectMapper mapper = new ObjectMapper();

            if(groupDTO.getName().length()== 0 || groupDTO.getAdditionalConfig().getHost().length() == 0 ){
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_NAME_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.NOT_NULL.getValue());
            } else if (minNumberOfSessions != (int) minNumberOfSessions || String.valueOf(minNumberOfSessions).length() > 2 || String.valueOf(minNumberOfSessions).length()==0) {
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_MIN_NUMBER_OF_SESSIONS_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            } else if (participationPoint != (float) participationPoint || String.valueOf(participationPoint).substring(1).length() != 2 || String.valueOf(participationPoint).length() == 0) {
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_POINT_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            } else if (effectivePoint != (float) effectivePoint || String.valueOf(effectivePoint).substring(1).length() != 2 || String.valueOf(effectivePoint).length() == 0) {
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_POINT_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            } else {
                String clubJson = mapper.writeValueAsString(groupDTO.getAdditionalConfig());
                BeanUtils.copyProperties(groupDTO, kpiGroup);
                kpiGroup.setAdditionalConfig(clubJson);
                kpiGroup.setCreatedDate(new Timestamp(System.currentTimeMillis()));
                Optional<KpiGroupType> kpiGroupType = kpiGroupTypeRepo.findById(groupDTO.getGroupTypeId().getId());

                if (kpiGroupType.isPresent()) {
                    kpiGroup.setGroupTypeId(kpiGroupType.get());
                    kpiGroupRepo.save(kpiGroup);
                } else {
                    groupDTO1.setMessage(ErrorMessage.NOT_FIND_GROUP_TYPE);
                    groupDTO1.setErrorCode(ErrorCode.NOT_FIND.getValue());
                }
            }

        } else {
            groupDTO1.setMessage(ErrorMessage.GROUP_ALREADY_EXISTS);
            groupDTO1.setErrorCode(ErrorCode.PARAMETERS_ALREADY_EXIST.getValue());
        }
        return groupDTO1;
    }

    Boolean validateData(GroupDTO<TeamBuildingDTO> groupDTO, GroupDTO validateGroupDTO){
        boolean validate = false;
        if (kpiGroupRepo.findByName(groupDTO.getName()) != null){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.DUPLICATED_ITEM);
        }
        else if(!UtilsValidate.isNumber(groupDTO.getAdditionalConfig().getFirstPrize())){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.INVALIDATED_FIRST_PRIZE);
        }
        else if(!UtilsValidate.isNumber(groupDTO.getAdditionalConfig().getSecondPrize())){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.INVALIDATED_SECOND_PRIZE);
        }
        else if(!UtilsValidate.isNumber(groupDTO.getAdditionalConfig().getThirdPrize())){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.INVALIDATED_THIRD_PRIZE);
        }
        else if(!UtilsValidate.isNumber(groupDTO.getAdditionalConfig().getOrganizers())){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.INVALIDATED_ORGNIZERS_PRIZE);
        }
        else if(Double.parseDouble(groupDTO.getAdditionalConfig().getFirstPrize()) <= Double.parseDouble(groupDTO.getAdditionalConfig().getSecondPrize())){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.FIRST_PRIZE_NOT_LARGER_THAN_SECOND_PRIZE);
        }
        else if(Double.parseDouble(groupDTO.getAdditionalConfig().getSecondPrize()) <= Double.parseDouble(groupDTO.getAdditionalConfig().getThirdPrize())){
            validateGroupDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            validateGroupDTO.setMessage(ErrorMessage.SECOND_PRIZE_NOT_LARGER_THAN_THIRD_PRIZE);
        } else {
            validate = true;
        }
        return validate;
    }
}

