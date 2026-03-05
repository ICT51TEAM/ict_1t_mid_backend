package com.example.backend.qna.repository;

import com.example.backend.qna.entity.QnaPost;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QnaRepository extends JpaRepository<QnaPost, Long> {
	

	Page<QnaPost> findAllByOrderByIdDesc(Pageable pageable);
}
