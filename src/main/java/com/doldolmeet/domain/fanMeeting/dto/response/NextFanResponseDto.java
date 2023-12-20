package com.doldolmeet.domain.fanMeeting.dto.response;

import com.doldolmeet.domain.fanMeeting.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextFanResponseDto {
    private String username;
    private String waitRoomId;
    private String connectionId;
    private RoomType roomType;
}

// 현재 있는 세션ID API
