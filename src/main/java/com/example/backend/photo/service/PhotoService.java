package com.example.backend.photo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.photo.dto.PhotoDto;
import com.example.backend.photo.entity.PhotoEntity;
import com.example.backend.photo.repository.PhotoRepository;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
[파일 역할]
- [BACK][SERVICE] 파일 저장 + PHOTO 테이블 저장 + 응답 DTO 변환을 담당합니다.

[관련 기능(화면/요청)]
- 화면: CreatePhotoAlbumPage.jsx
- API: POST /api/photos/upload

[실행 흐름(순서)]
1) 요청 검증(files, userId)
2) uploads/{yyyy}/{MM} 디렉토리 생성
3) UUID_originalFilename 규칙으로 파일 저장
4) PHOTO 테이블 insert
5) photoId/photoUrl/thumbUrl 목록 반환
*/
@Service
@RequiredArgsConstructor
public class PhotoService {

    // [BACK] 파일 최대 크기 10MB
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    // [BACK] 허용 확장자 화이트리스트
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    // [BACK][CONFIG] application.yml 의 app.upload.dir 값을 주입
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // [BACK][API]
    // - 어디서 호출? : PhotoController.uploadPhotos()
    // - 입력값 : files(MultipartFile 목록), userId(Long)
    // - 출력값 : List<PhotoDto>
    @Transactional
    public List<PhotoDto> uploadPhotos(List<MultipartFile> files, Long userId) {
        validateUploadRequest(files, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Path monthlyDir = resolveMonthlyUploadDir();
        String year = monthlyDir.getParent().getFileName().toString();
        String month = monthlyDir.getFileName().toString();

        List<PhotoDto> result = new ArrayList<>();

        for (MultipartFile file : files) {
            validateSingleFile(file);
            String savedFileName = buildSavedFileName(file.getOriginalFilename());

            Path target = monthlyDir.resolve(savedFileName);
            saveFile(file, target);

            String photoUrl = "/uploads/" + year + "/" + month + "/" + savedFileName;

            PhotoEntity saved = photoRepository.save(
                    PhotoEntity.builder()
                            .user(user)
                            .photoUrl(photoUrl)
                            .thumbUrl(photoUrl)
                            .build());

            result.add(PhotoDto.builder()
                    .photoId(saved.getId())
                    .photoUrl(saved.getPhotoUrl())
                    .thumbUrl(saved.getThumbUrl())
                    .build());
        }

        return result;
    }

    // [BACK][VALIDATION]
    // - 어디서 호출? : uploadPhotos() 초반
    // - 입력값 : files, userId
    // - 출력값 : 없음(실패 시 IllegalArgumentException)
    private void validateUploadRequest(List<MultipartFile> files, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상의 파일이 필요합니다.");
        }
    }

    // [BACK][VALIDATION]
    // - 어디서 호출? : 업로드 루프 내부
    // - 입력값 : file
    // - 출력값 : 없음(실패 시 IllegalArgumentException)
    private void validateSingleFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("파일 최대 크기(10MB)를 초과했습니다.");
        }

        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 확장자입니다. (jpg, jpeg, png, webp만 가능)");
        }
    }

    // [BACK][FILE]
    // - 어디서 호출? : uploadPhotos()
    // - 입력값 : 없음
    // - 출력값 : 현재 월 업로드 디렉토리 Path
    private Path resolveMonthlyUploadDir() {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path monthlyDir = root.resolve(year).resolve(month);

        try {
            Files.createDirectories(monthlyDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 폴더를 생성할 수 없습니다.", e);
        }

        return monthlyDir;
    }

    // [BACK][FILE]
    // - 어디서 호출? : 업로드 루프 내부
    // - 입력값 : originalFileName
    // - 출력값 : UUID_originalFilename 형식 파일명
    private String buildSavedFileName(String originalFileName) {
        String cleanedName = StringUtils.cleanPath(originalFileName == null ? "file" : originalFileName)
                .replace(" ", "_");
        return UUID.randomUUID() + "_" + cleanedName;
    }

    // [BACK][FILE]
    // - 어디서 호출? : uploadPhotos()
    // - 입력값 : MultipartFile, target Path
    // - 출력값 : 없음(파일 저장)
    private void saveFile(MultipartFile file, Path target) {
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }
    }

    // [BACK][UTIL]
    // - 어디서 호출? : validateSingleFile()
    // - 입력값 : 원본 파일명
    // - 출력값 : 소문자 확장자
    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
