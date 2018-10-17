package com.higgsup.kpi.service;

import com.higgsup.kpi.dto.BestPerformancesDTO;
import com.higgsup.kpi.dto.PointDTO;
import com.higgsup.kpi.dto.RankingDTO;

import java.util.List;

public interface RankingService {
    List<RankingDTO> showNormalPointRanking(Integer currentPage);

    List<RankingDTO> showFamedPointRanking(Integer currentPage);

    List<BestPerformancesDTO> showBestPerformancesOfEachMonth();

}
