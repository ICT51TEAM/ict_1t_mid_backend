package com.example.backend.auth.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "REFRESH_TOKENS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)	
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id",referencedColumnName = "USER_ID",nullable = false)
	private UserEntity user;

	@Column(nullable = false, unique = true)
	private String token;
	
	@Column(nullable = false)
	private LocalDateTime expiryDate;
	
	public RefreshToken(UserEntity user,String token, LocalDateTime expiryDate ) {
		this.user = user;
		this.token = token;
		this.expiryDate = expiryDate;
	}

	public void update(String token,LocalDateTime expiryDate) {
		this.token = token;
		this.expiryDate = expiryDate;
		
	}
}
