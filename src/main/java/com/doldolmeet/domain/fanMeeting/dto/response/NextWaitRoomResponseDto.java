package com.doldolmeet.domain.fanMeeting.dto.response;

import com.doldolmeet.domain.fanMeeting.entity.RoomType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextWaitRoomResponseDto {
    private String roomId;
    private RoomType roomType;
}
