package com.doldolmeet.recording.service;
import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.users.fan.repository.FanRepository;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.recording.RecordingInfoRepository.RecordingInfoRepository;
import com.doldolmeet.recording.entity.RecordingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingInfoService {
    private final RecordingInfoRepository recordingInfoRepository;
    private final FanMeetingRepository fanMeetingRepository;
    private final FanRepository fanRepository;
    private final IdolRepository idolRepository;

//    public RecordingInfoService(RecordingInfoRepository recordingInfoRepository, FanMeetingRepository fanMeetingRepository, FanRepository fanRepository, IdolRepository idolRepository) {
//        this.recordingInfoRepository = recordingInfoRepository;
//        this.fanMeetingRepository = fanMeetingRepository;
//        this.fanRepository = fanRepository;
//        this.idolRepository = idolRepository;
//    }

    @Transactional
    public void saveRecordingInfo(Long fanMeetingId, String fanUserName, String idolKinckname, String fileName, String recordingId  ) {


        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).get();
        Fan fan = fanRepository.findByUserCommonsUsername(fanUserName).get();
        Idol idol = idolRepository.findByUserCommonsNickname(idolKinckname).get();


        RecordingInfo recordingInfo = RecordingInfo.builder()
                .fanMeeting(fanMeeting)
                .fan(fan)
                .idol(idol)
                .fileName(fileName)
                .recordingId(recordingId)
                .build();

        // Save the RecordingInfo instance to the database
        recordingInfoRepository.save(recordingInfo);
    }

    @Transactional
    public String findRecordingId(Long fanMeetingId, String fan, String idol) {
        Long fanId = fanRepository.findByUserCommonsUsername(fan).get().getId();
        Long idolId = idolRepository.findByUserCommonsNickname(idol).get().getId();

        Optional<RecordingInfo> recordingInfo = recordingInfoRepository.findByFanMeetingIdAndFanIdAndIdolId(fanMeetingId, fanId, idolId);
        return recordingInfo.get().getRecordingId();
    }

    @Transactional
    public List<RecordingInfo> findRecordingInfos(Long fanMeetingId, String fan) {
        Long fanId = fanRepository.findByUserCommonsUsername(fan).get().getId();
        List<RecordingInfo> recordingInfos = recordingInfoRepository.findByFanMeetingIdAndFanId(fanMeetingId, fanId);

        log.info("Fan: {}, recordingInfos: {}", fan, recordingInfos);
        return recordingInfos;
    }
}
