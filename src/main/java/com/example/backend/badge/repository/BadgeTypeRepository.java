package com.example.backend.badge.repository;

import com.example.backend.badge.dto.BadgeTypeDto;
import com.example.backend.badge.entity.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * [달개 유형 레포지토리 (BadgeType Repository)]
 * - BadgeType 마스터 엔티티에 대한 DB 조회를 담당
 *
 * [구현해야 할 커스텀 쿼리 메서드]
 *
 * 1. findAllByOrderBySortOrderAsc() → List<BadgeType>
 * - 정렬 순서대로 전체 달개 유형 조회
 * - 프론트엔드 badgeService.getAllTypes() 응답에 사용
 * 
 *
 * 2. findByName(String name) → Optional<BadgeType>
 * - 이름으로 달개 유형 조회 (달개 추가 시 유형 확인용)
 */





public interface BadgeTypeRepository extends JpaRepository<BadgeType, Long> {
	// 전체 달개 유형 조회
	@Query("SELECT new com.example.backend.badge.dto.BadgeTypeDto(" +
		       "bt.id, bt.name, bt.emoji, bt.sortOrder, bt.description) " + 
		       "FROM BadgeType bt " +  
		       "ORDER BY bt.sortOrder ASC") 
	List<BadgeTypeDto> findAllByOrderBySortOrderAsc();
    
    // 특정 유형의 달개 조회
    Optional<BadgeType> findByName(String name);
    //

}
