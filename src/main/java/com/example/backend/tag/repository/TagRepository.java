package com.example.backend.tag.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.tag.entity.Tag;

/*
[파일 역할]
- [BACK][DB] TAGS 테이블 조회/저장을 담당하는 Repository입니다.

[관련 기능(화면/요청)]
- AlbumService.saveAlbumTags()에서 태그 조회/생성에 사용합니다.
*/
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // [BACK][DB]
    // - 어디서 호출? : AlbumService.saveAlbumTags()
    // - 입력값 : 태그명
    // - 출력값 : Optional<Tag>
    Optional<Tag> findByName(String name);

}
