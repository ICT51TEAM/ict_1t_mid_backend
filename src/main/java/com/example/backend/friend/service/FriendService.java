package com.example.backend.friend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.friend.dto.FriendResponseDto;
import com.example.backend.friend.dto.UserSearchDto;
import com.example.backend.friend.entity.Friendship;
import com.example.backend.friend.repository.FriendshipRepository;
import com.example.backend.notification.service.NotificationService;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

        private final FriendshipRepository friendshipRepository;
        private final UserRepository userRepository;
        private final NotificationService notificationService;

        public List<FriendResponseDto> getAcceptedFriendsList(Long myId) {
                UserEntity currentUser = userRepository.findById(myId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(currentUser);
                return friendships.stream()
                                .map(friendship -> {
                                        UserEntity friendUser = friendship
                                                        .getFromUser()
                                                        .getId()
                                                        .equals(currentUser.getId())
                                                                        ? friendship.getToUser()
                                                                        : friendship.getFromUser();
                                        return FriendResponseDto.builder()
                                                        .friendshipId(friendship.getId())
                                                        .userId(friendUser.getId())
                                                        .username(friendUser.getUsername())
                                                        .profileImageUrl(friendUser.getProfileImageUrl())
                                                        .status(friendship.getStatus())
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        public List<FriendResponseDto> listPendingRequests(Long userId) {
                // 여기에 받은 친구 요청 목록 조회 로직을 작성하세요.
                UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                return friendshipRepository.findPendingRequests(user).stream()
                                .map(friendship -> {
                                        UserEntity requester = friendship.getFromUser();
                                        return FriendResponseDto.builder()
                                                        .friendshipId(friendship.getId())
                                                        .userId(requester.getId())
                                                        .username(requester.getUsername())
                                                        .profileImageUrl(requester.getProfileImageUrl())
                                                        .status(friendship.getStatus())
                                                        .build();
                                })
                                .collect(Collectors.toList());

        }

        @Transactional
        public void sendRequest(Long fromUserId, Long targetUserId) {
                // 여기에 친구 요청 로직을 작성하세요.
                if (fromUserId.equals(targetUserId)) {
                        throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
                }

                UserEntity fromUser = userRepository.findById(fromUserId)
                                .orElseThrow(() -> new IllegalArgumentException("요청자를 찾을 수 없습니다."));
                UserEntity targetUser = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

                // 기존 관계 확인 (중복 요청 방지)
                Optional<Friendship> existing = friendshipRepository.findByUsers(fromUser, targetUser);
                if (existing.isPresent()) {
                        String status = existing.get().getStatus();
                        if ("REJECTED".equals(status)) {
                                // 거절된 요청은 삭제 후 재신청 허용
                                friendshipRepository.delete(existing.get());
                                friendshipRepository.flush();
                        } else {
                                throw new IllegalArgumentException("이미 친구이거나 요청이 진행 중입니다.");
                        }
                }

                Friendship friendship = Friendship.builder()
                                .fromUser(fromUser)
                                .toUser(targetUser)
                                // status("PENDING")와 createdAt은 엔티티의 @PrePersist(onCreate)에 의해 자동 설정됨
                                .build();
                friendshipRepository.save(friendship);

                // 상대방에게 글벗 요청 알림 생성
                notificationService.createNotification(
                                targetUserId,
                                "FRIEND_REQUEST",
                                "새로운 글벗 요청",
                                fromUser.getUsername() + "님이 글벗 요청을 보냈습니다.");

        }

        @Transactional
        public void acceptRequest(Long friendshipId, Long userId) {
                // 여기에 친구 요청 수락 로직을 작성하세요.
                Friendship friendship = friendshipRepository.findById(friendshipId)
                                .orElseThrow(() -> new IllegalArgumentException("해당 친구 요청을 찾을 수 없습니다."));
                
                if (!friendship.getToUser().getId().equals(userId)) {
                    throw new IllegalArgumentException("요청을 받은 사용자만 수락할 수 있습니다.");
                }

                if (!"PENDING".equals(friendship.getStatus())) {
                        throw new IllegalArgumentException("대기 중인 요청만 수락 가능합니다.");
                }

                friendship.setStatus("ACCEPTED");
                // @PreUpdate 어노테이션에 의해 updatedAt 자동 갱신

                // 요청자에게 글벗 수락 알림 생성
                notificationService.createNotification(
                                friendship.getFromUser().getId(),
                                "FRIEND_ACCEPT",
                                "글벗 요청 수락",
                                friendship.getToUser().getUsername() + "님이 글벗 요청을 수락했습니다.");

        }

        @Transactional
        public void rejectRequest(Long friendshipId, Long userId) {
                // 여기에 친구 요청 거절 로직을 작성하세요.
                Friendship friendship = friendshipRepository.findById(friendshipId)
                                .orElseThrow(() -> new IllegalArgumentException("해당 친구 요청을 찾을 수 없습니다."));

                if (!friendship.getToUser().getId().equals(userId)) {
                        throw new IllegalArgumentException("요청을 받은 사용자만 거절할 수 있습니다.");
                }

                if (!"PENDING".equals(friendship.getStatus())) {
                        throw new IllegalArgumentException("대기 중인 요청만 거절 가능합니다.");
                }

                friendship.setStatus("REJECTED");

        }

        @Transactional
        public void removeFriend(Long friendId, Long userId) {
                UserEntity currentUser = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                Friendship friendship = friendshipRepository.findById(friendId)
                                .orElseThrow(() -> new IllegalArgumentException("친구 관계를 찾을 수 없습니다."));

                if (!friendship.getFromUser().getId().equals(currentUser.getId()) &&
                                !friendship.getToUser().getId().equals(currentUser.getId())) {
                        throw new IllegalArgumentException("친구 관계를 삭제할 권한이 없습니다.");
                }
                friendshipRepository.delete(friendship);
        }

        public List<UserSearchDto> searchUsers(String query, Long userId) {
                UserEntity currentUser = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                List<UserEntity> userList = userRepository.searchByUsernameIgnoreCaseExcludePrivate(query);
                return userList.stream()
                                .filter(user -> !user.getId().equals(currentUser.getId()))
                                .map(user -> {
                                        boolean isFriend = false;
                                        boolean isPending = false;
                                        Optional<Friendship> friendshipOpt = friendshipRepository
                                                        .findByUsers(currentUser, user);
                                        if (friendshipOpt.isPresent()) {
                                                String status = friendshipOpt.get().getStatus();
                                                if ("ACCEPTED".equals(status)) {
                                                        isFriend = true;
                                                } else if ("PENDING".equals(status)) {
                                                        isPending = true;
                                                }
                                        }

                                        return UserSearchDto.builder()
                                                        .userId(user.getId())
                                                        .username(user.getUsername())
                                                        .profileImageUrl(user.getProfileImageUrl())
                                                        .isFriend(isFriend)
                                                        .isPending(isPending)
                                                        .badgeCount(0)
                                                        .representativeBadge(null)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

		public List<FriendResponseDto> listSentPendingRequests(Long userId) {
			// 사용자 존재 확인
			UserEntity user = userRepository.findById(userId)
					.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
			
			// 내가 보낸 친구 요청중 pending 상태인 목록 조회
			return friendshipRepository.findSentPendingRequests(user).stream()
					.map(friendship -> {
						// 내가 보낸 것에 대한 상대방 정보 추출
						UserEntity targetUser = friendship.getToUser();
						
						return FriendResponseDto.builder()
								.friendshipId(friendship.getId())
								.userId(targetUser.getId())
								.username(targetUser.getUsername())
								.profileImageUrl(targetUser.getProfileImageUrl())
								.status(friendship.getStatus())
								.build();
					})
					.collect(Collectors.toList());
		}
}