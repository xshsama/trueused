package com.xsh.trueused.interaction.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.interaction.dto.BrowsingHistoryDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.interaction.service.BrowsingHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class BrowsingHistoryController {

    private final BrowsingHistoryService browsingHistoryService;

    @GetMapping
    public Streamable<BrowsingHistoryDTO> getMyHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "viewedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return browsingHistoryService.getUserHistory(principal.getId(), pageable);
    }
}
