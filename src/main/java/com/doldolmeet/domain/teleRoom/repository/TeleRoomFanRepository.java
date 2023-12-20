package com.doldolmeet.domain.teleRoom.repository;

import com.doldolmeet.domain.teleRoom.entity.TeleRoomFan;
import com.doldolmeet.domain.waitRoom.entity.WaitRoomFan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeleRoomFanRepository extends JpaRepository<TeleRoomFan, Long> {

    @Query("select t from TeleRoomFan t join fetch t.teleRoom where t.fan.id = :fanId and t.teleRoom.fanMeeting.id = :fanMeetingId")
    Optional<TeleRoomFan> findByFanIdAndTeleRoomId(@Param("fanId") Long id, @Param("fanMeetingId") Long fanMeetingId);
}
