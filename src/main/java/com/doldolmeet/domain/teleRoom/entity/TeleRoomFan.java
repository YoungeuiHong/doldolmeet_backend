package com.doldolmeet.domain.teleRoom.entity;

import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeleRoomFan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    // 커넥션Id
    @Column
    private String connectionId;

    @Column
    private Long orderNumber;

    @Column
    private String connectionToken;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tele_room_id")
    private TeleRoom teleRoom;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_id")
    private Fan fan;
}
