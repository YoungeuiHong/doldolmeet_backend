package com.doldolmeet.domain.waitRoom.entity;

import com.doldolmeet.domain.users.fan.entity.Fan;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitRoomFan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    // 커넥션Id
    @Column
    private String connectionId;

    @Column
    private Long orderNumber;
    // orderNumber를 TeleRoomFan에 추가하기

    @Column
    private String connectionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wait_room_id")
    private WaitRoom waitRoom;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_id")
    private Fan fan;
}
