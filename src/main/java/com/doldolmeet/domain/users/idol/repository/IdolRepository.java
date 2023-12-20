package com.doldolmeet.domain.users.idol.repository;

import com.doldolmeet.domain.users.idol.entity.Idol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdolRepository extends JpaRepository<Idol, Long> {
    Optional<Idol> findByUserCommonsUsername(String username);
    Optional<Idol> findByUserCommonsNickname(String nickname);
}
