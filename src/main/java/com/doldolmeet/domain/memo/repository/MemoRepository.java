package com.doldolmeet.domain.memo.repository;

import com.doldolmeet.domain.memo.entity.Memo;
import com.doldolmeet.domain.users.fan.entity.Fan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    List<Memo> findAllByOrderByCreatedAtDesc();
    List<Memo> findByFanOrderByCreatedAtAsc(Fan fan);

}
