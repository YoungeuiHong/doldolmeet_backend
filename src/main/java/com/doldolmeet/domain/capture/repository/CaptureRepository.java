package com.doldolmeet.domain.capture.repository;

import com.doldolmeet.domain.capture.entity.Capture;
import com.doldolmeet.domain.users.fan.entity.Fan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaptureRepository extends JpaRepository<Capture, Long> {
    List<Capture> findByFan(Fan fan);
    List<Capture> findByFanIdAndFanMeetingId(Long fanId, Long fanMeetingId);
    List<Capture> findByFanIdAndFanMeetingIdAndIdolId(Long fanId, Long fanMeetingId, Long idolId);

}
