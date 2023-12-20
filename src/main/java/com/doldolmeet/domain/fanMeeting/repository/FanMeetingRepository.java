package com.doldolmeet.domain.fanMeeting.repository;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.idol.entity.Idol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface FanMeetingRepository extends JpaRepository<FanMeeting, Long> {
    List<FanMeeting> findFanMeetingsByEndTimeAfter(LocalDateTime now);
    List<FanMeeting> findFanMeetingsByEndTimeBefore(LocalDateTime now);

    // 가장 이른 팬미팅 조회(팬이 신청)
    @Query("SELECT ftfm.fanMeeting FROM FanToFanMeeting ftfm WHERE ftfm.fan = :fan AND ftfm.fanMeeting.startTime >= :midNightTime AND ftfm.fanMeeting.endTime > :currentTime AND ftfm.fanMeeting.startTime < :tomorrowMidnightTime ORDER BY ftfm.fanMeeting.endTime ASC LIMIT 1")
    Optional<FanMeeting> findFanMeetingsByFan(@Param("fan") Fan fan, @Param("midNightTime") LocalDateTime midNightTime, @Param("currentTime") LocalDateTime currentTime, @Param("tomorrowMidnightTime") LocalDateTime tomorrowMidnightTime);


    @Query("SELECT f FROM FanMeeting f WHERE f.startTime >= :midNightTime AND f.startTime < :tomorrowMidnightTime ORDER BY f.startTime ASC")
    List<FanMeeting> findTodayFanMeetings(@Param("midNightTime") LocalDateTime midNightTime, @Param("tomorrowMidnightTime") LocalDateTime tomorrowMidnightTime);

    // 아이돌을 전달해서 팀을 찾고, 팀을 통해 팬미팅들을 조회하는데, 오늘 열리는 팬미팅 중 하나 뽑기
    @Query("SELECT f FROM FanMeeting f WHERE f.team = :team AND f.startTime >= :midNightTime AND f.endTime > :currentTime AND f.startTime < :tomorrowMidnightTime ORDER BY f.endTime ASC LIMIT 1")
    Optional<FanMeeting> findFanMeetingsByTeamOne(@Param("team") Team team, @Param("midNightTime") LocalDateTime midNightTime, @Param("currentTime") LocalDateTime currentTime, @Param("tomorrowMidnightTime") LocalDateTime tomorrowMidnightTime);

    Optional<FanMeeting> findById(Long id);
}
