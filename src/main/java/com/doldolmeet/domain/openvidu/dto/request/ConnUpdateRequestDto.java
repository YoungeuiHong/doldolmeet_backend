package com.doldolmeet.domain.openvidu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnUpdateRequestDto {
    private String connectionId;
    private String connectionToken;
    private Long fanMeetingId;
    private String username;
    private String roomId;
    private String type;
}
