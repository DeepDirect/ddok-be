package goorm.ddok.chat.repository;

import goorm.ddok.chat.domain.ChatContentType;
import goorm.ddok.chat.domain.ChatMessage;
import goorm.ddok.chat.domain.ChatRoom;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    interface MessageView {
        Long getId();
        Long getSenderId();
        String getSenderNickname();
        ChatContentType getContentType();
        String getContentText();
        String getFileUrl();
        Instant getCreatedAt();
    }

    Optional<ChatMessage> findById(Long id);

    // createdAt 기준으로 정렬해서 반환
    List<ChatMessage> findAllByRoom_IdInAndDeletedAtIsNullOrderByCreatedAtDesc(List<Long> roomIds);

    // ContentText 조회
    List<ChatMessage> findAllByRoom_IdAndContentTextContainingAndDeletedAtIsNullOrderByCreatedAtDesc(Long roomId, String contentText);

    List<ChatMessage> findAllByRoom_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long roomId);

    @Query("""
      select new goorm.ddok.chat.repository.projection.MessageViewImpl(
        m.id, s.id, s.nickname, m.contentType, m.contentText, m.fileUrl, m.createdAt
      )
      from ChatMessage m
      join m.sender s
      where m.room = :room and m.deletedAt is null
      order by m.createdAt desc
    """)
    Page<MessageView> pageViewsByRoom(@Param("room") ChatRoom room, Pageable pageable);

    @Query("""
      select new goorm.ddok.chat.repository.projection.MessageViewImpl(
        m.id, s.id, s.nickname, m.contentType, m.contentText, m.fileUrl, m.createdAt
      )
      from ChatMessage m
      join m.sender s
      where m.room = :room and m.deletedAt is null
        and m.contentText like concat('%', :keyword, '%')
      order by m.createdAt desc
    """)
    Page<MessageView> pageViewsByRoomAndKeyword(@Param("room") ChatRoom room,
                                                @Param("keyword") String keyword,
                                                Pageable pageable);
}