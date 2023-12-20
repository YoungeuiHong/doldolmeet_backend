package com.doldolmeet.s3.controller;

import com.doldolmeet.s3.service.AwsS3Service;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/s3")
@ApiResponse
public class AwsS3Controller {

    private final AwsS3Service awsS3Service;

    // 팬미팅 생성 이미지 업로드
    @PostMapping("/file")
    public ResponseEntity<List<String>> uploadFile(@RequestPart List<MultipartFile> multipartFile) {
        // Upload logic
        List<String> uploadedFiles = awsS3Service.uploadMultipartFile(multipartFile);

        // Return success response
        return ResponseEntity.ok(uploadedFiles);
    }

    /**
     * Amazon S3에서 파일 다운로드
     * @param fileName 다운로드할 파일의 파일명
     * @return 성공 시 200 Success와 함께 파일 다운로드 응답 반환
     * ex) http://localhost:8080/s3/file/download?fileName=c11111.mp4
     */
    @GetMapping("/file/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String fileName) {
        // Download logic
        return awsS3Service.downloadFile(fileName);
    }

    /**
     * Amazon S3에 업로드 된 파일을 삭제
     * @return 성공 시 200 Success
     */
    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(@RequestParam String fileName) {
        // Delete logic
        awsS3Service.deleteFile(fileName);

        // Return success response with no content
        return ResponseEntity.noContent().build();
    }
}
