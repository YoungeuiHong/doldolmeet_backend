package com.doldolmeet.domain.users.commons;

import com.doldolmeet.domain.commons.Role;
import com.doldolmeet.domain.users.admin.entity.Admin;
import com.doldolmeet.domain.users.admin.repository.AdminRepository;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.fan.repository.FanRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.s3.service.AwsS3Service;
import com.doldolmeet.utils.Message;
import com.doldolmeet.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.doldolmeet.exception.ErrorCode.NOT_USER;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AwsS3Service awsS3Service;
    private final FanRepository fanRepository;
    private final IdolRepository idolRepository;
    private final AdminRepository adminRepository;
    private final UserUtils userUtils;
    @Transactional
    public ResponseEntity<Message> saveFanProfileImg(MultipartFile file, String username, String role) {
        String profileImgUrl;

        if (role.equals(Role.FAN.getKey())) {
            Fan fan = userUtils.getFan(username);
            profileImgUrl = awsS3Service.uploadFile(file);
            fan.getUserCommons().setProfileImgUrl(profileImgUrl);
            fanRepository.save(fan);
        } else if (role.equals(Role.IDOL.getKey())) {
            Idol idol = userUtils.getIdol(username);
            profileImgUrl = awsS3Service.uploadFile(file);
            idol.getUserCommons().setProfileImgUrl(profileImgUrl);
            idolRepository.save(idol);
        } else if (role.equals(Role.ADMIN.getKey())) {
            Admin admin = userUtils.getAdmin(username);
            profileImgUrl = awsS3Service.uploadFile(file);
            admin.getUserCommons().setProfileImgUrl(profileImgUrl);
            adminRepository.save(admin);
        } else {
            throw new CustomException(NOT_USER);
        }

        ProfileImgResponseDto profileImgResponseDto = ProfileImgResponseDto.builder()
                .fileName(file.getOriginalFilename())
                .fileUrl(profileImgUrl)
                .build();

        return new ResponseEntity<>(new Message("s3에 프로필 저장 완료!!", profileImgResponseDto), HttpStatus.OK);
    }
}
