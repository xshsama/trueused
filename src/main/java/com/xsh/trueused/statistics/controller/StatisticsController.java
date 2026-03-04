package com.xsh.trueused.statistics.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.statistics.dto.StatisticsDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.statistics.service.StatisticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/seller")
    public StatisticsDTO getSellerStatistics(
            @RequestParam(defaultValue = "近7日") String range,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return statisticsService.getSellerStatistics(principal.getId(), range);
    }
}
