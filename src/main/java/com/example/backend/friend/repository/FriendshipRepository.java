package com.example.backend.friend.repository;

import com.example.backend.friend.entity.Friendship;
import com.example.backend.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.toUser = :user AND f.status ='PENDING'")
    List<Friendship> findPendingRequests(@Param("user") UserEntity user);

    @Query("SELECT f FROM Friendship f WHERE (f.fromUser = :user1 AND f.toUser =:user2) OR (f.fromUser = :user2 AND f.toUser = :user1)")
    Optional<Friendship> findByUsers(@Param("user1") UserEntity user1, @Param("user2") UserEntity user2);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.status = 'ACCEPTED' " +
            "AND (f.fromUser.id = :userId OR f.toUser.id = :userId)")
    long countAcceptedFriendsByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE (f.fromUser = :user OR f.toUser = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("user") UserEntity user);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.fromUser.id = :userId OR f.toUser.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.fromUser = :user AND f.status = 'PENDING'")
    List<Friendship> findSentPendingRequests(@Param("user") UserEntity user);

    @Query("SELECT CASE WHEN f.fromUser.id = :userId THEN f.toUser.id ELSE f.fromUser.id END " +
            "FROM Friendship f WHERE (f.fromUser.id = :userId OR f.toUser.id = :userId) AND f.status = 'ACCEPTED'")
    Set<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
}
