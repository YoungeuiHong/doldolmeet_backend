package com.doldolmeet.domain.teleRoom.repository;

import com.doldolmeet.domain.teleRoom.entity.TeleRoom;
import com.doldolmeet.domain.teleRoom.entity.TeleRoomFan;
import com.doldolmeet.domain.waitRoom.entity.WaitRoomFan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TeleRoomRepository extends JpaRepository<TeleRoom, Long> {
    Optional<TeleRoom> findByRoomId(String roomId);

}
