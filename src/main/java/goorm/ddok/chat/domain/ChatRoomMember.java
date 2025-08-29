package goorm.ddok.chat.domain;

import goorm.ddok.member.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
        name = "chat_room_member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_user", columnNames = {"room_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_chat_room_member_user", columnList = "user_id"),
                @Index(name = "idx_chat_room_member_room", columnList = "room_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 방 FK */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    /** 사용자 FK */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** ADMIN / MEMBER */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private ChatMemberRole role = ChatMemberRole.MEMBER;

    /** 마지막 읽은 메시지 ID (조인 비용 줄이려 Long 유지) */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /* 레거시/DTO 호환용 */
    @Transient
    public Long getRoomId() { return room != null ? room.getId() : null; }

    @Transient
    public Long getUserId() { return user != null ? user.getId() : null; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
}

