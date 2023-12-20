package com.doldolmeet.domain.waitRoom.entity;


import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class WaitRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String roomId;

    @OneToMany(mappedBy = "waitRoom", cascade = CascadeType.ALL)
    private List<WaitRoomFan> waitRoomFans = new ArrayList<>();

    public void createWaitRoomId() {
        this.roomId = UUID.randomUUID().toString();
    }

    public void addWaitRoomFan(WaitRoomFan waitRoomFan) {
        this.waitRoomFans.add(waitRoomFan);
    }

    public void removeWaitRoomFan(WaitRoomFan waitRoomFan) {
        this.waitRoomFans.remove(waitRoomFan);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_meeting_id")
    private FanMeeting fanMeeting;
}
