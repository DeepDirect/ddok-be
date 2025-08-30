package goorm.ddok.study.dto.response;

import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.study.domain.StudyType;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Builder
public class StudyUpdateResultResponse {

    private Long studyId;

    private String title;
    private StudyType studyType;
    private String teamStatus;
    private String bannerImageUrl;

    private List<String> traits;
    private Integer capacity;
    private Integer applicantCount;

    private String mode;     // "online" | "offline"
    private String address;  // online이면 "ONLINE" 또는 null

    private PreferredAgesDto preferredAges;

    private Integer expectedMonth;
    private LocalDate startDate;

    private String detail;

    private LeaderBlock leader;
    private List<ParticipantBlock> participants;

    @Getter @Builder
    public static class LeaderBlock {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String mainPosition;
        private BadgeDto mainBadge;
        private AbandonBadgeDto abandonBadge;
        private Double temperature;
        private boolean IsMine;
        private Long chatRoomId;
        private boolean dmRequestPending;
    }

    @Getter @Builder
    public static class ParticipantBlock {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String mainPosition;
        private BadgeDto mainBadge;
        private AbandonBadgeDto abandonBadge;
        private Double temperature;
        private boolean IsMine;
        private Long chatRoomId;
        private boolean dmRequestPending;
    }
}