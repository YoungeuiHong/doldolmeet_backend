package com.doldolmeet.domain.fanMeeting.sse;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserNameAndOrderNumber {
    private String username;
    private Long orderNumber;
    private Long cnt;
}
