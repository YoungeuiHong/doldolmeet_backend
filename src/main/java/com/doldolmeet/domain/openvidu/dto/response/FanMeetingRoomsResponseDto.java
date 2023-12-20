package com.doldolmeet.domain.openvidu.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanMeetingRoomsResponseDto {
    List<String> roomIds = new ArrayList<>();
}
