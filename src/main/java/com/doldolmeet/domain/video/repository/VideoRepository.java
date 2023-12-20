package com.doldolmeet.domain.video.repository;

import com.doldolmeet.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
