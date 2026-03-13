package com.example.backend.photo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.photo.entity.PhotoEntity;

/*
[파일 역할]
- [BACK][DB] PHOTO 테이블 CRUD를 담당하는 Repository입니다.

[관련 기능(화면/요청)]
- PhotoService에서 사진 저장/조회 시 사용합니다.

[실행 흐름(순서)]
1) Service에서 photoRepository.save() 호출
2) JPA가 PHOTO 테이블 insert 실행
3) 저장된 엔티티 반환
*/
@Repository
public interface PhotoRepository extends JpaRepository<PhotoEntity, Long> {

    @Modifying
    @Transactional
    // PhotoEntity는 'album' 필드를 가지고 있으므로 여기서만 이 이름이 가능합니다.
    void deleteByAlbum_Id(Long albumId);
}
