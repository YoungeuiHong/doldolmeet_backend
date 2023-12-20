package com.doldolmeet.domain.fanMeeting.sse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SseEvent {
    private String username;
    private String id;
    private String name;
    private Object data;
}
