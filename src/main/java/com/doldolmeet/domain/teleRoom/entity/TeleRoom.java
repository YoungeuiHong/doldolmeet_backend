package com.doldolmeet.domain.teleRoom.entity;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.waitRoom.entity.WaitRoomFan;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeleRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column
    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_meeting_id")
    private FanMeeting fanMeeting;

    @OneToOne(mappedBy = "teleRoom", cascade = CascadeType.ALL)
    private TeleRoomFan teleRoomFan;
}
