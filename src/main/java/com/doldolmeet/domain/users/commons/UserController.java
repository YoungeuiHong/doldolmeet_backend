package com.doldolmeet.domain.users.commons;


import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.utils.Message;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@ApiResponse
public class UserController {
    // 프로필 이미지 업로드
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/uploadProfileImage")
    public ResponseEntity<Message> uploadProfileImage(MultipartFile file, HttpServletRequest request) {
        String username = jwtUtil.getClaims(request).getSubject();
        String role = (String)jwtUtil.getClaims(request).get("auth");
        return userService.saveFanProfileImg(file, username, role);
    }
}
