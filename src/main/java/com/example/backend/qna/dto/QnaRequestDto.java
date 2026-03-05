package com.example.backend.qna.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaRequestDto {
    private String title;
    private String content;
}
