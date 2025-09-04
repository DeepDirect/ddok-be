package goorm.ddok.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import goorm.ddok.member.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이름 + 휴대폰번호 회원 조회
    Optional<User> findByUsernameAndPhoneNumber(String username, String phoneNumber);

    // 이메일 중복 확인
    boolean existsByEmail(String email);

    // 휴대폰 번호 중복 확인
    boolean existsByPhoneNumber(String phoneNumber);

    // 로그인 계정 조회
    Optional<User> findByEmail(String email);

    // 이메일 + 이름 회원 조회
    Optional<User> findByEmailAndUsername(String email, String username);

    // 닉네임 중복 확인
    boolean existsByNickname(String nickname);

    // 닉네임 키워드로 조회
    List<User> findAllByNicknameContaining(String keyword);

    interface MapRow {
        Long getId();
        String getNickname();
        String getProfileImageUrl();
        String getPositionName();
        String getRegion1DepthName();
        String getRegion2DepthName();
        String getRegion3DepthName();
        String getRoadName();
        String getMainBuildingNo();
        String getSubBuildingNo();
        String getZoneNo();
        BigDecimal getLatitude();
        BigDecimal getLongitude();
    }

    @Query("""
        select distinct
          u.id as id,
          u.nickname as nickname,
          u.profileImageUrl as profileImageUrl,
          coalesce(pPri.positionName, pSec1.positionName) as positionName,
          l.region1DepthName as region1DepthName,
          l.region2DepthName as region2DepthName,
          l.region3DepthName as region3DepthName,
          l.roadName as roadName,
          l.mainBuildingNo as mainBuildingNo,
          l.subBuildingNo as subBuildingNo,
          l.zoneNo as zoneNo,
          l.activityLatitude as latitude,
          l.activityLongitude as longitude
        from User u
        join u.location l
        left join goorm.ddok.member.domain.UserPosition pPri
          on pPri.user = u and pPri.type = goorm.ddok.member.domain.UserPositionType.PRIMARY
        left join goorm.ddok.member.domain.UserPosition pSec1
          on pSec1.user = u
         and pSec1.type = goorm.ddok.member.domain.UserPositionType.SECONDARY
         and pSec1.ord = 1
        where u.isPublic = true
          and l.activityLatitude  is not null
          and l.activityLongitude is not null
          and l.activityLatitude  between :swLat and :neLat
          and l.activityLongitude between :swLng and :neLng
    """)
    List<MapRow> findPublicPlayersInBounds(
            @Param("swLat") BigDecimal swLat,
            @Param("neLat") BigDecimal neLat,
            @Param("swLng") BigDecimal swLng,
            @Param("neLng") BigDecimal neLng
    );

    // === 추가: "삭제되지 않은" 사용자만 대상으로 하는 안전 메서드들 ===
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
