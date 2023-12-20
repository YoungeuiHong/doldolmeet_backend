package com.doldolmeet.domain.guest.dto.request;

import com.doldolmeet.domain.commons.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SignupRequestDto {
    // 알파벳 소문자(a~z), 숫자(0~9)
    @NotBlank
    @Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)[a-z\\d]{4,10}", message = "입력 양식과 맞지 않습니다.")
    private String username;

    // 알파벳 대소문자(a~z, A~Z), 숫자(0~9)
    @NotBlank
    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@$!%*?&])[A-Za-z\\d$@$!%*?&]{8,15}", message = "입력 양식과 맞지 않습니다.")
    private String password;

    private Role role = Role.FAN;

    private String nickname;
    private String teamName;
    private String profileImgUrl;
    private String thumbNailImgUrl;
}
