package goorm.ddok.chat.domain;

import goorm.ddok.member.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
        name = "chat_room",
        indexes = {
                @Index(name = "idx_chat_room_type", columnList = "room_type"),
                @Index(name = "idx_chat_room_last_msg_at", columnList = "last_message_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PRIVATE / GROUP */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private ChatRoomType roomType;

    /** 그룹명 (PRIVATE에서는 보통 null) */
    @Column(name = "name", length = 100)
    private String name;

    /** GROUP 전용 소유자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

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

    /* 양방향 편의 연관 (필요 시) */
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /* 레거시/DTO 호환용 */
    @Transient
    public Long getOwnerUserId() {
        return owner != null ? owner.getId() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
}
