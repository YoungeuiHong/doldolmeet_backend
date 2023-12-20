package com.doldolmeet.domain.fanMeeting.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanMeetingRoomOrderDto {
    private String currentRoom; // current sessionId
    private String type;
    private String motionType;
    private String idolName;
    private Long fanMeetingId;
    private Long id;
}
