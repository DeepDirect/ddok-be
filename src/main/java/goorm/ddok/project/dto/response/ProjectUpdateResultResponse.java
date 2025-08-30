package goorm.ddok.project.dto.response;

import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ProjectUpdateResultResponse {

    private Long projectId;
    private boolean IsMine;

    private String title;
    private String teamStatus;
    private String bannerImageUrl;

    private List<String> traits;
    private Integer capacity;
    private Long applicantCount;

    private String mode;     // "ONLINE" | "ONLINE"
    private String address;  // ONLINE: 도로명 / ONLINE: "ONLINE"

    private PreferredAgesDto preferredAges;

    private Integer expectedMonth;
    private LocalDate startDate;

    private String detail;

    private List<PositionItem> positions;
    private LeaderBlock leader;                 // 리더 정보
    private List<ParticipantBlock> participants;// 멤버 목록

    @Getter
    @Builder
    public static class PositionItem {
        private String position;
        private Long applied;
        private Long confirmed;
        private boolean IsApplied;   // 내 지원 여부
        private boolean IsApproved;  // 내 승인 여부
        private boolean IsAvailable; // 단순 모집중 여부
    }

    @Getter
    @Builder
    public static class LeaderBlock {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String mainPosition;
        private BadgeDto mainBadge;
        private AbandonBadgeDto abandonBadge;
        private Double temperature;
        private String decidedPosition;
        private boolean IsMine;
        private Long chatRoomId;
        private boolean dmRequestPending;
    }

    @Getter
    @Builder
    public static class ParticipantBlock {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String mainPosition;
        private BadgeDto mainBadge;
        private AbandonBadgeDto abandonBadge;
        private Double temperature;
        private String decidedPosition;
        private boolean IsMine;
        private Long chatRoomId;
        private boolean dmRequestPending;
    }
}