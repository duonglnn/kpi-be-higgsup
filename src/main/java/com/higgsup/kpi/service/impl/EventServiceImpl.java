package com.higgsup.kpi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higgsup.kpi.dto.*;
import com.higgsup.kpi.entity.*;
import com.higgsup.kpi.glossary.*;
import com.higgsup.kpi.repository.KpiEventRepo;
import com.higgsup.kpi.repository.KpiEventUserRepo;
import com.higgsup.kpi.repository.KpiGroupRepo;
import com.higgsup.kpi.repository.KpiUserRepo;
import com.higgsup.kpi.service.BaseService;
import com.higgsup.kpi.service.EventService;
import com.higgsup.kpi.service.LdapUserService;
import com.higgsup.kpi.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl extends BaseService implements EventService {

    @Autowired
    private KpiEventRepo kpiEventRepo;

    @Autowired
    private KpiEventUserRepo kpiEventUserRepo;

    @Autowired
    private KpiGroupRepo kpiGroupRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private KpiUserRepo kpiUserRepo;

    @Autowired
    private LdapUserService ldapUserService;

    @Autowired
    private Environment environment;

    @Override
    @Transactional
    public EventDTO createSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) throws IOException, NoSuchFieldException {
        EventDTO<List<EventSupportDetail>> eventSupportDTO = new EventDTO<>();
        List<ErrorDTO> validates = validateSupportEvent(supportDTO);
        if (CollectionUtils.isEmpty(validates)) {
            KpiEvent eventSupport = convertEventSupportDTOToEntityForCreate(supportDTO);
            eventSupport = kpiEventRepo.save(eventSupport);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            userService.registerUser(authentication.getPrincipal().toString());
            KpiUser kpiUser = kpiUserRepo.findByUserName(authentication.getPrincipal().toString());

            KpiEventUser kpiEventUser = new KpiEventUser();
            kpiEventUser.setType(EventUserType.MEMBER.getValue());
            KpiEventUserPK kpiEventUserPK = new KpiEventUserPK();
            kpiEventUserPK.setUserName(kpiUser.getUserName());
            kpiEventUserPK.setEventId(eventSupport.getId());
            kpiEventUser.setKpiEventUserPK(kpiEventUserPK);

            kpiEventUserRepo.save(kpiEventUser);

            eventSupportDTO = convertSupportEntiyToDTO(eventSupport);
        } else {
            eventSupportDTO.setErrorCode(validates.get(0).getErrorCode());
            eventSupportDTO.setMessage(validates.get(0).getMessage());
            eventSupportDTO.setErrorDTOS(validates);
        }
        return eventSupportDTO;
    }

    @Override
    @Transactional
    public EventDTO updateSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) throws IOException, NoSuchFieldException {
        EventDTO<List<EventSupportDetail>> eventSupportDTO = new EventDTO<>();
        List<ErrorDTO> validates = validateSupportEvent(supportDTO);
        if (CollectionUtils.isEmpty(validates)) {
            KpiEvent eventSupport = convertEventSupportDTOToEntityForUpdate(supportDTO);
            eventSupportDTO = convertSupportEntiyToDTO(eventSupport);

        } else {
            eventSupportDTO.setErrorCode(validates.get(0).getErrorCode());
            eventSupportDTO.setMessage(validates.get(0).getMessage());
            eventSupportDTO.setErrorDTOS(validates);
        }
        return eventSupportDTO;
    }

    @Override
    public EventDTO confirmOrCancelEvent(EventDTO eventDTO) throws IOException, NoSuchFieldException, IllegalAccessException {
        Optional<KpiEvent> event = kpiEventRepo.findById(eventDTO.getId());
        if (event.isPresent()) {
            KpiEvent kpiEvent = event.get();
            if (validateYearMonth(kpiEvent.getCreatedDate())) {
                if (Objects.equals(kpiEvent.getStatus(), StatusEvent.WAITING.getValue())) {
                    GroupType groupType = GroupType.getGroupType(kpiEvent.getGroup().getGroupType().getId());
                    switch (groupType) {
                        case CLUB:
                            eventDTO = confirmOrCancelEventClub(kpiEvent, eventDTO);
                            break;
                        case SUPPORT:
                            eventDTO = confirmOrCancelEventSupport(kpiEvent, eventDTO);
                            break;
                    }
                } else {
                    eventDTO.setErrorCode(ErrorCode.DATA_CAN_NOT_CHANGE.getValue());
                    eventDTO.setMessage(ErrorMessage.EVENT_CONFIRMED_OR_CANCELED);
                }
            } else {
                eventDTO.setErrorCode(ErrorCode.DATA_CAN_NOT_CHANGE.getValue());
                eventDTO.setMessage(ErrorMessage.CAN_NOT_CHANGE_STATUS_EVENT_LAST_MONTH);
            }
        } else {
            eventDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
            eventDTO.setMessage(ErrorMessage.NOT_FIND_EVENT_BY_ID);
        }
        return eventDTO;
    }

    private EventDTO confirmOrCancelEventClub(KpiEvent kpiEvent, EventDTO eventDTO) throws IOException {
        if (Objects.equals(eventDTO.getStatus(), StatusEvent.CONFIRMED.getValue())) {
            kpiEvent.setStatus(StatusEvent.CONFIRMED.getValue());
        } else {
            kpiEvent.setStatus(StatusEvent.CANCEL.getValue());
        }
        kpiEvent.setAdditionalConfig(kpiEvent.getGroup().getAdditionalConfig());

        kpiEvent = kpiEventRepo.save(kpiEvent);
        if (Objects.equals(kpiEvent.getStatus(), StatusEvent.CONFIRMED.getValue())) {
            addPointWhenConfirmClub(kpiEvent);
        }
        eventDTO = convertEventClubEntityToDTO(kpiEvent);
        return eventDTO;
    }

    private EventDTO convertEventClubEntityToDTO(KpiEvent kpiEvent) throws IOException {
        EventDTO<EventClubDetail> clubDetailEventDTO = new EventDTO<>();
        EventClubDetail eventClubDetail;
        ObjectMapper mapper = new ObjectMapper();

        BeanUtils.copyProperties(kpiEvent, clubDetailEventDTO);

        eventClubDetail = mapper.readValue(kpiEvent.getAdditionalConfig(), EventClubDetail.class);
        clubDetailEventDTO.setAdditionalConfig(eventClubDetail);

        List<KpiEventUser> kpiEventUsers = kpiEventUserRepo.findByKpiEventId(kpiEvent.getId());

        List<EventUserDTO> eventUserDTOS = convertListEventUserEntityToDTO(kpiEventUsers);
        clubDetailEventDTO.setEventUserList(eventUserDTOS);
        clubDetailEventDTO.setGroup(convertConfigEventToDTO(kpiEvent.getGroup()));

        return clubDetailEventDTO;
    }

    private List<EventUserDTO> convertListEventUserEntityToDTO(List<KpiEventUser> kpiEventUsers) {
        List<EventUserDTO> eventUserDTOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(kpiEventUsers)) {
            List<String> namesUser = kpiEventUsers.stream().map(kpiEventUser -> kpiEventUser.getKpiEventUserPK().getUserName())
                    .collect(Collectors.toList());
            List<KpiUser> usersEvent = (List<KpiUser>) kpiUserRepo.findAllById(namesUser);

            for (KpiEventUser kpiEventUser : kpiEventUsers) {
                EventUserDTO eventUserDTO = new EventUserDTO();
                eventUserDTO.setType(kpiEventUser.getType());
                Optional<KpiUser> kpiUser = usersEvent.stream().filter(
                        kpiUserTemp -> kpiUserTemp.getUserName().equals(kpiEventUser.getKpiEventUserPK().getUserName()))
                        .findFirst();
                if (kpiUser.isPresent()) {
                    UserDTO userDTO = new UserDTO();
                    KpiUser userDB = kpiUser.get();

                    BeanUtils.copyProperties(userDB, userDTO);
                    userDTO.setUsername(userDB.getUserName());
                    eventUserDTO.setUser(userDTO);

                    eventUserDTOS.add(eventUserDTO);
                }
            }
        }
        return eventUserDTOS;
    }

    private void addPointWhenConfirmClub(KpiEvent kpiEvent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GroupClubDetail groupClubDetail = mapper.readValue(kpiEvent.getGroup().getAdditionalConfig(), GroupClubDetail.class);
        for (KpiEventUser kpiEventUser : kpiEvent.getKpiEventUserList()) {
            addPoint(kpiEventUser.getKpiUser(), groupClubDetail.getParticipationPoint());
        }
    }

    private boolean validateYearMonth(java.util.Date yearMonth) {
        //is true if in month old , is false if la new
        Timestamp dateCun = new Timestamp(System.currentTimeMillis());
        if (dateCun.getYear() + 1900 == yearMonth.getYear() + 1900
                && dateCun.getMonth() + 1 == yearMonth.getMonth() + 1
                && dateCun.getDate() >= Integer.valueOf(
                environment.getProperty("config.day.new.year.month"))
                ) {
            return true;
        } else if (dateCun.getYear() + 1900 == yearMonth.getYear() + 1900
                && dateCun.getMonth() + 1 > yearMonth.getMonth() + 1
                && (dateCun.getDate() < Integer.valueOf(
                environment.getProperty("config.day.new.year.month"))
                || (dateCun.getDate() == Integer.valueOf(
                environment.getProperty("config.day.new.year.month"))
                && dateCun.getHours() < Integer.valueOf(
                environment.getProperty("config.hour.new.year.month"))))) {
            return true;
        } else if (dateCun.getYear() + 1900 > yearMonth.getYear() + 1900 && (dateCun.getDate() <= Integer.valueOf(
                environment.getProperty("config.day.new.year.month"))
                || (dateCun.getDate() == Integer.valueOf(
                environment.getProperty("config.day.new.year.month"))
                && dateCun.getHours() < Integer.valueOf(
                environment.getProperty("config.hour.new.year.month"))))) {
            return true;
        }
        return false;
    }

    private EventDTO confirmOrCancelEventSupport(KpiEvent kpiEvent, EventDTO eventDTO) throws IOException, NoSuchFieldException,
            IllegalAccessException {
        if (Objects.equals(eventDTO.getStatus(), StatusEvent.CONFIRMED.getValue())) {
            kpiEvent.setStatus(StatusEvent.CONFIRMED.getValue());
        } else {
            kpiEvent.setStatus(StatusEvent.CANCEL.getValue());
        }
        Float point = setHistorySupportAndGetAllPoint(kpiEvent);
        //ad point
        if (Objects.equals(kpiEvent.getStatus(), StatusEvent.CONFIRMED.getValue())) {
            addPoint(kpiEvent.getKpiEventUserList().get(0).getKpiUser(), point);
        }

        kpiEvent = kpiEventRepo.save(kpiEvent);

        eventDTO = convertSupportEntiyToDTO(kpiEvent);
        return eventDTO;
    }

    private Float setHistorySupportAndGetAllPoint(KpiEvent kpiEvent) throws NoSuchFieldException, IllegalAccessException,
            IOException {
        ObjectMapper mapper = new ObjectMapper();
        Float point = 0F;
        TypeReference<List<EventSupportDetail>> tRefBedType = new TypeReference<List<EventSupportDetail>>() {
        };
        List<EventSupportDetail> configEventSupport = mapper.readValue(kpiEvent.getAdditionalConfig(), tRefBedType);
        GroupSupportDetail groupSupportDetail = mapper.readValue(kpiEvent.getGroup().getAdditionalConfig(),
                GroupSupportDetail.class);
        for (EventSupportDetail eventSupportDetail : configEventSupport) {
            Field field = groupSupportDetail.getClass().getDeclaredField(eventSupportDetail.getName());
            field.setAccessible(true);
            Float pointConfig = (Float) field.get(groupSupportDetail);
            eventSupportDetail.setPoint(pointConfig);
            point += pointConfig * eventSupportDetail.getQuantity();
        }
        kpiEvent.setAdditionalConfig(mapper.writeValueAsString(configEventSupport));
        return point;
    }

    private KpiEvent convertEventSupportDTOToEntityForUpdate(EventDTO<List<EventSupportDetail>> supportDTO) throws
            JsonProcessingException {
        KpiEvent eventSupport;
        ObjectMapper mapper = new ObjectMapper();
        eventSupport = kpiEventRepo.findById(supportDTO.getId()).get();

        eventSupport.setBeginDate(supportDTO.getBeginDate());
        String additionalConfigSupport = mapper.writeValueAsString(supportDTO.getAdditionalConfig());
        eventSupport.setAdditionalConfig(additionalConfigSupport);

        eventSupport.setUpdatedDate(new Timestamp(System.currentTimeMillis()));

        return kpiEventRepo.save(eventSupport);
    }

    private EventDTO<List<EventSupportDetail>> convertSupportEntiyToDTO(KpiEvent kpiEvent) throws IOException {
        EventDTO<List<EventSupportDetail>> eventSupportDTO = new EventDTO<>();
        List<EventSupportDetail> detailsConfigSupport;
        ObjectMapper mapper = new ObjectMapper();

        BeanUtils.copyProperties(kpiEvent, eventSupportDTO);

        TypeReference<List<EventSupportDetail>> tRefBedType = new TypeReference<List<EventSupportDetail>>() {
        };
        detailsConfigSupport = mapper.readValue(kpiEvent.getAdditionalConfig(), tRefBedType);
        eventSupportDTO.setAdditionalConfig(detailsConfigSupport);

        List<KpiEventUser> kpiEventUsers = kpiEventUserRepo.findByKpiEventId(kpiEvent.getId());

        List<EventUserDTO> eventUserDTOS = convertListEventUserEntityToDTO(kpiEventUsers);
        eventSupportDTO.setEventUserList(eventUserDTOS);
        eventSupportDTO.setGroup(convertConfigEventToDTO(kpiEvent.getGroup()));

        return eventSupportDTO;
    }

    private GroupDTO convertClubEntityToDTO(KpiGroup kpiGroup) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        GroupDTO groupDTO = new GroupDTO();
        BeanUtils.copyProperties(kpiGroup, groupDTO);

        groupDTO.setAdditionalConfig(mapper.readValue(kpiGroup.getAdditionalConfig(), GroupClubDetail.class));
        groupDTO.setGroupType(convertGroupTypeEntityToDTO(kpiGroup.getGroupType()));
        return groupDTO;
    }

    private GroupTypeDTO convertGroupTypeEntityToDTO(KpiGroupType kpiGroupType) {
        GroupTypeDTO groupTypeDTO = new GroupTypeDTO();
        BeanUtils.copyProperties(kpiGroupType, groupTypeDTO);
        return groupTypeDTO;
    }

    public GroupDTO convertConfigEventToDTO(KpiGroup kpiGroup) throws IOException {
        GroupDTO groupDTO = new GroupDTO<>();
        ObjectMapper mapper = new ObjectMapper();

        switch (GroupType.getGroupType(kpiGroup.getGroupType().getId())) {
            case SUPPORT:

                BeanUtils.copyProperties(kpiGroup, groupDTO);
                GroupSupportDetail groupSeminarDetail = mapper.readValue(kpiGroup.getAdditionalConfig(),
                        GroupSupportDetail.class);

                groupDTO.setAdditionalConfig(groupSeminarDetail);
                groupDTO.setGroupType(convertGroupTypeEntityToDTO(kpiGroup.getGroupType()));
                break;
            case CLUB:

                BeanUtils.copyProperties(kpiGroup, groupDTO);
                EventClubDetail eventClubDetail = mapper.readValue(kpiGroup.getAdditionalConfig(),
                        EventClubDetail.class);

                groupDTO.setAdditionalConfig(eventClubDetail);
                groupDTO.setGroupType(convertGroupTypeEntityToDTO(kpiGroup.getGroupType()));
                break;
        }

        return groupDTO;
    }

    private KpiEvent convertEventSupportDTOToEntityForCreate(EventDTO<List<EventSupportDetail>> supportDTO) throws
            JsonProcessingException {
        KpiEvent eventSupport = new KpiEvent();
        ObjectMapper mapper = new ObjectMapper();

        eventSupport.setStatus(StatusEvent.WAITING.getValue());
        eventSupport.setBeginDate(supportDTO.getBeginDate());
        Optional<KpiGroup> kpiGroup = kpiGroupRepo.findById(supportDTO.getGroup().getId());
        if (kpiGroup.isPresent()) {
            KpiGroup kpiGroupDB = kpiGroup.get();
            kpiGroup.ifPresent(eventSupport::setGroup);
            eventSupport.setGroup(kpiGroupDB);

            String additionalConfigSupport = mapper.writeValueAsString(supportDTO.getAdditionalConfig());
            eventSupport.setAdditionalConfig(additionalConfigSupport);
            eventSupport.setName(kpiGroupDB.getName());
        }

        return eventSupport;
    }

    private List<ErrorDTO> validateSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) {
        List<ErrorDTO> errors = new ArrayList<>();
        if (Objects.isNull(supportDTO.getBeginDate())) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            errorDTO.setMessage(ErrorMessage.START_DATE_CAN_NOT_NULL);

            errors.add(errorDTO);
        }

        if (Objects.isNull(supportDTO.getGroup())) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            errorDTO.setMessage(ErrorMessage.GROUP_CAN_NOT_NULL);

            errors.add(errorDTO);
        } else {
            if (Objects.isNull(supportDTO.getGroup().getId())) {
                ErrorDTO errorDTO = new ErrorDTO();

                errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                errorDTO.setMessage(ErrorMessage.GROUP_ID_CAN_NOT_NULL);

                errors.add(errorDTO);
            } else {
                Optional<KpiGroup> kpiGroup = kpiGroupRepo.findById(supportDTO.getGroup().getId());
                if (!kpiGroup.isPresent() || !Objects.equals(kpiGroup.get().getGroupType().getId(), GroupType.SUPPORT.getId())) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                    errorDTO.setMessage(ErrorMessage.GROUP_ID_NOT_IS_SUPPORT);
                    errors.add(errorDTO);
                }
            }
        }

        if (CollectionUtils.isEmpty(supportDTO.getAdditionalConfig())) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
            errorDTO.setMessage(ErrorMessage.LIST_SUPPORT_CAN_NOT_EMPTY);

            errors.add(errorDTO);
        } else {
            for (int i = 0; i < supportDTO.getAdditionalConfig().size(); i++) {
                EventSupportDetail supportDetail = supportDTO.getAdditionalConfig().get(i);

                if (Objects.isNull(supportDetail.getName())) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                    errorDTO.setMessage(String.format(ErrorMessage.NAME_AT_INDEX_CAN_NOT_NULL, i));

                    errors.add(errorDTO);
                } else {
                    try {
                        GroupSupportDetail.class.getDeclaredField(supportDetail.getName());
                    } catch (NoSuchFieldException e) {
                        ErrorDTO errorDTO = new ErrorDTO();

                        errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                        errorDTO.setMessage(String.format(ErrorMessage.NAME_SUPPORT_AT_INDEX_CAN_NOT_INCORRECT, i));

                        errors.add(errorDTO);
                    }
                }
                if (Objects.isNull(supportDetail.getQuantity())) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                    errorDTO.setMessage(String.format(ErrorMessage.QUANTITY_AT_INDEX_CAN_NOT_NULL, i));

                    errors.add(errorDTO);
                } else {
                    if (supportDetail.getQuantity() <= 0) {
                        ErrorDTO errorDTO = new ErrorDTO();

                        errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                        errorDTO.setMessage(String.format(ErrorMessage.QUANTITY_AT_INDEX_CAN_NOT_LESS_ONE, i));

                        errors.add(errorDTO);
                    }
                }

            }
        }
        //for update
        if (Objects.nonNull(supportDTO.getId())) {
            Optional<KpiEvent> kpiEventOptional = kpiEventRepo.findById(supportDTO.getId());

            if (kpiEventOptional.isPresent()) {
                KpiEvent kpiEvent = kpiEventOptional.get();
                if (!Objects.equals(kpiEvent.getGroup().getGroupType().getId(), GroupType.SUPPORT.getId())) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                    errorDTO.setMessage(ErrorMessage.GROUP_WITH_ID_NOT_IS_SUPPORT);

                    errors.add(errorDTO);
                }
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                List<KpiEventUser> kpiEventUsers = kpiEventUserRepo.findByKpiEventId(supportDTO.getId());
                if (!(kpiEventUsers.size() > 1)
                        && !(kpiEventUsers.get(0).getKpiEventUserPK().getUserName().equals(authentication.getPrincipal()))) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(HttpStatus.FORBIDDEN.value());
                    errorDTO.setMessage(HttpStatus.FORBIDDEN.getReasonPhrase());

                    errors.add(errorDTO);
                }
            } else {
                ErrorDTO errorDTO = new ErrorDTO();

                errorDTO.setErrorCode(ErrorCode.PARAMETERS_IS_NOT_VALID.getValue());
                errorDTO.setMessage(ErrorMessage.ID_NOT_INCORRECT);

                errors.add(errorDTO);
            }

        }

        return errors;
    }

    @Override
    @Transactional
    public EventDTO createClub(EventDTO<EventClubDetail> eventDTO) throws IOException {
        eventDTO.setStatus(StatusEvent.WAITING.getValue());

        EventDTO<EventClubDetail> validatedEventDTO = new EventDTO<>();

        KpiEvent kpiEvent = new KpiEvent();

        ObjectMapper mapper = new ObjectMapper();

        List<ErrorDTO> validates = validateClub(eventDTO);

        if (CollectionUtils.isEmpty(validates)) {
            String clubJson = mapper.writeValueAsString(eventDTO.getAdditionalConfig());
            BeanUtils.copyProperties(eventDTO, kpiEvent);

            kpiEvent.setAdditionalConfig(clubJson);
            Optional<KpiGroup> groupOptional = kpiGroupRepo.findById(eventDTO.getGroup().getId());

            if (groupOptional.isPresent()) {
                kpiEvent.setGroup(groupOptional.get());
                kpiEvent = kpiEventRepo.save(kpiEvent);

                List<KpiEventUser> eventUsers = convertEventUsersToEntity(kpiEvent, eventDTO.getEventUserList());
                kpiEventUserRepo.saveAll(eventUsers);

                BeanUtils.copyProperties(kpiEvent, validatedEventDTO);
                validatedEventDTO.setGroup(convertClubEntityToDTO(kpiEvent.getGroup()));
                validatedEventDTO.setEventUserList(eventDTO.getEventUserList());
            } else {
                validatedEventDTO.setMessage(ErrorMessage.NOT_FIND_GROUP);
                validatedEventDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
            }
        } else {
            validatedEventDTO.setErrorCode(validates.get(0).getErrorCode());
            validatedEventDTO.setMessage(validates.get(0).getMessage());
            validatedEventDTO.setErrorDTOS(validates);
        }
        return validatedEventDTO;
    }

    @Override
    public EventDTO updateClub(EventDTO<EventClubDetail> eventDTO) throws IOException {

        EventDTO<EventClubDetail> validatedEventDTO = new EventDTO<>();

        List<ErrorDTO> validates = validateClub(eventDTO);

        Optional<KpiEvent> kpiEventOptional = kpiEventRepo.findById(eventDTO.getId());

        if (kpiEventOptional.isPresent()) {
            if (CollectionUtils.isEmpty(validates)) {

                KpiEvent kpiEvent = kpiEventOptional.get();
                eventDTO.setId(kpiEvent.getId());

                ObjectMapper mapper = new ObjectMapper();
                String clubJson = mapper.writeValueAsString(eventDTO.getAdditionalConfig());
                BeanUtils.copyProperties(eventDTO, kpiEvent, "createdDate", "updatedDate");

                kpiEvent.setAdditionalConfig(clubJson);
                Optional<KpiGroup> groupOptional = kpiGroupRepo.findById(eventDTO.getGroup().getId());

                if (groupOptional.isPresent()) {
                    kpiEvent.setGroup(groupOptional.get());
                    kpiEvent.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
                    kpiEvent = kpiEventRepo.save(kpiEvent);

                    List<KpiEventUser> eventUsers = convertEventUsersToEntity(kpiEvent, eventDTO.getEventUserList());
                    kpiEventUserRepo.saveAll(eventUsers);

                    BeanUtils.copyProperties(kpiEvent, validatedEventDTO);
                    validatedEventDTO.setGroup(convertClubEntityToDTO(kpiEvent.getGroup()));
                    validatedEventDTO.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
                    validatedEventDTO.setAdditionalConfig(eventDTO.getAdditionalConfig());
                    validatedEventDTO.setEventUserList(eventDTO.getEventUserList());

                } else {
                    validatedEventDTO.setMessage(ErrorMessage.NOT_FIND_GROUP);
                    validatedEventDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
                }
            } else {
                validatedEventDTO.setErrorCode(validates.get(0).getErrorCode());
                validatedEventDTO.setMessage(validates.get(0).getMessage());
                validatedEventDTO.setErrorDTOS(validates);
            }
        }

        return validatedEventDTO;
    }

    private List<KpiEventUser> convertEventUsersToEntity(KpiEvent kpiEvent, List<EventUserDTO> eventUserList) {
        List<KpiEventUser> kpiEventUsers = new ArrayList<>();

        for (EventUserDTO eventUserDTO : eventUserList) {
            KpiEventUser kpiEventUser = new KpiEventUser();
            KpiEventUserPK kpiEventUserPK = new KpiEventUserPK();
            userService.registerUser(eventUserDTO.getUser().getUsername());

            kpiEventUserPK.setEventId(kpiEvent.getId());
            kpiEventUserPK.setUserName(eventUserDTO.getUser().getUsername());
            kpiEventUser.setKpiEventUserPK(kpiEventUserPK);

            kpiEventUser.setType(eventUserDTO.getType());
            kpiEventUsers.add(kpiEventUser);

        }
        return kpiEventUsers;
    }

    private List<ErrorDTO> validateClub(EventDTO<EventClubDetail> eventDTO) {
        List<ErrorDTO> errors = new ArrayList<>();

        if (eventDTO.getName() == null) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setErrorCode(ErrorCode.NOT_NULL.getValue());
            errorDTO.setMessage(ErrorMessage.NAME_DOES_NOT_ALLOW_NULL);

            errors.add(errorDTO);
        }

        if (eventDTO.getBeginDate() == null) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setMessage(ErrorMessage.BEGIN_DATE_CAN_NOT_NULL);
            errorDTO.setErrorCode(ErrorCode.NOT_NULL.getValue());

            errors.add(errorDTO);

        } else if (eventDTO.getEndDate() == null) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setMessage(ErrorMessage.END_DATE_CAN_NOT_NULL);
            errorDTO.setErrorCode(ErrorCode.NOT_NULL.getValue());

            errors.add(errorDTO);

        } else {
            if (eventDTO.getBeginDate().after(eventDTO.getEndDate())) {
                ErrorDTO errorDTO = new ErrorDTO();

                errorDTO.setMessage(ErrorCode.BEGIN_DATE_IS_NOT_AFTER_END_DATE.getDescription());
                errorDTO.setErrorCode(ErrorCode.BEGIN_DATE_IS_NOT_AFTER_END_DATE.getValue());

                errors.add(errorDTO);
            }
        }

        if (eventDTO.getEventUserList().size() == 0) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setMessage(ErrorMessage.LIST_OF_PARTICIPANTS_CAN_NOT_NULL);
            errorDTO.setErrorCode(ErrorCode.NOT_NULL.getValue());

            errors.add(errorDTO);
        }

        if (!kpiGroupRepo.findById(eventDTO.getGroup().getId()).isPresent()) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setMessage(ErrorMessage.NOT_FIND_GROUP);
            errorDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());

            errors.add(errorDTO);
        } else if (eventDTO.getEventUserList().size() != 0) {
            for (EventUserDTO eventUserDTO : eventDTO.getEventUserList()) {
                Integer userType = eventUserDTO.getType();
                if (userType == null) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.NOT_NULL.getValue());
                    errorDTO.setMessage(ErrorMessage.USER_TYPE_CAN_NOT_NULL);

                    errors.add(errorDTO);
                } else if (userType < EventUserType.HOST.getValue() || userType > EventUserType.LISTEN.getValue()) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
                    errorDTO.setMessage(ErrorMessage.MEMBER_TYPE_DOES_NOT_EXIST);

                    errors.add(errorDTO);
                } else if (!validateUser(eventDTO)) {
                    ErrorDTO errorDTO = new ErrorDTO();

                    errorDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
                    errorDTO.setMessage(ErrorMessage.USER_DOES_NOT_EXIST);

                    errors.add(errorDTO);
                }
            }
        }

        //Validate update
        if (Objects.nonNull(eventDTO.getId())) {
            Optional<KpiEvent> kpiEventOptional = kpiEventRepo.findById(eventDTO.getId());

            if (!kpiEventOptional.isPresent()) {
                ErrorDTO errorDTO = new ErrorDTO();

                errorDTO.setErrorCode(ErrorCode.NOT_FIND.getValue());
                errorDTO.setMessage(ErrorMessage.NOT_FIND_EVENT);

                errors.add(errorDTO);
            }
        }

        if (eventDTO.getStatus() != 1) {
            ErrorDTO errorDTO = new ErrorDTO();

            errorDTO.setErrorCode(ErrorCode.CANNOT_UPDATE.getValue());
            errorDTO.setMessage(ErrorMessage.CAN_NOT_UPDATE_EVENT);

            errors.add(errorDTO);
        }

        return errors;
    }

    private Boolean validateUser(EventDTO<EventClubDetail> eventDTO) {
        List<UserDTO> ldapUserList = ldapUserService.getAllUsers();
        List<UserDTO> ldapUserListClone = new ArrayList<>(ldapUserList);
        List<KpiUser> dbUserList = (List<KpiUser>) kpiUserRepo.findAll();

        ldapUserList.removeIf(userDTO -> dbUserList.stream()
                .anyMatch(kpiUser -> kpiUser.getUserName().equals(userDTO.getUsername())));

        ldapUserList.forEach(userDTO -> userService.registerUser(userDTO.getUsername()));

        for (EventUserDTO eventUserDTO : eventDTO.getEventUserList()) {
            if (ldapUserListClone.stream().noneMatch(
                    userDTO -> userDTO.getUsername().equals(eventUserDTO.getUser().getUsername()))) {
                return false;
            }
        }
        return true;
    }
}
