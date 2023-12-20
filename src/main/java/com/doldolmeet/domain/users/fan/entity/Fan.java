package com.doldolmeet.domain.users.fan.entity;

import com.doldolmeet.domain.capture.entity.Capture;
import com.doldolmeet.domain.fanMeeting.entity.FanToFanMeeting;
import com.doldolmeet.domain.commons.UserCommons;
import com.doldolmeet.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Embedded
    private UserCommons userCommons;

    @OneToMany(mappedBy = "fan", cascade = CascadeType.ALL)
    private List<FanToFanMeeting> fanToFanMeetings = new ArrayList<>();

    @OneToMany(mappedBy = "fan", cascade = CascadeType.ALL)
    private List<Capture> captures = new ArrayList<>();
}
