package com.example.backend.album.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.album.entity.AlbumEntity;

/*
[파일 역할]
- [BACK][DB] ALBUM 테이블 CRUD Repository입니다.

[관련 기능(화면/요청)]
- AlbumService.createAlbum()에서 앨범 저장 시 사용합니다.
- AlbumService.getAlbumFeed()에서 피드 목록(최신순) 조회 시 사용합니다.
*/
public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {

    // [DB 조회]
    // - 어디서 호출? : AlbumService.getAlbumFeed()
    // - 출력값 : 생성시각(createdAt) 내림차순 앨범 목록
    List<AlbumEntity> findAllByOrderByCreatedAtDesc();
    
    void deleteById(Long id);
}
