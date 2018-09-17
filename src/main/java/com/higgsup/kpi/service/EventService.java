package com.higgsup.kpi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.higgsup.kpi.dto.EventDTO;
import com.higgsup.kpi.dto.EventSeminarDetail;
import com.higgsup.kpi.dto.EventSupportDetail;
import com.higgsup.kpi.dto.EventClubDetail;

import java.io.IOException;
import java.util.List;

import com.higgsup.kpi.dto.EventDTO;

import java.util.List;

import com.higgsup.kpi.dto.EventClubDetail;

public interface EventService {
    List<EventDTO> getAllEvent() throws IOException;

    EventDTO createSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) throws IOException, NoSuchFieldException;

    EventDTO updateSupportEvent(EventDTO<List<EventSupportDetail>> supportDTO) throws IOException, NoSuchFieldException;

    EventDTO createClub(EventDTO<EventClubDetail> eventDTO) throws IOException;

    EventDTO updateClub(EventDTO<EventClubDetail> eventDTO) throws IOException;


    EventDTO createSeminar(EventDTO<EventSeminarDetail> eventDTO) throws IOException;
    EventDTO updateSeminar(EventDTO<EventSeminarDetail> eventDTO) throws IOException;
}
