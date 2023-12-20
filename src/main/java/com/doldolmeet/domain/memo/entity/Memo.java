package com.doldolmeet.domain.memo.entity;

import com.doldolmeet.domain.memo.dto.MemoRequestDto;
import com.doldolmeet.domain.users.fan.entity.Fan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "memos")
public class Memo extends Timestamped{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fan_id", referencedColumnName = "id")
    private Fan fan;

    public Memo(MemoRequestDto requestDto, Fan fan) {
        this.contents = requestDto.getContents();
        this.fan = fan;

    }
    public void update(MemoRequestDto requestDto) {
        this.contents = requestDto.getContents();
    }
}
