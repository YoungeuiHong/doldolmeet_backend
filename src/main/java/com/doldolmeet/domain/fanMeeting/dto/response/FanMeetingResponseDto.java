package com.doldolmeet.domain.fanMeeting.dto.response;


import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanMeetingResponseDto {
    private Long id;
    private String imgUrl;
    private String title;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    public FanMeetingResponseDto(FanMeeting fanMeeting) {
        this.id = fanMeeting.getId();
        this.imgUrl = fanMeeting.getFanMeetingImgUrl();
        this.title = fanMeeting.getFanMeetingName();
        this.startTime = fanMeeting.getStartTime();
        this.endTime =fanMeeting.getEndTime();
    }
}
