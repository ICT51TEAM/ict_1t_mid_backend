package com.example.backend.photo.entity;

import java.time.LocalDateTime;

import com.example.backend.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
[파일 역할]
- [BACK][DB] 업로드된 사진 메타데이터를 PHOTO 테이블에 저장하는 엔티티입니다.

[관련 기능(화면/요청)]
- 화면: frontend/src/pages/write/CreatePhotoAlbumPage.jsx
- API: POST /api/photos/upload

[실행 흐름(순서)]
1) [API] PhotoController가 파일 업로드 요청을 수신
2) [SERVICE] PhotoService가 파일 저장 후 photoUrl 생성
3) [DB] PhotoEntity를 PHOTO 테이블에 저장
4) [API] photoId/photoUrl/thumbUrl 응답에 사용
*/
@Entity
@Table(name = "PHOTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoEntity {

    // [DB] PHOTO.PHOTO_ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PHOTO_ID")
    private Long id;

    // [DB] PHOTO.USER_ID (FK -> USERS.USER_ID)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    // [DB] PHOTO.PHOTO_URL (실제 이미지 접근 경로)
    @Column(name = "PHOTO_URL", nullable = false, length = 500)
    private String photoUrl;

    // [DB] PHOTO.THUMB_URL (현재는 동일 URL 사용)
    @Column(name = "THUMB_URL", nullable = false, length = 500)
    private String thumbUrl;

    // [DB] PHOTO.CREATED_AT (업로드 시각)
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // [DB] insert 직전에 createdAt 기본값을 세팅합니다.
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
