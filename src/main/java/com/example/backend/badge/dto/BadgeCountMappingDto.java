package com.example.backend.badge.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BadgeCountMappingDto {
	private Long typeId;
	private Long count;
	
	public BadgeCountMappingDto(Long typeId, Long count) {
		this.typeId = typeId;
		this.count = count;
	}

}
