package com.doldolmeet.domain.video.service;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.video.dto.VideoDto;
import com.doldolmeet.domain.video.entity.Video;
import com.doldolmeet.domain.video.repository.VideoRepository;
import com.doldolmeet.s3.service.AwsS3Service;
import com.doldolmeet.utils.Message;
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
public class VideoService {
    private final AwsS3Service awsS3Service;
    private final FanMeetingRepository fanMeetingRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public ResponseEntity<Message> uploadVideo(Long fanMeetingId, MultipartFile file) {
        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FanMeeting not found with id: " + fanMeetingId));

        String videoUrl = awsS3Service.uploadFile(file);

        Video video = Video.builder()
                .videoName(file.getOriginalFilename())
                .fanMeeting(fanMeeting)
                .videoUrl(videoUrl)
                .build();

        videoRepository.save(video);
        return new ResponseEntity<>(new Message("비디오 저장(s3에) 완료", videoUrl), HttpStatus.OK);
//        return videoUrl;
    }

    public List<VideoDto> getAllVideos() {
        List<Video> videos = videoRepository.findAll();
        return videos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private VideoDto convertToDto(Video video) {
        return VideoDto.builder()
                .videoId(video.getId())
                .videoName(video.getVideoName())
                .videoUrl(video.getVideoUrl())
                .fanMeetingId(video.getFanMeeting().getId())
                .build();
    }
}