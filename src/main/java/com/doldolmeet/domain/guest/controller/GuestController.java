package com.doldolmeet.domain.guest.controller;


import com.doldolmeet.domain.guest.dto.request.LoginRequestDto;
import com.doldolmeet.domain.guest.dto.request.SignupRequestDto;
import com.doldolmeet.domain.guest.service.GuestService;
import com.doldolmeet.utils.Message;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ApiResponse
@Slf4j
public class GuestController {
    private final GuestService guestService;

    // 1. 회원 가입 API
    @PostMapping("/signup")
    public ResponseEntity<Message> signup(@RequestBody @Valid SignupRequestDto signupRequestDto) {
        return guestService.signup(signupRequestDto);
    }

    // 2. 로그인 API
    @PostMapping("/login")
    public ResponseEntity<Message> login(@RequestBody @Valid LoginRequestDto loginRequestDto, HttpServletResponse response) {
        return guestService.login(loginRequestDto, response);
    }

}
