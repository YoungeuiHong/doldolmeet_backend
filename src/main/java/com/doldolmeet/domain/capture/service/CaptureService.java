package com.doldolmeet.domain.capture.service;

import com.doldolmeet.domain.capture.dto.CaptureDto;
import com.doldolmeet.domain.capture.entity.Capture;
import com.doldolmeet.domain.capture.repository.CaptureRepository;
import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.s3.service.AwsS3Service;
import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.utils.Message;
import com.doldolmeet.utils.UserUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaptureService {
    private final AwsS3Service awsS3Service;
    private final FanMeetingRepository fanMeetingRepository;
    private final IdolRepository idolRepository;
    private final CaptureRepository captureRepository;
    private final JwtUtil jwtUtil;
    private final UserUtils userUtils;
    private Claims claims;


    // Capture Upload to S3
    @Transactional
    public ResponseEntity<Message> uploadCapture(Long fanMeetingId, String nickname, MultipartFile file, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Fan fan = userUtils.getFan(claims.getSubject());

        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FanMeeting not found with id: " + fanMeetingId));

        Idol foundidol = fanMeeting.getTeam().getIdols().stream()
                .filter(idol -> idol.getUserCommons().getNickname().equals(nickname))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idol not found with nickname: " + nickname));

        String captureUrl = awsS3Service.uploadFile(file);

        Capture capture = Capture.builder()
                .captureName(file.getOriginalFilename())
                .captureUrl(captureUrl)
                .fanMeeting(fanMeeting)
                .idol(foundidol)
                .fan(fan)
                .build();

        captureRepository.save(capture);
        return new ResponseEntity<>(new Message("캡쳐 저장(s3에) 완료!!", captureUrl), HttpStatus.OK);
    }

    // fan이 찍은 모든 capture 조회
    @Transactional
    public ResponseEntity<Message> getAllCapture(HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Fan fan = userUtils.getFan(claims.getSubject());

        List<Capture> captures = captureRepository.findByFan(fan);

        List<CaptureDto> captureDtos = captures.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Message("내가 작성한 모든 캡쳐!!", captureDtos), HttpStatus.OK);
    }

    // fan이 fanMeeting에서 찍은 모든 capture 조회
    @Transactional
    public ResponseEntity<Message> getCaptureFanMeeting(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Fan fan = userUtils.getFan(claims.getSubject());

        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FanMeeting not found with id: " + fanMeetingId));

        List<Capture> captures = captureRepository.findByFanIdAndFanMeetingId(fan.getId(), fanMeetingId);

        List<CaptureDto> captureDtos = captures.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Message("내가 " + fanMeeting.getFanMeetingName() + "에서 찍은 모든 캡쳐!!", captureDtos), HttpStatus.OK);
    }

    // fan이 fanMeeting에서 Idol과 찍은 모든 capture 조회
    @Transactional
    public ResponseEntity<Message> getCaptureIdol(Long fanMeetingId, Long idolId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Fan fan = userUtils.getFan(claims.getSubject());

        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FanMeeting not found with id: " + fanMeetingId));

//        Idol idol = idolRepository.findById(idolId)
//                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idol not found with id: " + idolId));
        // FanMeeting에 속한 Team을 가져온 후 해당 Team에서 Idol을 찾습니다.
        Team team = fanMeeting.getTeam();
        Idol foundidol = team.getIdols().stream()
                .filter(idol -> idol.getId().equals(idolId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idol not found with id: " + idolId));

        List<Capture> captures = captureRepository.findByFanIdAndFanMeetingIdAndIdolId(fan.getId(), fanMeetingId, idolId);

        List<CaptureDto> captureDtos = captures.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Message("내가 " + fanMeeting.getFanMeetingName() + "에서 " + foundidol.getUserCommons().getUsername()+ "와 찍은 모든 캡쳐!!", captureDtos), HttpStatus.OK);
    }

    // CaptureService 내부에 추가
    private CaptureDto mapToDto(Capture capture) {
        return CaptureDto.builder()
                .captureId(capture.getId())
                .captureName(capture.getCaptureName())
                .fanMeetingId(capture.getFanMeeting().getId())
                .nickName(capture.getIdol().getUserCommons().getNickname())
                .captureUrl(capture.getCaptureUrl())
                .fanId(capture.getFan().getId())
                .build();
    }
}
