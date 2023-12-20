package com.doldolmeet.domain.fanMeeting.dto.response;

import com.doldolmeet.domain.fanMeeting.entity.FanMeetingApplyStatus;
import com.doldolmeet.domain.fanMeeting.entity.FanToFanMeeting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanToFanMeetingResponseDto {
    private Long id;
    private Long fanId;
    private Long fanMeetingId;
    private FanMeetingApplyStatus fanMeetingApplyStatus;
    private Long orderNumber;
    private String chatRoomId;
    private String teamName;
    private Long gameScore;
}
