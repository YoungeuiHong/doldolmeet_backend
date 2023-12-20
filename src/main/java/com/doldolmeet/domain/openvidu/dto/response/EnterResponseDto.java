package com.doldolmeet.domain.openvidu.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterResponseDto {
    private String token;
    private String teleRoomId;
    private String waitRoomId;
    private String mainWaitRoomId;
}
