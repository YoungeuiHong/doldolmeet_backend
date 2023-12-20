package com.doldolmeet.domain.team.service;

import com.doldolmeet.domain.team.dto.request.AddIdolToTeamRequestDto;
import com.doldolmeet.domain.team.dto.request.CreateTeamRequestDto;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.team.repository.TeamRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.utils.Message;
import com.doldolmeet.utils.UserUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.doldolmeet.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final IdolRepository idolRepository;
    private final JwtUtil jwtUtil;
    private final UserUtils userUtils;
    private Claims claims;


    @Transactional
    public ResponseEntity<Message> createTeam(CreateTeamRequestDto requestDto, HttpServletRequest request) {
        teamRepository.findByTeamName(requestDto.getTeamName())
                .ifPresent(team -> {
                    throw new CustomException(TEAM_ALREADY_EXIST);
                });

        claims = jwtUtil.getClaims(request);
        userUtils.checkIfAdmin(claims);
        userUtils.checkIfUserExist(claims);

        Team team = new Team(requestDto);
        teamRepository.save(team);

        return new ResponseEntity<>(new Message("팀 생성 성공", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> addIdoToTeam(AddIdolToTeamRequestDto requestDto, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        userUtils.checkIfAdmin(claims);
        userUtils.checkIfUserExist(claims);

        Optional<Team> team = teamRepository.findByTeamName(requestDto.getTeamName());
        Optional<Idol> idol = idolRepository.findByUserCommonsUsername(requestDto.getIdolName());

        if (!team.isPresent()) {
            throw new CustomException(TEAM_NOT_FOUND);
        }

        if (!idol.isPresent()) {
            throw new CustomException(IDOL_NOT_FOUND);
        }

        Idol idolEntity = idol.get();

        idolEntity.setTeam(team.get());
        idolRepository.save(idolEntity);

        return new ResponseEntity<>(new Message("아이돌 팀에 추가 성공", null), HttpStatus.OK);
    }
}
