package com.doldolmeet.domain.users.commons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImgResponseDto {
    private String fileName;
    private String fileUrl;
}
