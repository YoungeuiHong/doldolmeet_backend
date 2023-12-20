package com.doldolmeet.recording.entity;


import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.idol.entity.Idol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fanMeetingId", referencedColumnName = "id")
    private  FanMeeting fanMeeting;

    @ManyToOne
    @JoinColumn(name = "fanId")
    private Fan fan;

    @ManyToOne
    @JoinColumn(name = "idolId")
    private Idol idol;

    @Column
    private String fileName;

    @Column
    private String recordingId;
}