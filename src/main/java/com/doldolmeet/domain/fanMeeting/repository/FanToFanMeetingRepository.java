package com.doldolmeet.domain.fanMeeting.repository;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.entity.FanToFanMeeting;
import com.doldolmeet.domain.users.fan.entity.Fan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FanToFanMeetingRepository extends JpaRepository<FanToFanMeeting, Long> {
    Optional<FanToFanMeeting> findByFanAndFanMeeting(Fan fan, FanMeeting fanMeeting);

    // 팬미팅Id와 username을 통해 FantoFanMeeting을 구한다.

    @Query("SELECT ftfm from FanToFanMeeting ftfm WHERE ftfm.fanMeeting.id = :fanMeetingId AND ftfm.fan.userCommons.username = :username")
    Optional<FanToFanMeeting> findByFanMeetingIdAndFanUsername(@Param("fanMeetingId") Long fanMeetingId, @Param("username") String username);


    @Query("select f from FanToFanMeeting f where f.fanMeeting.startTime >= :now and f.fan = :fan order by f.fanMeeting.startTime asc")
    List<FanToFanMeeting> findFanToFanMeetingsByFanByStartTimeAfter(LocalDateTime now, Fan fan);

    @Query("select f from FanToFanMeeting f where f.fanMeeting.endTime < :now and f.fan = :fan order by f.fanMeeting.endTime asc")
    List<FanToFanMeeting> findFanToFanMeetingsByFanByEndTimeBefore(LocalDateTime now, Fan fan);

    @Query("select f from FanToFanMeeting f where f.fan = :fan order by f.fanMeeting.startTime asc")
    List<FanToFanMeeting> findAllByFan(Fan fan);

    @Query("select f from FanToFanMeeting f where f.fanMeeting.startTime <= :now and f.fanMeeting.endTime > :now and f.fan = :fan order by f.fanMeeting.startTime asc")
    List<FanToFanMeeting> findFanToFanMeetingsByFanByStartTimeBeforeAndEndTimeAfter(LocalDateTime now, Fan fan);
}
