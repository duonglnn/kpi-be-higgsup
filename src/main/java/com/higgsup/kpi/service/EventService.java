package com.higgsup.kpi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.higgsup.kpi.dto.*;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public interface EventService {
    List<EventDTO> getAllEvent() throws IOException;

    List<EventDTO> getEventCreatedByUser(String username) throws IOException;

    EventDTO createSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) throws IOException, NoSuchFieldException;

    EventDTO updateSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) throws IOException, NoSuchFieldException;

    EventDTO createClub(EventDTO<EventClubDetail> eventDTO) throws IOException;

    EventDTO updateClub(EventDTO<EventClubDetail> eventDTO) throws IOException;

    EventDTO createSeminar(EventDTO<EventSeminarDetail> eventDTO) throws IOException;

    EventDTO updateSeminar(EventDTO<EventSeminarDetail> eventDTO) throws IOException;

    EventDTO confirmOrCancelEvent(EventDTO eventDTO) throws IOException, NoSuchFieldException, IllegalAccessException;

    EventDTO createTeamBuildingEvent(EventDTO<EventTeamBuildingDetail> eventDTO) throws IOException;
}
