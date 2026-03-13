package com.example.backend.album.entity;

import java.time.LocalDate;
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
- [BACK][DB] ALBUM 테이블과 매핑되는 엔티티입니다.

[관련 기능(화면/요청)]
- 화면: CreatePhotoAlbumPage.jsx
- API: POST /api/albums

[실행 흐름(순서)]
1) 프론트가 앨범 정보(title/bodyText/recordDate/visibility/layoutType) 전송
2) AlbumService가 AlbumEntity 생성
3) ALBUM 테이블에 insert
*/
@Entity
@Table(name = "ALBUM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumEntity {

    // [DB] ALBUM.ALBUM_ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ALBUM_ID")
    private Long id;

    // [DB] ALBUM.USER_ID (FK -> USERS.USER_ID)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    // [DB] 앨범 제목
    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    // [DB] 앨범 본문
    @Column(name = "BODY_TEXT", nullable = false, columnDefinition = "CLOB")
    private String bodyText;

    // [DB] 기록 날짜 (LocalDate)
    @Column(name = "RECORD_DATE", nullable = false)
    private LocalDate recordDate;

    // [DB] 공개 범위 (PUBLIC/FRIENDS/PRIVATE)
    @Column(name = "VISIBILITY", nullable = false, length = 20)
    private String visibility;

    // [DB] 레이아웃 타입 값
    @Column(name = "LAYOUT_TYPE", nullable = false, length = 50)
    private String layoutType;

    // [DB] 생성 시각
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // [DB] insert 직전에 생성 시각 자동 설정
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void updateInfo(String title, String bodyText, String visibility, String recordDate, String layoutType) {
        this.title = title;
        this.bodyText = bodyText;
        this.visibility = visibility;
        this.recordDate = java.time.LocalDate.parse(recordDate); 
        this.layoutType = layoutType;
		
	}
}
