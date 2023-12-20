package com.doldolmeet.domain.memo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MemoRequestDto {
    private String contents;
}
