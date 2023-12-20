package com.doldolmeet.domain.guest.service;

import com.doldolmeet.domain.guest.dto.request.LoginRequestDto;
import com.doldolmeet.domain.commons.Role;
import com.doldolmeet.domain.guest.dto.request.SignupRequestDto;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.team.repository.TeamRepository;
import com.doldolmeet.domain.users.admin.entity.Admin;
import com.doldolmeet.domain.users.admin.repository.AdminRepository;
import com.doldolmeet.domain.commons.UserCommons;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.fan.repository.FanRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.security.password.PwEncoder;
import com.doldolmeet.utils.Message;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import static com.doldolmeet.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class GuestService {
    public final FanRepository fanRepository;
    public final IdolRepository idolRepository;
    public final AdminRepository adminRepository;
    public final TeamRepository teamRepository;
    public final JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<Message> signup(SignupRequestDto requestDto) {
        // 받은 username으로 이미 아이디가 존재하는지 확인
        String username = requestDto.getUsername();
        String password = PwEncoder.encoder.encode(requestDto.getPassword());

        validateSignup(requestDto);

        // 공통 컬럼 생성
        UserCommons userCommons = UserCommons.builder()
                .username(username)
                .password(password)
                .nickname(requestDto.getNickname())
                .profileImgUrl(requestDto.getProfileImgUrl())
                .thumbNailImgUrl(requestDto.getThumbNailImgUrl())
                .role(requestDto.getRole())
                .build();

        // 팬인 경우,
        if (requestDto.getRole().equals(Role.FAN)) {
            Fan fan = Fan.builder()
                    .userCommons(userCommons)
                    .build();

            fanRepository.save(fan);
        }
        // 아이돌인 경우,
        else if (requestDto.getRole().equals(Role.IDOL)) {
            Idol idol = Idol.builder()
                            .userCommons(userCommons)
                            .build();

            Optional<Team> team = teamRepository.findByTeamName(requestDto.getTeamName());

            if (!team.isPresent()) {
                throw new CustomException(TEAM_NOT_FOUND);
            }
            idol.createTeleRoomId();
            idol.createWaitRoomId();
            idol.setTeam(team.get());
            idolRepository.save(idol);
        }

        // 어드민인 경우,
        else if (requestDto.getRole().equals(Role.ADMIN)) {
            Admin admin = Admin.builder()
                    .userCommons(userCommons)
                    .build();

            adminRepository.save(admin);
        }

        return new ResponseEntity<>(new Message("회원 가입 성공", null), HttpStatus.OK);
    }

    private void validateSignup(SignupRequestDto requestDto) {
        if (requestDto.getRole().equals(Role.FAN)) {
            Optional<Fan> fan = fanRepository.findByUserCommonsUsername(requestDto.getUsername());

            if (fan.isPresent()) {
                throw new CustomException(ALREADY_JOIN_USER);
            }
        }

        else if (requestDto.getRole().equals(Role.IDOL)) {
            Optional<Idol> idol = idolRepository.findByUserCommonsUsername(requestDto.getUsername());

            if (idol.isPresent()) {
                throw new CustomException(ALREADY_JOIN_USER);
            }
        }

        else if (requestDto.getRole().equals(Role.ADMIN)) {
            Optional<Admin> admin = adminRepository.findByUserCommonsUsername(requestDto.getUsername());

            if (admin.isPresent()) {
                throw new CustomException(ALREADY_JOIN_USER);
            }
        }
    }

    @Transactional
    public ResponseEntity<Message> login(LoginRequestDto requestDto, HttpServletResponse response) {
        String username = requestDto.getUsername();
        String password = requestDto.getPassword();

        Optional<Fan> fan = fanRepository.findByUserCommonsUsername(username);
        Optional<Idol> idol = idolRepository.findByUserCommonsUsername(username);
        Optional<Admin> admin = adminRepository.findByUserCommonsUsername(username);
        String jwtToken;

        if (fan.isPresent()) {
            // 비번 검사,
            if (!PwEncoder.encoder.matches(password, fan.get().getUserCommons().getPassword())) {
                throw new CustomException(INVALID_PASSWORD);
            }

            jwtToken = jwtUtil.createToken(username, fan.get().getUserCommons().getRole(), fan.get().getUserCommons().getNickname(), fan.get().getUserCommons().getProfileImgUrl(), fan.get().getUserCommons().getThumbNailImgUrl());
        }

        else if (idol.isPresent()) {
            if (!PwEncoder.encoder.matches(password, idol.get().getUserCommons().getPassword())) {
                throw new CustomException(INVALID_PASSWORD);
            }

            jwtToken = jwtUtil.createToken(username, idol.get().getUserCommons().getRole(), idol.get().getUserCommons().getNickname(), idol.get().getUserCommons().getProfileImgUrl(), idol.get().getUserCommons().getThumbNailImgUrl());
        }

        else if (admin.isPresent()) {
            if (!PwEncoder.encoder.matches(password, admin.get().getUserCommons().getPassword())) {
                throw new CustomException(INVALID_PASSWORD);
            }

            jwtToken = jwtUtil.createToken(username, admin.get().getUserCommons().getRole(), admin.get().getUserCommons().getNickname(), admin.get().getUserCommons().getProfileImgUrl(), admin.get().getUserCommons().getThumbNailImgUrl());
        }

        else {
            throw new CustomException(USER_NOT_FOUND);
        }

        // jwtToken 헤더에 넣어주기
        response.addHeader("Authorization", jwtToken);

        return new ResponseEntity<>(new Message("로그인 성공", null), HttpStatus.OK);
    }
}
