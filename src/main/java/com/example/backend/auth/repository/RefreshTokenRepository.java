package com.example.backend.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.auth.entity.RefreshToken;
import com.example.backend.user.entity.UserEntity;


public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
	
	// 토큰값으로 RefreshToken찾기
	Optional<RefreshToken> findByToken(String token);
	// 특정 유저의 RefreshToken 존재 여부
	@Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
    Optional<RefreshToken> findByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
	

}
