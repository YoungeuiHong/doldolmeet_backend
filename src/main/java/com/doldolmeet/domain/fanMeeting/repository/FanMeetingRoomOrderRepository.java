package com.doldolmeet.domain.fanMeeting.repository;

import com.doldolmeet.domain.fanMeeting.entity.FanMeetingRoomOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FanMeetingRoomOrderRepository extends JpaRepository<FanMeetingRoomOrder, Long> {
    @Query("select f from FanMeetingRoomOrder f join fetch f.fanMeeting where f.fanMeeting.id = :fanMeetingId")
    List<FanMeetingRoomOrder> findByFanMeetingId(@Param("fanMeetingId") Long fanMeetingId);

    @Query("select f from FanMeetingRoomOrder f join fetch f.fanMeeting where f.fanMeeting.id = :fanMeetingId and f.currentRoom = :currentRoom")
    Optional<FanMeetingRoomOrder> findByFanMeetingIdAndCurrentRoom(@Param("fanMeetingId") Long fanMeetingId, @Param("currentRoom") String currentRoom);

    @Query("select f from FanMeetingRoomOrder f join fetch f.fanMeeting where f.fanMeeting.id = :fanMeetingId and f.nextRoom = :nextRoom")
    Optional<FanMeetingRoomOrder> findByFanMeetingIdAndNextRoom(@Param("fanMeetingId") Long fanMeetingId, @Param("nextRoom") String nextRoom);


    @Query("select f from FanMeetingRoomOrder f join fetch f.fanMeeting where f.fanMeeting.id = :fanMeetingId and f.type = 'mainWaitRoom'")
    Optional<FanMeetingRoomOrder> getMainWaitingRoomfindByFanMeetingId(@Param("fanMeetingId") Long fanMeetingId);

    List<FanMeetingRoomOrder> findByFanMeetingIdAndType(Long fanMeetingId, String type);

    Optional<FanMeetingRoomOrder> findByFanMeetingIdAndTypeAndNickname(Long fanMeetingId, String type, String nickname);

    @Query("select f from FanMeetingRoomOrder f join fetch f.fanMeeting where f.fanMeeting.id = :fanMeetingId and f.type = 'gameRoom'")
    Optional<FanMeetingRoomOrder> getGameRoomFindByFanMeetingId(@Param("fanMeetingId") Long fanMeetingId);
}
