package com.doldolmeet.domain.team.controller;

import com.doldolmeet.domain.team.dto.request.AddIdolToTeamRequestDto;
import com.doldolmeet.domain.team.dto.request.CreateTeamRequestDto;
import com.doldolmeet.domain.team.service.TeamService;
import com.doldolmeet.utils.Message;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ApiResponse
public class TeamController {
    private final TeamService teamService;

    // 팀 생성
    @PostMapping("/team")
    public ResponseEntity<Message> createTeam(@RequestBody CreateTeamRequestDto requestDto, HttpServletRequest request) {
        return teamService.createTeam(requestDto, request);
    }

    // 아이돌을 팀에 추가
    @PostMapping("/team/idol")
    public ResponseEntity<Message> addIdolToTeam(@RequestBody AddIdolToTeamRequestDto requestDto, HttpServletRequest request) {
        return teamService.addIdoToTeam(requestDto, request);
    }
}
