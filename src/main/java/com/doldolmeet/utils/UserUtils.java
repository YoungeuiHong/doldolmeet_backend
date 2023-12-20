package com.doldolmeet.utils;
import com.doldolmeet.domain.teleRoom.entity.TeleRoom;
import com.doldolmeet.domain.teleRoom.repository.TeleRoomRepository;
import com.doldolmeet.domain.users.admin.entity.Admin;
import com.doldolmeet.domain.users.admin.repository.AdminRepository;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.fan.repository.FanRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import com.doldolmeet.domain.waitRoom.repository.WaitRoomRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanToFanMeetingRepository;
import com.doldolmeet.exception.CustomException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.doldolmeet.exception.ErrorCode.*;

@Component
@RequiredArgsConstructor
public class UserUtils {
    private final AdminRepository adminRepository;
    private final FanRepository fanRepository;
    private final IdolRepository idolRepository;
    private final WaitRoomRepository waitRoomRepository;
    private final TeleRoomRepository teleRoomRepository;

    public void checkIfUserExist(Claims claims) {
        String username = claims.getSubject();

        Optional<Fan> fan = fanRepository.findByUserCommonsUsername(username);
        Optional<Admin> admin = adminRepository.findByUserCommonsUsername(username);
        Optional<Idol> idol = idolRepository.findByUserCommonsUsername(username);

        if (!fan.isPresent() && !admin.isPresent() && !idol.isPresent()) {
            throw new CustomException(NOT_USER);
        }
    }


    public void checkIfAdmin(Claims claims) {
        String role = (String)claims.get("auth");
        Optional<Admin> admin = adminRepository.findByUserCommonsUsername(claims.getSubject());

        if (!admin.isPresent()) {
            throw new CustomException(USER_NOT_FOUND);
        }
    }

    public Fan getFan(String username) {
        Optional<Fan> fan = fanRepository.findByUserCommonsUsername(username);

        if (!fan.isPresent()) {
            throw new CustomException(NOT_USER);
        }

        return fan.get();
    }

    public Idol getIdol(String username) {
        Optional<Idol> idol = idolRepository.findByUserCommonsUsername(username);

        if (!idol.isPresent()) {
            throw new CustomException(NOT_USER);
        }

        return idol.get();

    }

    public Admin getAdmin(String username) {
        Optional<Admin> admin = adminRepository.findByUserCommonsUsername(username);

        if (!admin.isPresent()) {
            throw new CustomException(NOT_USER);
        }

        return admin.get();
    }


    public WaitRoom getWaitRoom(String waitRoomId) {
        Optional<WaitRoom> waitRoom = waitRoomRepository.findByRoomId(waitRoomId);

        if (!waitRoom.isPresent()) {
            throw new CustomException(NOT_USER);
        }

        return waitRoom.get();
    }

    public TeleRoom getTeleRoom(String teleRoomId) {
        Optional<TeleRoom> teleRoom = teleRoomRepository.findByRoomId(teleRoomId);

        if (!teleRoom.isPresent()) {
            throw new CustomException(NOT_USER);
        }

        return teleRoom.get();
    }



}
