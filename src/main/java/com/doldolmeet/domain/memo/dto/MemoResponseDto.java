package com.doldolmeet.domain.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
public class MemoResponseDto {
    private Long id;
    private String contents;
    private String createdAt;

    public MemoResponseDto(Long id, String contents, LocalDateTime createdAt) {
        this.id = id;
        this.contents = contents;
        this.createdAt = formatDateTime(createdAt);
    }

    // LocalDateTime을 원하는 형식으로 포매팅하는 메서드 추가
    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}
