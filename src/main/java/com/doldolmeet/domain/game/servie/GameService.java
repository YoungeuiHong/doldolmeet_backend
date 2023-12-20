package com.doldolmeet.domain.game.servie;

import com.doldolmeet.domain.game.dto.SameMindRequestDto;
import com.doldolmeet.domain.game.entity.SameMindQuiz;
import com.doldolmeet.domain.game.repository.SameMindQuizRepository;
import com.doldolmeet.utils.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameService {
    private final SameMindQuizRepository sameMindQuizRepository;
    @Transactional
    public ResponseEntity<Message> getSameMinds() {
        return new ResponseEntity<>(new Message("이심전심 퀴즈 다 받기 성공", sameMindQuizRepository.findAll()), HttpStatus.OK);
    }


    @Transactional
    public ResponseEntity<Message> createSameMindQuiz(SameMindRequestDto sameMindRequestDto) {
        SameMindQuiz sameMindQuiz = SameMindQuiz.builder()
                .title(sameMindRequestDto.getTitle())
                .choice1(sameMindRequestDto.getChoice1())
                .choice2(sameMindRequestDto.getChoice2())
                .build();

        sameMindQuizRepository.save(sameMindQuiz);
        return new ResponseEntity<>(new Message("이심전심 퀴즈 생성 성공", sameMindQuiz), HttpStatus.OK);
    }
}
