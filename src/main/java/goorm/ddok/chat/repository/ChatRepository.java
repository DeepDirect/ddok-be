package goorm.ddok.chat.repository;

import goorm.ddok.chat.domain.ChatMessage;
import goorm.ddok.chat.domain.ChatRoom;
import goorm.ddok.chat.domain.ChatRoomMember;
import goorm.ddok.chat.domain.ChatRoomType;
import goorm.ddok.member.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 채팅방에 대한 사용자의 멤버십 조회
    @Query("""
        SELECT crm FROM ChatRoomMember crm
        WHERE crm.room.id = :roomId
        AND crm.user.id = :roomId
        AND crm.deletedAt IS NULL
        """)
    Optional<ChatRoomMember> findMembershipByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 특정 채팅방의 멤버 수 조회
    @Query("""
        SELECT COUNT(crm) FROM ChatRoomMember crm
        WHERE crm.room.id = :roomId
        AND crm.deletedAt IS NULL
        """)
    Long countMembersByRoomId(@Param("roomId") Long roomId);

    // 특정 채팅방의 마지막 메시지 조회
    @Query("""
        SELECT cm FROM ChatMessage cm
        WHERE cm.room.id = :roomId
        AND cm.deletedAt IS NULL
        ORDER BY cm.createdAt DESC
        LIMIT 1
        """)
    Optional<ChatMessage> findLastMessageByRoomId(@Param("roomId") Long roomId);

    // 내가 속한 특정 타입의 방을 최근 대화순
    @Query("""
      select distinct r
      from ChatRoom r
      join r.members m
      where m.user = :user
        and r.roomType = :type
        and m.deletedAt is null
      order by coalesce(r.lastMessageAt, r.createdAt) desc, r.createdAt desc
    """)
    Page<ChatRoom> pageRoomsByMemberAndTypeOrderByRecent(@Param("user") User user,
                                                         @Param("type") ChatRoomType type,
                                                         Pageable pageable);

    // 1:1 방에서 상대 닉네임으로 검색
    @Query("""
      select distinct r
      from ChatRoom r
      join r.members mMe
      join r.members mPeer
      join mPeer.user uPeer
      where r.roomType = "PRIVATE"
        and mMe.user = :me
        and mPeer.user <> :me
        and mMe.deletedAt is null and mPeer.deletedAt is null
        and uPeer.nickname like concat('%', :search, '%')
      order by coalesce(r.lastMessageAt, r.createdAt) desc, r.createdAt desc
    """)
    Page<ChatRoom> pagePrivateRoomsByMemberAndPeerNickname(@Param("me") User me,
                                                           @Param("search") String search,
                                                           Pageable pageable);

    // 팀방: 방 이름 OR 멤버 닉네임으로 검색
    @Query("""
      select distinct r
      from ChatRoom r
      join r.members mm
      left join r.members mx
      left join mx.user ux
      where mm.user = :me
        and r.roomType = "GROUP"
        and mm.deletedAt is null
        and (
             r.name like concat('%', :search, '%')
             or (ux is not null and ux.nickname like concat('%', :search, '%'))
        )
      order by coalesce(r.lastMessageAt, r.createdAt) desc, r.createdAt desc
    """)
    Page<ChatRoom> pageGroupRoomsByMemberAndRoomOrMemberName(@Param("me") User me,
                                                             @Param("search") String search,
                                                             Pageable pageable);

    // 기존 1:1 채팅방 존재 여부 확인
    @Query("""
      select case when count(r) > 0 then true else false end
      from ChatRoom r
      join r.members m1
      join r.members m2
      where r.roomType = "PRIVATE"
        and m1.user.id = :u1 and m2.user.id = :u2
        and m1.deletedAt is null and m2.deletedAt is null
    """)
    boolean existsPrivateRoomByUserIds(@Param("u1") Long u1, @Param("u2") Long u2);
}