package com.doldolmeet.domain.game.controller;

import com.doldolmeet.domain.game.dto.SameMindRequestDto;
import com.doldolmeet.domain.game.repository.SameMindQuizRepository;
import com.doldolmeet.domain.game.servie.GameService;
import com.doldolmeet.utils.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @GetMapping("/game/sameminds")
    public ResponseEntity<Message> getSameMinds() {
        return gameService.getSameMinds();
    }

    @PostMapping("/game/sameminds")
    public ResponseEntity<Message> postSameMinds(@RequestBody SameMindRequestDto sameMindRequestDto) {
        return gameService.createSameMindQuiz(sameMindRequestDto);
    }

}
