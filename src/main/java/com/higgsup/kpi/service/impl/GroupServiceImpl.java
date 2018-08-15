package com.higgsup.kpi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higgsup.kpi.dto.GroupClubDetail;
import com.higgsup.kpi.dto.GroupDTO;
import com.higgsup.kpi.dto.Response;
import com.higgsup.kpi.entity.KpiGroup;
import com.higgsup.kpi.entity.KpiGroupType;
import com.higgsup.kpi.glossary.ErrorCode;
import com.higgsup.kpi.glossary.ErrorMessage;
import com.higgsup.kpi.repository.KpiGroupRepo;
import com.higgsup.kpi.repository.KpiGroupTypeRepo;
import com.higgsup.kpi.service.GroupService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

import static com.higgsup.kpi.util.UtilsValidate.minNumberOfSessionsValidation;
import static com.higgsup.kpi.util.UtilsValidate.nameValidation;
import static com.higgsup.kpi.util.UtilsValidate.pointValidation;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private KpiGroupRepo kpiGroupRepo;

    @Autowired
    private KpiGroupTypeRepo kpiGroupTypeRepo;

    @Override
    public GroupDTO updateClub(GroupDTO<GroupClubDetail> groupDTO) throws JsonProcessingException {
        Integer id = groupDTO.getId();
        GroupDTO groupDTO1 = new GroupDTO();
        if (kpiGroupRepo.findById(id) == null) {
            groupDTO1.setMessage(ErrorCode.NOT_FIND.getDescription());
            groupDTO1.setErrorCode(ErrorCode.NOT_FIND.getValue());
        } else {
            if (!nameValidation(groupDTO.getAdditionalConfig().getName()) || !nameValidation(groupDTO.getAdditionalConfig().getHost())) {
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_NAME_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            } else if (!minNumberOfSessionsValidation(groupDTO.getAdditionalConfig().getMinNumberOfSessions())) {
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_MIN_NUMBER_OF_SESSIONS_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            } else if (!pointValidation(groupDTO.getAdditionalConfig().getEffectivePoint()) && !pointValidation(groupDTO.getAdditionalConfig().getParticipationPoint())) {
                groupDTO1.setMessage(ErrorMessage.PARAMETERS_POINT_IS_NOT_VALID);
                groupDTO1.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            } else {
                Optional<KpiGroup> kpiGroup = kpiGroupRepo.findById(id);
                ObjectMapper mapper = new ObjectMapper();
                BeanUtils.copyProperties(groupDTO, kpiGroup);
                String clubJson = mapper.writeValueAsString(groupDTO.getAdditionalConfig());
                kpiGroup.get().setAdditionalConfig(clubJson);
                kpiGroup.get().setCreatedDate(new Timestamp(System.currentTimeMillis()));
                Optional<KpiGroupType> kpiGroupType = kpiGroupTypeRepo.findById(groupDTO.getGroupTypeId().getId());

                if (kpiGroupType.isPresent()) {
                    kpiGroup.get().setGroupTypeId(kpiGroupType.get());
                    kpiGroupRepo.save(kpiGroup.get());
                } else {
                    groupDTO1.setMessage(ErrorMessage.NOT_FIND_GROUP_TYPE);
                    groupDTO1.setErrorCode(ErrorCode.NOT_FIND.getValue());
                }
            }

        }
        return groupDTO1;
    }

}
