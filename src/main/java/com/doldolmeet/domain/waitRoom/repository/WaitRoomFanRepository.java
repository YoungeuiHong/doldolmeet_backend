package com.doldolmeet.domain.waitRoom.repository;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import com.doldolmeet.domain.waitRoom.entity.WaitRoomFan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WaitRoomFanRepository extends JpaRepository<WaitRoomFan, Long> {
    @Query("select w from WaitRoomFan w join fetch w.waitRoom where w.fan.id = :fanId and w.waitRoom.fanMeeting.id = :fanMeetingId")
    Optional<WaitRoomFan> findByFanIdAndWaitRoomId(@Param("fanId") Long fanId, @Param("fanMeetingId") Long fanMeetingId);

    // 해당 대기방에 있는 WaitRoomFan 중 가장 order가 낮은 사람을 가져온ek.
    @Query("SELECT w FROM WaitRoomFan w WHERE w.waitRoom = :waitRoom ORDER BY w.orderNumber ASC LIMIT 1")
    Optional<WaitRoomFan> findFirstByWaitRoomOrderByOrderAsc(@Param("waitRoom") WaitRoom waitRoom);
}
