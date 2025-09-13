package goorm.ddok.chat.repository;

import goorm.ddok.chat.domain.ChatRoom;
import goorm.ddok.chat.domain.ChatRoomMember;
import goorm.ddok.member.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 특정 채팅방에 특정 사용자가 멤버로 존재하는지 확인
    boolean existsByRoomAndUser(ChatRoom room, User user);
    boolean existsByRoomAndUserAndDeletedAtIsNull(ChatRoom room, User user);

    // 단일 멤버십 조회
    Optional<ChatRoomMember> findByRoomAndUser(ChatRoom room, User user);

    // User 엔티티 정보가 필요한 경우를 위한 별도 쿼리
    @Query("""
        SELECT crm, u
        FROM ChatRoomMember crm
        JOIN User u ON crm.user.id = u.id
        WHERE crm.room.id = :roomId
        AND crm.deletedAt IS NULL
    """)
    List<Object[]> findAllByRoomIdWithUserInfo(@Param("roomId") Long roomId);

    // 멤버 + 유저를 한 번에 (N+1 방지)
    @EntityGraph(attributePaths = "user")
    @Query("""
      select m from ChatRoomMember m
      where m.room = :room and m.deletedAt is null
      order by m.createdAt asc
    """)
    List<ChatRoomMember> findByRoomWithUser(@Param("room") ChatRoom room);

    // 나를 제외한 멤버
    Optional<ChatRoomMember> findFirstByRoom_IdAndDeletedAtIsNullAndUser_IdNotOrderByCreatedAtAsc(
            Long roomId, Long currentUserId);

    boolean existsByRoom_IdAndUser_IdAndDeletedAtIsNull(Long roomId, Long userId);

    @Query(value = """
        select *
          from chat_room_member
         where room_id = :roomId
           and user_id = :userId
         order by id desc
         limit 1
    """, nativeQuery = true)
    Optional<ChatRoomMember> findLatestIncludingDeleted(@Param("roomId") Long roomId,
                                                        @Param("userId") Long userId);

    @Query("""
          select m from ChatRoomMember m
          where m.room.id = :roomId
            and m.deletedAt is null
            and m.user.id <> :senderId
        """)
    List<ChatRoomMember> findActiveMembersExcludingSender(@Param("roomId") Long roomId,
                                                          @Param("senderId") Long senderId);

}

