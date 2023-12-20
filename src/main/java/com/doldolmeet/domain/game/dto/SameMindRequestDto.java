package com.doldolmeet.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SameMindRequestDto {
    private String title;
    private String choice1;
    private String choice2;
}
