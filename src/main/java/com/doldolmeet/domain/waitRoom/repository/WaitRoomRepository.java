package com.doldolmeet.domain.waitRoom.repository;

import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitRoomRepository extends JpaRepository<WaitRoom, Long> {
    Optional<WaitRoom> findByRoomId(String waitRoomId);

//    Optional<WaitRoom> findByFanMeetingIdAndFanId(Long fanMeetingId, Long id);
}
