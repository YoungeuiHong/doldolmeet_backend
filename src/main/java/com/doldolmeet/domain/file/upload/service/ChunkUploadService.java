package com.doldolmeet.domain.file.upload.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class ChunkUploadService {
    @Value("${file.upload.directory}")
    private String uploadDirectory;

//    @Value("${file.upload.tempdirectory} + key")
//    private String tempDirectory;

    public boolean chunkUpload(MultipartFile file, int chunkNumber, int totalChunks, String key) throws IOException {
        String tempDirectory = "src/main/resources/storage/" + key;
        // 파일 업로드 위치
        File dir = new File(tempDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        // 임시 저장 파일 이름
        String filename = originalFilename + ".part" + chunkNumber;

        Path filePath = Paths.get(tempDirectory, filename);
        // 임시 저장
        Files.write(filePath, file.getBytes());

        // 마지막 조각이 전송 됐을 경우
        if (chunkNumber == totalChunks - 1) {
//            String[] split = originalFilename.split("\\.");
//            String outputFilename = UUID.randomUUID() + "." + split[split.length - 1];
            // UUID를 파일 이름 뒤에 붙여서 다른 이름 선택
            String outputFilename = appendUUIDToFilename(originalFilename);
//            Path outputFile = Paths.get(uploadDirectory, originalFilename);
            Path outputFile = Paths.get(uploadDirectory, outputFilename);
            Files.createFile(outputFile);
            log.info("파일 {}생성 완료", outputFile);

            // 임시 파일들을 하나로 합침
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = Paths.get(tempDirectory, originalFilename + ".part" + i);
                Files.write(outputFile, Files.readAllBytes(chunkFile), StandardOpenOption.APPEND);
                log.info("파일 {}업로드 완료", outputFile);
                // 합친 후 삭제
                Files.delete(chunkFile);
                log.info("파일 {}삭제 완료", chunkFile);
            }
            deleteDirectory(Paths.get(tempDirectory));
            log.info("파일디렉토리 {}삭제 완료", tempDirectory);

            log.info("파일 업로드 성공 - 파일 이름: {}", originalFilename);
            return true;
        } else {
            return false;
        }
    }

//    private void deleteDirectory(Path directory) throws IOException {
//        try (Stream<Path> walk = Files.walk(directory)){
//            walk.map(Path::toFile).forEach(File::delete);
//        }
//        Files.delete(directory);
//    }

    private void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()) // Reverse order for files and sub-directories
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public int getLastChunkNumber(String key) {
        Path temp = Paths.get("storage", key);
        String[] list = temp.toFile().list();
        return list == null ? 0 : Math.max(list.length-2, 0);
    }

    private String appendUUIDToFilename(String originalFilename) {
        // 원본 파일 이름에 UUID를 추가하여 새로운 파일 이름 생성
        String uuid = UUID.randomUUID().toString();
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex != -1) {
            // 파일 이름에 확장자가 있는 경우
            String fileNameWithoutExtension = originalFilename.substring(0, lastDotIndex);
            String fileExtension = originalFilename.substring(lastDotIndex + 1);
            return fileNameWithoutExtension + "_" + uuid + "." + fileExtension;
        } else {
            // 파일 이름에 확장자가 없는 경우
            return originalFilename + "_" + uuid;
        }
    }
}
