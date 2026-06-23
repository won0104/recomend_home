package com.ssafy.prj.model.service.neighborhood;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendRequest;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;

public interface NeighborhoodRecommendService {

    NeighborhoodRecommendResponse recommend(NeighborhoodRecommendRequest request) throws Exception;
}
