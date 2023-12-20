package com.doldolmeet.domain.capture.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptureDto {
    private Long captureId;
    private String captureName;
    private Long fanMeetingId;
    private String nickName;
    private String captureUrl;
    private Long fanId;
}