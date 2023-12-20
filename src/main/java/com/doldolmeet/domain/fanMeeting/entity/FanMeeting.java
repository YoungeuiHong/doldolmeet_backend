package com.doldolmeet.domain.fanMeeting.entity;

import com.doldolmeet.domain.capture.entity.Capture;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.teleRoom.entity.TeleRoom;
import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import com.doldolmeet.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    // 그룹에 대한 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // 팬미팅 시작 시간 및 끝 시간
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // 팬미팅 이름
    @Column(nullable = false)
    private String fanMeetingName;

    // 팬미팅 이미지
    @Column
    private String fanMeetingImgUrl;

    // 팬미팅 인원수
    @Column(nullable = false)
    private Integer capacity;

    @Column
    private Long nextOrder;

    @Column
    private String chatRoomId;

    @Column
    private Boolean isRoomsCreated;

    @Column
    private Boolean isStarted;

    // 팬미팅 신청서들
    @OneToMany(mappedBy = "fanMeeting", cascade = CascadeType.ALL)
    private List<FanToFanMeeting> fanToFanMeetings = new ArrayList<>();

    // 대기방 해시맵(메인 대기룸 + 아이돌별 대기룸)
    @OneToMany(mappedBy = "fanMeeting", cascade = CascadeType.ALL)
    private List<WaitRoom> waitRooms = new ArrayList<>();

    // 통화방 해시맵
    @OneToMany(mappedBy = "fanMeeting", cascade = CascadeType.ALL)
    private List<TeleRoom> teleRooms = new ArrayList<>();

    @OneToMany(mappedBy = "fanMeeting", cascade = CascadeType.ALL)
    private List<Video> videos = new ArrayList<>();

    @OneToMany(mappedBy = "fanMeeting", cascade = CascadeType.ALL)
    private List<Capture> captures = new ArrayList<>();
  
    @OneToMany(mappedBy = "fanMeeting", cascade = CascadeType.ALL)
    private List<FanMeetingRoomOrder> fanMeetingRoomOrders = new ArrayList<>();
}
