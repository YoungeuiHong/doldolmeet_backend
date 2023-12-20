package com.doldolmeet.domain.fanMeeting.entity;

import com.doldolmeet.domain.memo.entity.Memo;
import com.doldolmeet.domain.users.fan.entity.Fan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class FanToFanMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    // 팬
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_id", referencedColumnName = "id")
    private Fan fan;

    // 팬미팅
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_meeting_id", referencedColumnName = "id")
    private FanMeeting fanMeeting;

    // 신청 상태
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private FanMeetingApplyStatus fanMeetingApplyStatus;

    @Column(nullable = false)
    private Long orderNumber;

    @Column(nullable = false)
    private String chatRoomId;

    @Column
    private Long gameScore;

    public void setUserAndFanMeeting(Fan fan, FanMeeting fanMeeting) {
        this.fan = fan;
        this.fanMeeting = fanMeeting;
    }


}
