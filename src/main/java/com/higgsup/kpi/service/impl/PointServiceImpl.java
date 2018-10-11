package com.higgsup.kpi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.higgsup.kpi.dto.*;
import com.higgsup.kpi.dto.EventDTO;
import com.higgsup.kpi.dto.EventTeamBuildingDetail;
import com.higgsup.kpi.entity.*;
import com.higgsup.kpi.glossary.EventUserType;
import com.higgsup.kpi.glossary.PointValue;
import com.higgsup.kpi.repository.*;
import com.higgsup.kpi.service.BaseService;
import com.higgsup.kpi.service.PointService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.higgsup.kpi.glossary.EventUserType.ORGANIZER;
import static com.higgsup.kpi.glossary.PointValue.FULL_RULE_POINT;
import static com.higgsup.kpi.glossary.PointValue.LATE_TIME_POINT;

@Service
public class PointServiceImpl extends BaseService implements PointService {

    @Autowired
    private KpiLateTimeCheckRepo kpiLateTimeCheckRepo;

    @Autowired
    private KpiPointRepo kpiPointRepo;

    @Autowired
    private KpiMonthRepo kpiMonthRepo;

    @Autowired
    private KpiProjectUserRepo kpiProjectUserRepo;

    @Autowired
    EventServiceImpl kpiEventService;

    @Autowired
    private KpiEventUserRepo kpiEventUserRepo;

    @Autowired
    private KpiGroupRepo kpiGroupRepo;

    @Autowired
    private KpiUserRepo kpiUserRepo;

    @Autowired
    private KpiEventRepo kpiEventRepo;

    @Autowired
    KpiSeminarSurveyRepo kpiSeminarSurveyRepo;

    @Autowired
    KpiGroupTypeRepo kpiGroupTypeRepo;

    @Scheduled(cron = "0 0 16 10 * ?")
    public void calculateRulePoint() {
        Optional<KpiYearMonth> kpiYearMonthOptional = kpiMonthRepo.findByMonthCurrent();

        if (kpiYearMonthOptional.isPresent()) {
            List<KpiLateTimeCheck> LateTimeCheckList = kpiLateTimeCheckRepo.findByMonth(kpiYearMonthOptional.get());

            for (KpiLateTimeCheck kpiLateTimeCheck : LateTimeCheckList) {

                Float rulePoint = (float) (FULL_RULE_POINT.getValue() + kpiLateTimeCheck.getLateTimes() * LATE_TIME_POINT.getValue());

                if (Objects.nonNull(kpiPointRepo.findByRatedUser(kpiLateTimeCheck.getUser()))) {
                    KpiPoint kpiPoint = kpiPointRepo.findByRatedUser(kpiLateTimeCheck.getUser());
                    kpiPoint.setRulePoint(rulePoint);
                    kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());

                    kpiPointRepo.save(kpiPoint);
                } else {
                    KpiPoint kpiPoint = new KpiPoint();
                    kpiPoint.setRatedUser(kpiLateTimeCheck.getUser());
                    kpiPoint.setRulePoint(rulePoint);
                    kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());

                    kpiPointRepo.save(kpiPoint);
                }
            }
        }

    }


    @Override
    public void calculateTeambuildingPoint(EventDTO<EventTeamBuildingDetail> teamBuildingDTO) {
        Float firstPrizePoint = teamBuildingDTO.getAdditionalConfig().getFirstPrizePoint();
        Float secondPrizePoint = teamBuildingDTO.getAdditionalConfig().getSecondPrizePoint();
        Float thirdPrizePoint = teamBuildingDTO.getAdditionalConfig().getThirdPrizePoint();
        Float organizerPoint = teamBuildingDTO.getAdditionalConfig().getOrganizerPoint();

        KpiPoint kpiPoint = new KpiPoint();

        List<KpiEventUser> kpiEventUserList = kpiEventUserRepo.findByKpiEventId(teamBuildingDTO.getId());

        List<KpiEventUser> organizers = kpiEventUserList.stream().filter(u -> u.getType()
                .equals(ORGANIZER.getValue())).collect(Collectors.toList());

        List<KpiEventUser> participants = kpiEventUserList.stream().filter(u -> !u.getType()
                .equals(ORGANIZER.getValue())).collect(Collectors.toList());

        for (KpiEventUser participant : participants) {

            if (Objects.nonNull(kpiPointRepo.findByRatedUser(participant.getKpiUser()))) {
                kpiPoint = kpiPointRepo.findByRatedUser(participant.getKpiUser());

                EventUserType eventUserType = EventUserType.getEventUserType(participant.getType());
                switch (Objects.requireNonNull(eventUserType)) {
                    case FIRST_PLACE:
                        kpiPoint.setTeambuildingPoint(firstPrizePoint);
                        break;
                    case SECOND_PLACE:
                        kpiPoint.setTeambuildingPoint(secondPrizePoint);
                        break;
                    case THIRD_PLACE:
                        kpiPoint.setTeambuildingPoint(thirdPrizePoint);
                        break;
                }
                kpiPointRepo.save(kpiPoint);
            } else {
                kpiPoint = new KpiPoint();
                kpiPoint.setRatedUser(participant.getKpiUser());
            }


        }

        for (KpiEventUser kpiEventUser : organizers) {
            kpiPoint.setRatedUser(kpiEventUser.getKpiUser());

            List<KpiEventUser> gamingOrganizers = kpiEventUserList.stream()
                    .filter(u -> u.getKpiUser().equals(kpiEventUser.getKpiUser())).collect(Collectors.toList());

            if (gamingOrganizers.isEmpty()){
                kpiPoint.setTeambuildingPoint(organizerPoint);
            } else {
                for (KpiEventUser gamingOrganizer : gamingOrganizers ){

                    EventUserType eventUserType = EventUserType.getEventUserType(gamingOrganizer.getType());
                    switch (Objects.requireNonNull(eventUserType)) {
                        case FIRST_PLACE: {
                            if (organizerPoint > firstPrizePoint) {
                                kpiPoint.setTeambuildingPoint(organizerPoint);
                            } else {
                                kpiPoint.setTeambuildingPoint(firstPrizePoint);
                            }
                            break;
                        }
                        case SECOND_PLACE: {
                            if (organizerPoint > secondPrizePoint) {
                                kpiPoint.setTeambuildingPoint(organizerPoint);
                            } else {
                                kpiPoint.setTeambuildingPoint(secondPrizePoint);
                            }
                            break;
                            }
                        case THIRD_PLACE: {
                            if (organizerPoint > thirdPrizePoint) {
                                kpiPoint.setTeambuildingPoint(organizerPoint);
                            } else {
                                kpiPoint.setTeambuildingPoint(thirdPrizePoint);
                            }
                            break;
                        }
                    }
                }
            }
            kpiPointRepo.save(kpiPoint);
        }
    }

    @Scheduled(cron = "40 48 18 10 * ?")
    private void addEffectivePointForHost() throws IOException{
        Optional<KpiYearMonth> kpiYearMonthOptional = kpiMonthRepo.findByMonthCurrent();

        List<KpiGroup> allClub = kpiGroupRepo.findAllClub();

        List<GroupDTO<GroupClubDetail>> allClubDTO = convertClubGroupEntityToDTO(allClub);

        Long hostParticipate = 0L;
        for(GroupDTO<GroupClubDetail> clubDTO:allClubDTO) {
            KpiUser clubOwner = kpiUserRepo.findByUserName(clubDTO.getAdditionalConfig().getHost());
            List<KpiEvent> confirmClubEvents = kpiEventRepo.findConfirmedClubEventCreatedByHost(clubDTO.getAdditionalConfig().getHost());
            for(KpiEvent event:confirmClubEvents){
                List<KpiEventUser> kpiEventUsers = kpiEventUserRepo.findByKpiEventId(event.getId());
                hostParticipate = kpiEventUsers.stream().filter(e -> e.getKpiEventUserPK().getUserName()
                        .equals(clubDTO.getAdditionalConfig().getHost())).count();
            }
            if(confirmClubEvents.size() >= clubDTO.getAdditionalConfig().getMinNumberOfSessions() / 2 &&
                    hostParticipate >= (float)confirmClubEvents.size() * 3/4){
                if(kpiPointRepo.findByRatedUser(clubOwner) != null){
                    KpiPoint kpiPoint = kpiPointRepo.findByRatedUser(clubOwner);
                    kpiPoint.setClubPoint(kpiPoint.getClubPoint() + clubDTO.getAdditionalConfig().getEffectivePoint());
                    kpiPointRepo.save(kpiPoint);
                }else{
                    KpiPoint kpiPoint = new KpiPoint();
                    kpiPoint.setRatedUser(clubOwner);
                    kpiPoint.setClubPoint(clubDTO.getAdditionalConfig().getEffectivePoint());
                    kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());
                    kpiPointRepo.save(kpiPoint);
                }
            }
        }
    }

    @Scheduled(cron = "40 24 17 11 * ?")
    private void addUnfinishedSurveySeminarPoint() throws IOException{
        List<KpiEvent> unfinishedSurveySeminarEvent = kpiEventRepo.findUnfinishedSurveySeminarEvent();
        if(unfinishedSurveySeminarEvent.size() > 0){
            for(KpiEvent event: unfinishedSurveySeminarEvent){
                event.setGroup(kpiGroupRepo.findGroupByEventId(event.getId()));
                KpiGroupType kpiGroupType = kpiGroupTypeRepo.findByGroupId(event.getGroup().getId());
                event.getGroup().setGroupType(kpiGroupType);
                List<KpiEventUser> kpiEventUsers = kpiEventUserRepo.findByKpiEventId(event.getId());
                event.setKpiEventUserList(kpiEventUsers);
                EventDTO<EventSeminarDetail> seminarEventDTO = kpiEventService.convertSeminarEntityToDTO(event);
                addSeminarPoint(kpiEventUsers, seminarEventDTO);
            }
        }
    }

    private List<GroupDTO<GroupClubDetail>> convertClubGroupEntityToDTO(List<KpiGroup> allClub) throws IOException {
        List<GroupDTO<GroupClubDetail>> groupDTOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(allClub)) {
            for (KpiGroup kpiGroup : allClub) {
                    ObjectMapper mapper = new ObjectMapper();
                    GroupDTO<GroupClubDetail> groupClubDTO = new GroupDTO<>();

                    BeanUtils.copyProperties(kpiGroup, groupClubDTO);
                    GroupClubDetail groupClubDetail = mapper.readValue(kpiGroup.getAdditionalConfig(), GroupClubDetail.class);

                    groupClubDTO.setAdditionalConfig(groupClubDetail);
                    groupClubDTO.setGroupType(convertGroupTypeEntityToDTO(kpiGroup.getGroupType()));
                    groupDTOS.add(groupClubDTO);
                }
            }
        return groupDTOS;
    }

    private GroupTypeDTO convertGroupTypeEntityToDTO(KpiGroupType kpiGroupType) {
        GroupTypeDTO groupTypeDTO = new GroupTypeDTO();
        BeanUtils.copyProperties(kpiGroupType, groupTypeDTO, "name");
        return groupTypeDTO;
    }

    @Override
    public void addSeminarPoint(List<KpiEventUser> eventUsers, EventDTO<EventSeminarDetail> seminarEventDTO) throws IOException {
        List<KpiEventUser> addPointUsers = eventUsers.stream().filter(e -> (e.getType() == 1) || ((e.getType() == 2 || e.getType() == 3) && e.getStatus() == 1)).collect(Collectors.toList());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(seminarEventDTO.getBeginDate().getTime());
            KpiEvent kpiEvent = convertEventDTOToEntity(seminarEventDTO);
            Optional<KpiYearMonth> kpiYearMonthOptional = kpiMonthRepo.findByMonthCurrent();

            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                for (KpiEventUser eventUser : addPointUsers) {
                    if (eventUser.getType().equals(EventUserType.HOST.getValue())) {
                        List<KpiSeminarSurvey> surveysEvaluateHost = kpiSeminarSurveyRepo.findByEvaluatedUsernameAndEvent
                                (kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()), kpiEvent);
                        Float evaluatePoint = 0F;

                        if (surveysEvaluateHost.size() == 0) {
                            evaluatePoint += PointValue.DEFAULT_EFFECTIVE_POINT.getValue();
                        } else {
                            Integer totalEvaluatePoint = surveysEvaluateHost.stream().mapToInt(KpiSeminarSurvey::getRating).sum();
                            evaluatePoint = (float) totalEvaluatePoint / surveysEvaluateHost.size();
                        }

                        if (kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName()) == null) {
                            KpiPoint kpiPoint = new KpiPoint();
                            kpiPoint.setRatedUser(kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()));
                            kpiPoint.setWeekendSeminarPoint(Float.parseFloat(seminarEventDTO.getAdditionalConfig().getHostPoint()) + evaluatePoint);
                            kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());
                            kpiPointRepo.save(kpiPoint);
                        } else {
                            KpiPoint kpiPoint = kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName());
                            if (memberPointOfSaturdaySeminar(eventUser) < PointValue.MAX_WEEKEND_SEMINAR_POINT.getValue()) {
                                if (PointValue.MAX_WEEKEND_SEMINAR_POINT.getValue() - memberPointOfSaturdaySeminar(eventUser) <
                                        Float.parseFloat(seminarEventDTO.getAdditionalConfig().getHostPoint())) {
                                    kpiPoint.setWeekendSeminarPoint(kpiPoint.getWeekendSeminarPoint() + PointValue.MAX_WEEKEND_SEMINAR_POINT.getValue() - memberPointOfSaturdaySeminar(eventUser)
                                            + evaluatePoint);
                                } else {
                                    kpiPoint.setWeekendSeminarPoint(kpiPoint.getWeekendSeminarPoint() + Float.parseFloat(seminarEventDTO.getAdditionalConfig().getHostPoint()) + evaluatePoint);
                                }
                                kpiPointRepo.save(kpiPoint);
                            }
                        }
                    } else {
                        if (kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName()) == null) {
                            KpiPoint kpiPoint = new KpiPoint();
                            kpiPoint.setRatedUser(kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()));
                            kpiPoint.setWeekendSeminarPoint(Float.parseFloat(seminarEventDTO.getAdditionalConfig().getMemberPoint()));
                            kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());
                            kpiPointRepo.save(kpiPoint);
                        } else {
                            KpiPoint kpiPoint = kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName());
                            if (memberPointOfSaturdaySeminar(eventUser) < PointValue.MAX_WEEKEND_SEMINAR_POINT.getValue()) {
                                if (PointValue.MAX_WEEKEND_SEMINAR_POINT.getValue() - memberPointOfSaturdaySeminar(eventUser) <
                                        Float.parseFloat(seminarEventDTO.getAdditionalConfig().getMemberPoint())) {
                                    kpiPoint.setWeekendSeminarPoint(kpiPoint.getWeekendSeminarPoint() + PointValue.MAX_WEEKEND_SEMINAR_POINT.getValue() - memberPointOfSaturdaySeminar(eventUser));
                                } else {
                                    kpiPoint.setWeekendSeminarPoint(kpiPoint.getWeekendSeminarPoint() + Float.parseFloat(seminarEventDTO.getAdditionalConfig().getMemberPoint()));
                                }
                                kpiPointRepo.save(kpiPoint);
                            }
                        }
                    }
                }
            } else {
                for (KpiEventUser eventUser : addPointUsers) {
                    if (eventUser.getType().equals(EventUserType.HOST.getValue())) {
                        Float evaluatePoint = 0F;
                        List<KpiSeminarSurvey> surveysEvaluateHost = kpiSeminarSurveyRepo.findByEvaluatedUsernameAndEvent
                                (kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()), kpiEvent);
                        if (surveysEvaluateHost.size() == 0) {
                            evaluatePoint += PointValue.DEFAULT_EFFECTIVE_POINT.getValue();
                        } else {
                            Integer totalEvaluatePoint = surveysEvaluateHost.stream().mapToInt(KpiSeminarSurvey::getRating).sum();
                            evaluatePoint = (float) totalEvaluatePoint / surveysEvaluateHost.size();
                        }

                        if (kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName()) == null) {
                            KpiPoint kpiPoint = new KpiPoint();
                            kpiPoint.setRatedUser(kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()));
                            kpiPoint.setNormalSeminarPoint(Float.parseFloat(seminarEventDTO.getAdditionalConfig().getHostPoint()) + evaluatePoint);
                            kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());
                            kpiPointRepo.save(kpiPoint);
                        } else {
                            KpiPoint kpiPoint = kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName());
                            if (memberPointOfNormalSeminar(eventUser) < PointValue.MAX_NORMAL_SEMINAR_POINT.getValue()) {
                                if (PointValue.MAX_NORMAL_SEMINAR_POINT.getValue() - memberPointOfNormalSeminar(eventUser) <
                                        Float.parseFloat(seminarEventDTO.getAdditionalConfig().getHostPoint())) {
                                    kpiPoint.setNormalSeminarPoint(kpiPoint.getNormalSeminarPoint() + PointValue.MAX_NORMAL_SEMINAR_POINT.getValue() - memberPointOfNormalSeminar(eventUser)
                                            + evaluatePoint);
                                } else {
                                    kpiPoint.setNormalSeminarPoint(kpiPoint.getNormalSeminarPoint() + Float.parseFloat(seminarEventDTO.getAdditionalConfig().getHostPoint()) + evaluatePoint);
                                }
                                kpiPointRepo.save(kpiPoint);
                            }
                        }
                    } else if (eventUser.getType().equals(EventUserType.MEMBER.getValue())) {
                        if (kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName()) == null) {
                            KpiPoint kpiPoint = new KpiPoint();
                            kpiPoint.setRatedUser(kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()));
                            kpiPoint.setNormalSeminarPoint(Float.parseFloat(seminarEventDTO.getAdditionalConfig().getMemberPoint()));
                            kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());
                            kpiPointRepo.save(kpiPoint);
                        } else {
                            KpiPoint kpiPoint = kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName());
                            if (memberPointOfNormalSeminar(eventUser) < PointValue.MAX_NORMAL_SEMINAR_POINT.getValue()) {
                                if (PointValue.MAX_NORMAL_SEMINAR_POINT.getValue() - memberPointOfNormalSeminar(eventUser) <
                                        Float.parseFloat(seminarEventDTO.getAdditionalConfig().getMemberPoint())) {
                                    kpiPoint.setNormalSeminarPoint(kpiPoint.getNormalSeminarPoint() + PointValue.MAX_NORMAL_SEMINAR_POINT.getValue() - memberPointOfNormalSeminar(eventUser));
                                } else {
                                    kpiPoint.setNormalSeminarPoint(kpiPoint.getNormalSeminarPoint() + Float.parseFloat(seminarEventDTO.getAdditionalConfig().getMemberPoint()));
                                }
                                kpiPointRepo.save(kpiPoint);
                            }
                        }
                    } else if (eventUser.getType().equals(EventUserType.LISTEN.getValue())) {
                        if (kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName()) == null) {
                            KpiPoint kpiPoint = new KpiPoint();
                            kpiPoint.setRatedUser(kpiUserRepo.findByUserName(eventUser.getKpiEventUserPK().getUserName()));
                            kpiPoint.setNormalSeminarPoint(Float.parseFloat(seminarEventDTO.getAdditionalConfig().getListenerPoint()));
                            kpiPoint.setYearMonthId(kpiYearMonthOptional.get().getId());
                            kpiPointRepo.save(kpiPoint);
                        } else {
                            KpiPoint kpiPoint = kpiPointRepo.findByRatedUsername(eventUser.getKpiEventUserPK().getUserName());
                            if (memberPointOfNormalSeminar(eventUser) < PointValue.MAX_NORMAL_SEMINAR_POINT.getValue()) {
                                if (PointValue.MAX_NORMAL_SEMINAR_POINT.getValue() - memberPointOfNormalSeminar(eventUser) <
                                        Float.parseFloat(seminarEventDTO.getAdditionalConfig().getListenerPoint())) {
                                    kpiPoint.setNormalSeminarPoint(kpiPoint.getNormalSeminarPoint() + PointValue.MAX_NORMAL_SEMINAR_POINT.getValue() - memberPointOfNormalSeminar(eventUser));
                                } else {
                                    kpiPoint.setNormalSeminarPoint(kpiPoint.getNormalSeminarPoint() + Float.parseFloat(seminarEventDTO.getAdditionalConfig().getListenerPoint()));
                                }
                                kpiPointRepo.save(kpiPoint);
                            }
                        }
                    }
                }
            }
    }

    private float memberPointOfNormalSeminar(KpiEventUser kpiEventUser) throws IOException {
        List<KpiEvent> seminarEventUserParticipateAsMember = kpiEventRepo.findFinishedSurveySeminarEventByUserAsMember
                (kpiEventUser.getKpiEventUserPK().getUserName());
        List<KpiEvent> seminarEventUserParticipateAsListener = kpiEventRepo.findFinishedSurveySeminarEventByUserAsListener
                (kpiEventUser.getKpiEventUserPK().getUserName());
        List<KpiEvent> seminarEventUserParticipateAsHost = kpiEventRepo.findFinishedSurveySeminarEventByUserAsHost
                (kpiEventUser.getKpiEventUserPK().getUserName());
        Float participatePoint = 0f;

        List<EventDTO<EventSeminarDetail>> memberEvents = convertToDTO(seminarEventUserParticipateAsMember);
        List<EventDTO<EventSeminarDetail>> listenerEvents = convertToDTO(seminarEventUserParticipateAsListener);
        List<EventDTO<EventSeminarDetail>> hostEvents = convertToDTO(seminarEventUserParticipateAsHost);

        for (EventDTO<EventSeminarDetail> eventDTO : memberEvents) {
            participatePoint += Float.parseFloat(eventDTO.getAdditionalConfig().getMemberPoint());
        }

        for(EventDTO<EventSeminarDetail> eventDTO : listenerEvents){
            participatePoint += Float.parseFloat(eventDTO.getAdditionalConfig().getListenerPoint());
        }

        for(EventDTO<EventSeminarDetail> eventDTO : hostEvents){
            participatePoint += Float.parseFloat(eventDTO.getAdditionalConfig().getHostPoint());
        }

        return participatePoint;
    }

    private float memberPointOfSaturdaySeminar(KpiEventUser kpiEventUser) throws IOException {
        Float memberPoint = 0f;
        List<KpiEvent> seminarEventUserParticipateAsHost = kpiEventRepo.findFinishedSurveySeminarEventByUserAsHostAtSaturday
                (kpiEventUser.getKpiEventUserPK().getUserName());

        List<KpiEvent> seminarEventUserParticipateAsMember = kpiEventRepo.findFinishedSurveySeminarEventByUserAsMemberAtSaturday
                (kpiEventUser.getKpiEventUserPK().getUserName());

        List<EventDTO<EventSeminarDetail>> hostEvents = convertToDTO(seminarEventUserParticipateAsHost);
        List<EventDTO<EventSeminarDetail>> memberEvents = convertToDTO(seminarEventUserParticipateAsMember);

        for (EventDTO<EventSeminarDetail> eventDTO : hostEvents) {
            memberPoint += Float.parseFloat(eventDTO.getAdditionalConfig().getHostPoint());
        }

        for (EventDTO<EventSeminarDetail> eventDTO : memberEvents) {
            memberPoint += Float.parseFloat(eventDTO.getAdditionalConfig().getMemberPoint());
        }

        return memberPoint;
    }

    private KpiEvent convertEventDTOToEntity(EventDTO eventDTO){
        KpiEvent kpiEvent = new KpiEvent();

        BeanUtils.copyProperties(eventDTO, kpiEvent);

        return kpiEvent;
    }

    private List<EventDTO<EventSeminarDetail>> convertToDTO(List<KpiEvent> kpiEvents) throws IOException{
        for(KpiEvent event: kpiEvents) {
            event.setGroup(kpiGroupRepo.findGroupByEventId(event.getId()));
            KpiGroupType kpiGroupType = kpiGroupTypeRepo.findByGroupId(event.getGroup().getId());
            event.getGroup().setGroupType(kpiGroupType);
            List<KpiEventUser> kpiEventUsers = kpiEventUserRepo.findByKpiEventId(event.getId());
            event.setKpiEventUserList(kpiEventUsers);
        }
        return kpiEventService.convertSeminarEventEntityToDTO(kpiEvents);
    }
}
