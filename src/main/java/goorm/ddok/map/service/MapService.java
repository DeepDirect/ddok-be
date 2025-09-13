package goorm.ddok.map.service;

import goorm.ddok.badge.service.BadgeService;
import goorm.ddok.cafe.repository.CafeRepository;
import goorm.ddok.global.dto.*;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.map.dto.response.*;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.repository.UserRepository;
import goorm.ddok.project.repository.ProjectRecruitmentRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final BadgeService badgeService;

    public List<AllMapItemResponse> getAllInBounds(
            BigDecimal swLat, BigDecimal swLng,
            BigDecimal neLat, BigDecimal neLng,
            BigDecimal centerLat, BigDecimal centerLng,
            Long userId
    ){
        validateBounds(swLat, swLng, neLat, neLng);

        List<AllMapItemResponse> result = new ArrayList<>();

        var projectRows = projectRecruitmentRepository.findAllInBounds(swLat, neLat, swLng, neLng);
        if (projectRows != null) {
            List<AllMapItemResponse> finalResult = result;
            projectRows.forEach(r -> finalResult.add(AllMapItemResponse.builder()
                    .category("project")
                    .projectId(r.getId())
                    .title(r.getTitle())
                    .teamStatus(normalizeProjectTeamStatus(r.getTeamStatus()))
                    .location(LocationDto.builder()
                            .address(composeRoadAddress(
                                    r.getRegion1depthName(),
                                    r.getRegion2depthName(),
                                    r.getRegion3depthName(),
                                    r.getRoadName(),
                                    r.getMainBuildingNo(),
                                    r.getSubBuildingNo()))
                            .region1depthName(r.getRegion1depthName())
                            .region2depthName(r.getRegion2depthName())
                            .region3depthName(r.getRegion3depthName())
                            .roadName(r.getRoadName())
                            .mainBuildingNo(r.getMainBuildingNo())
                            .subBuildingNo(r.getSubBuildingNo())
                            .zoneNo(r.getZoneNo())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build())
                    .build()));
        }

        var studyRows = studyRecruitmentRepository.findAllInBounds(swLat, neLat, swLng, neLng);
        if (studyRows != null) {
            List<AllMapItemResponse> finalResult1 = result;
            studyRows.forEach(r -> finalResult1.add(AllMapItemResponse.builder()
                    .category("study")
                    .studyId(r.getId())
                    .title(r.getTitle())
                    .teamStatus(normalizeStudyTeamStatus(r.getTeamStatus()))
                    .location(LocationDto.builder()
                            .address(composeRoadAddress(
                                    r.getRegion1depthName(),
                                    r.getRegion2depthName(),
                                    r.getRegion3depthName(),
                                    r.getRoadName(),
                                    r.getMainBuildingNo(),
                                    r.getSubBuildingNo()))
                            .region1depthName(r.getRegion1depthName())
                            .region2depthName(r.getRegion2depthName())
                            .region3depthName(r.getRegion3depthName())
                            .roadName(r.getRoadName())
                            .mainBuildingNo(r.getMainBuildingNo())
                            .subBuildingNo(r.getSubBuildingNo())
                            .zoneNo(r.getZoneNo())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build())
                    .build()));
        }

        var playerRows = userRepository.findPublicPlayersInBounds(swLat, neLat, swLng, neLng);
        if (playerRows != null) {
            List<AllMapItemResponse> finalResult2 = result;
            playerRows.forEach(r -> finalResult2.add(AllMapItemResponse.builder()
                    .category("player")
                    .userId(r.getId())
                    .nickname(r.getNickname())
                    .position(r.getPositionName())
                    .IsMine(userId != null && userId.equals(r.getId()))
                    .location(LocationDto.builder()
                            .address(composeRoadAddress(
                                    r.getRegion1DepthName(),
                                    r.getRegion2DepthName(),
                                    r.getRegion3DepthName(),
                                    r.getRoadName(),
                                    r.getMainBuildingNo(),
                                    r.getSubBuildingNo()))
                            .region1depthName(r.getRegion1DepthName())
                            .region2depthName(r.getRegion2DepthName())
                            .region3depthName(r.getRegion3DepthName())
                            .roadName(r.getRoadName())
                            .mainBuildingNo(r.getMainBuildingNo())
                            .subBuildingNo(r.getSubBuildingNo())
                            .zoneNo(r.getZoneNo())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build())
                    .build()));
        }

        var cafeRows = cafeRepository.findAllInBounds(swLat, neLat, swLng, neLng);
        if (cafeRows != null) {
            List<AllMapItemResponse> finalResult3 = result;
            cafeRows.forEach(r -> finalResult3.add(AllMapItemResponse.builder()
                    .category("cafe")
                    .cafeId(r.getId())
                    .title(r.getTitle())
                    .location(LocationDto.builder()
                            .address(composeRoadAddress(
                                    r.getRegion1depthName(),
                                    r.getRegion2depthName(),
                                    r.getRegion3depthName(),
                                    r.getRoadName(),
                                    r.getMainBuildingNo(),
                                    r.getSubBuildingNo()))
                            .region1depthName(r.getRegion1depthName())
                            .region2depthName(r.getRegion2depthName())
                            .region3depthName(r.getRegion3depthName())
                            .roadName(r.getRoadName())
                            .mainBuildingNo(r.getMainBuildingNo())
                            .subBuildingNo(r.getSubBuildingNo())
                            .zoneNo(r.getZoneNo())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build())
                    .build()));
        }

        if (centerLat != null && centerLng != null && !result.isEmpty()) {
            final double cLat = centerLat.doubleValue();
            final double cLng = centerLng.doubleValue();
            result = result.stream()
                    .sorted(Comparator.comparingDouble(item -> {
                        var loc = item.getLocation();
                        return haversineKm(
                                cLat, cLng,
                                loc.getLatitude().doubleValue(),
                                loc.getLongitude().doubleValue()
                        );
                    }))
                    .toList();
        }

        if (result.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return result;
    }


    @Transactional(readOnly = true)
    public List<ProjectMapItemResponse> getProjectsInBounds(
            BigDecimal swLat, BigDecimal swLng,
            BigDecimal neLat, BigDecimal neLng,
            BigDecimal centerLat, BigDecimal centerLng
    ) {
        validateBounds(swLat, swLng, neLat, neLng);

        var rows = projectRecruitmentRepository.findAllInBounds(swLat, neLat, swLng, neLng);

        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        var items = rows.stream()
                .map(r -> ProjectMapItemResponse.builder()
                        .category("project")
                        .projectId(r.getId())
                        .title(r.getTitle())
                        .teamStatus(normalizeProjectTeamStatus(r.getTeamStatus()))
                        .location(LocationDto.builder()
                                .address(composeRoadAddress(
                                        r.getRegion1depthName(),
                                        r.getRegion2depthName(),
                                        r.getRegion3depthName(),
                                        r.getRoadName(),
                                        r.getMainBuildingNo(),
                                        r.getSubBuildingNo()))
                                .region1depthName(r.getRegion1depthName())
                                .region2depthName(r.getRegion2depthName())
                                .region3depthName(r.getRegion3depthName())
                                .roadName(r.getRoadName())
                                .mainBuildingNo(r.getMainBuildingNo())
                                .subBuildingNo(r.getSubBuildingNo())
                                .zoneNo(r.getZoneNo())
                                .latitude(r.getLatitude())
                                .longitude(r.getLongitude())
                                .build())
                        .build())
                .toList();

        // 중심점이 있으면 거리순 정렬
        if (centerLat != null && centerLng != null && !items.isEmpty()) {
            final double cLat = centerLat.doubleValue();
            final double cLng = centerLng.doubleValue();

            items = items.stream()
                    .sorted(Comparator.comparingDouble(p ->
                            haversineKm(
                                    cLat, cLng,
                                    p.getLocation().getLatitude().doubleValue(),
                                    p.getLocation().getLongitude().doubleValue()
                            )))
                    .toList();
        }

        return items;
    }

    @Transactional(readOnly = true)
    public List<StudyMapItemResponse> getStudiesInBounds(
            BigDecimal swLat, BigDecimal swLng,
            BigDecimal neLat, BigDecimal neLng,
            BigDecimal centerLat, BigDecimal centerLng
    ) {
        validateBounds(swLat, swLng, neLat, neLng);

        var rows = studyRecruitmentRepository.findAllInBounds(swLat, neLat, swLng, neLng);

        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        var items = rows.stream()
                .map(r -> StudyMapItemResponse.builder()
                        .category("study")
                        .studyId(r.getId())
                        .title(r.getTitle())
                        .teamStatus(normalizeStudyTeamStatus(r.getTeamStatus()))
                        .location(LocationDto.builder()
                                .address(composeRoadAddress(
                                        r.getRegion1depthName(),
                                        r.getRegion2depthName(),
                                        r.getRegion3depthName(),
                                        r.getRoadName(),
                                        r.getMainBuildingNo(),
                                        r.getSubBuildingNo()))
                                .region1depthName(r.getRegion1depthName())
                                .region2depthName(r.getRegion2depthName())
                                .region3depthName(r.getRegion3depthName())
                                .roadName(r.getRoadName())
                                .mainBuildingNo(r.getMainBuildingNo())
                                .subBuildingNo(r.getSubBuildingNo())
                                .zoneNo(r.getZoneNo())
                                .latitude(r.getLatitude())
                                .longitude(r.getLongitude())
                                .build())
                        .build())
                .toList();

        // 중심점이 있으면 거리순 정렬
        if (centerLat != null && centerLng != null && !items.isEmpty()) {
            final double cLat = centerLat.doubleValue();
            final double cLng = centerLng.doubleValue();

            items = items.stream()
                    .sorted(Comparator.comparingDouble(p ->
                            haversineKm(
                                    cLat, cLng,
                                    p.getLocation().getLatitude().doubleValue(),
                                    p.getLocation().getLongitude().doubleValue()
                            )))
                    .toList();
        }

        return items;
    }

    public void validateBounds(BigDecimal swLat, BigDecimal swLng, BigDecimal neLat, BigDecimal neLng) {
        boolean hasAny = swLat != null || swLng != null || neLat != null || neLng != null;
        boolean hasAll = swLat != null && swLng != null && neLat != null && neLng != null;
        if (hasAny && !hasAll) {
            throw new GlobalException(ErrorCode.REQUIRED_PARAMETER_MISSING);
        }
        if (hasAll && (swLat.compareTo(neLat) > 0 || swLng.compareTo(neLng) > 0)) {
            throw new GlobalException(ErrorCode.INVALID_MAP_BOUNDS);
        }
    }

    public String composeRoadAddress(String r1, String r2, String r3,
                                     String road, String mainNo, String subNo) {
        if (road == null && r1 == null && r2 == null) return "-";
        var sb = new StringBuilder();
        if (r1 != null && !r1.isBlank()) sb.append(r1).append(" ");
        if (r2 != null && !r2.isBlank()) sb.append(r2).append(" ");
        if (r3 != null && !r3.isBlank()) sb.append(r3).append(" ");
        if (road != null && !road.isBlank()) sb.append(road).append(" ");

        String main = mainNo == null ? "" : mainNo.trim();
        String sub  = subNo == null ? "" : subNo.trim();
        if (!main.isEmpty() && !sub.isEmpty()) sb.append(main).append("-").append(sub);
        else if (!main.isEmpty()) sb.append(main);

        return sb.toString().trim();
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }

    private String normalizeProjectTeamStatus(goorm.ddok.project.domain.TeamStatus status) {
        if (status == null) return "ONGOING";
        return (status == goorm.ddok.project.domain.TeamStatus.CLOSED) ? "ONGOING" : status.name();
    }

    private String normalizeStudyTeamStatus(goorm.ddok.study.domain.TeamStatus status) {
        if (status == null) return "ONGOING";
        return (status == goorm.ddok.study.domain.TeamStatus.CLOSED) ? "ONGOING" : status.name();
    }

    @Transactional(readOnly = true)
    public List<PlayerMapItemResponse> getPlayersInBounds(
            BigDecimal swLat, BigDecimal swLng,
            BigDecimal neLat, BigDecimal neLng,
            BigDecimal centerLat, BigDecimal centerLng,
            Long userId
    ) {
        validateBounds(swLat, swLng, neLat, neLng);

        var rows = userRepository.findPublicPlayersInBounds(swLat, neLat, swLng, neLng);
        if (rows == null || rows.isEmpty()) return java.util.Collections.emptyList();

        var items = rows.stream()
                .map(r -> PlayerMapItemResponse.builder()
                        .category("player")
                        .userId(r.getId())
                        .nickname(r.getNickname())
                        .position(r.getPositionName()) // PRIMARY -> SECONDARY(1) fallback
                        .IsMine(userId != null && userId.equals(r.getId()))
                        .location(LocationDto.builder()
                                .address(composeRoadAddress(
                                        r.getRegion1DepthName(),
                                        r.getRegion2DepthName(),
                                        r.getRegion3DepthName(),
                                        r.getRoadName(),
                                        r.getMainBuildingNo(),
                                        r.getSubBuildingNo()))
                                .region1depthName(r.getRegion1DepthName())
                                .region2depthName(r.getRegion2DepthName())
                                .region3depthName(r.getRegion3DepthName())
                                .roadName(r.getRoadName())
                                .mainBuildingNo(r.getMainBuildingNo())
                                .subBuildingNo(r.getSubBuildingNo())
                                .zoneNo(r.getZoneNo())
                                .latitude(r.getLatitude())
                                .longitude(r.getLongitude())
                                .build())
                        .build())
                .toList();

        if (centerLat != null && centerLng != null && !items.isEmpty()) {
            final double cLat = centerLat.doubleValue();
            final double cLng = centerLng.doubleValue();
            items = items.stream()
                    .sorted(Comparator.comparingDouble(p ->
                            haversineKm(
                                    cLat, cLng,
                                    p.getLocation().getLatitude().doubleValue(),
                                    p.getLocation().getLongitude().doubleValue()
                            )))
                    .toList();
        }

        return items;
    }

    @Transactional(readOnly = true)
    public PageResponse<AllMapItemSearchResponse> search(
            String keyword,
            BigDecimal swLat, BigDecimal swLng, BigDecimal neLat, BigDecimal neLng,
            BigDecimal centerLat, BigDecimal centerLng,
            String categoryCsv,
            Long userId,
            int page, int size,
            String filterCsv
    ) {
        // 1) 검증
        validateBounds(swLat, swLng, neLat, neLng);

        // 2) 파싱
        Set<String> categories = parseCategories(categoryCsv);
        // 기본: project,study,player
        Set<goorm.ddok.project.domain.TeamStatus> projectStatusFilter = parseProjectTeamStatusFilter(filterCsv);
        Set<goorm.ddok.study.domain.TeamStatus> studyStatusFilter = parseStudyTeamStatusFilter(filterCsv);

        // 3) 페이지 파라미터+페치상한
        page = Math.max(0, page);
        size = (size <= 0) ? 20 : size;

        final List<AllMapItemSearchResponse> items = new ArrayList<>();

        if (categories.contains("project")) {
            var rows = projectRecruitmentRepository.findAllInBounds(swLat, neLat, swLng, neLng);

            if (rows != null) {
                rows.stream()
                        .filter(r -> projectStatusFilter.isEmpty() || projectStatusFilter.contains(r.getTeamStatus()))
                        .forEach(r -> items.add(AllMapItemSearchResponse.builder()
                                .category("project")
                                .projectId(r.getId())
                                .title(r.getTitle())
                                .teamStatus(normalizeProjectTeamStatus(r.getTeamStatus()))
                                .bannerImageUrl(r.getBannerImageUrl())
                                .location(LocationDto.builder()
                                        .address(composeRoadAddress(
                                                r.getRegion1depthName(),
                                                r.getRegion2depthName(),
                                                r.getRegion3depthName(),
                                                r.getRoadName(),
                                                r.getMainBuildingNo(),
                                                r.getSubBuildingNo()))
                                        .region1depthName(r.getRegion1depthName())
                                        .region2depthName(r.getRegion2depthName())
                                        .region3depthName(r.getRegion3depthName())
                                        .roadName(r.getRoadName())
                                        .mainBuildingNo(r.getMainBuildingNo())
                                        .subBuildingNo(r.getSubBuildingNo())
                                        .zoneNo(r.getZoneNo())
                                        .latitude(r.getLatitude())
                                        .longitude(r.getLongitude())
                                        .build())
                                .build()));
            }
        }

        if (categories.contains("study")) {
            var rows = studyRecruitmentRepository.findAllInBounds(swLat, neLat, swLng, neLng);

            if (rows != null) {
                rows.stream()
                        .filter(r -> studyStatusFilter.isEmpty() || studyStatusFilter.contains(r.getTeamStatus()))
                        .forEach(r -> items.add(AllMapItemSearchResponse.builder()
                                .category("study")
                                .studyId(r.getId())
                                .title(r.getTitle())
                                .teamStatus(normalizeStudyTeamStatus(r.getTeamStatus()))
                                .bannerImageUrl(r.getBannerImageUrl())
                                .location(LocationDto.builder()
                                        .address(composeRoadAddress(
                                                r.getRegion1depthName(),
                                                r.getRegion2depthName(),
                                                r.getRegion3depthName(),
                                                r.getRoadName(),
                                                r.getMainBuildingNo(),
                                                r.getSubBuildingNo()))
                                        .region1depthName(r.getRegion1depthName())
                                        .region2depthName(r.getRegion2depthName())
                                        .region3depthName(r.getRegion3depthName())
                                        .roadName(r.getRoadName())
                                        .mainBuildingNo(r.getMainBuildingNo())
                                        .subBuildingNo(r.getSubBuildingNo())
                                        .zoneNo(r.getZoneNo())
                                        .latitude(r.getLatitude())
                                        .longitude(r.getLongitude())
                                        .build())
                                .build()));
            }
        }

        if (categories.contains("player")) {
            var rows = userRepository.findPublicPlayersInBounds(swLat, neLat, swLng, neLng);

            if (rows != null) {
                rows.forEach(r -> {
                    User user = userRepository.findById(r.getId())
                            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

                    // 대표 배지
                    BadgeDto representative = badgeService.getRepresentativeGoodBadge(user);
                    AllMapItemSearchResponse.MainBadge mainBadge = null;
                    if (representative != null) {
                        mainBadge = AllMapItemSearchResponse.MainBadge.builder()
                                .type(representative.getType().name())
                                .tier(representative.getTier().name())
                                .build();
                    }

                    // 나쁜 배지
                    AbandonBadgeDto abandon = badgeService.getAbandonBadge(user);
                    AllMapItemSearchResponse.AbandonBadge abandonBadge =
                            AllMapItemSearchResponse.AbandonBadge.builder()
                                    .IsGranted(abandon.isIsGranted())
                                    .count(abandon.getCount())
                                    .build();

                    items.add(AllMapItemSearchResponse.builder()
                            .category("player")
                            .userId(r.getId())
                            .nickname(r.getNickname())
                            .IsMine(userId != null && userId.equals(r.getId()))
                            .profileImageUrl(r.getProfileImageUrl())
                            .temperature(r.getTemperature())
                            .mainBadge(mainBadge)
                            .abandonBadge(abandonBadge)
                            .location(LocationDto.builder()
                                    .address(composeRoadAddress(
                                            r.getRegion1DepthName(),
                                            r.getRegion2DepthName(),
                                            r.getRegion3DepthName(),
                                            r.getRoadName(),
                                            r.getMainBuildingNo(),
                                            r.getSubBuildingNo()))
                                    .region1depthName(r.getRegion1DepthName())
                                    .region2depthName(r.getRegion2DepthName())
                                    .region3depthName(r.getRegion3DepthName())
                                    .roadName(r.getRoadName())
                                    .mainBuildingNo(r.getMainBuildingNo())
                                    .subBuildingNo(r.getSubBuildingNo())
                                    .zoneNo(r.getZoneNo())
                                    .latitude(r.getLatitude())
                                    .longitude(r.getLongitude())
                                    .build())
                            .build());
                });
            }
        }

        if (categories.contains("cafe")) {
            var rows = cafeRepository.findAllInBounds(swLat, neLat, swLng, neLng);

            if (rows != null) {
                rows.forEach(r -> items.add(AllMapItemSearchResponse.builder()
                        .category("cafe")
                        .cafeId(r.getId())
                        .title(r.getTitle())
                        .bannerImageUrl(r.getBannerImageUrl())
                        .location(LocationDto.builder()
                                .address(composeRoadAddress(
                                        r.getRegion1depthName(),
                                        r.getRegion2depthName(),
                                        r.getRegion3depthName(),
                                        r.getRoadName(),
                                        r.getMainBuildingNo(),
                                        r.getSubBuildingNo()))
                                .region1depthName(r.getRegion1depthName())
                                .region2depthName(r.getRegion2depthName())
                                .region3depthName(r.getRegion3depthName())
                                .roadName(r.getRoadName())
                                .mainBuildingNo(r.getMainBuildingNo())
                                .subBuildingNo(r.getSubBuildingNo())
                                .zoneNo(r.getZoneNo())
                                .latitude(r.getLatitude())
                                .longitude(r.getLongitude())
                                .build())
                        .build()));
            }

        }

            if (centerLat != null && centerLng != null && !items.isEmpty()) {
            final double cLat = centerLat.doubleValue();
            final double cLng = centerLng.doubleValue();
            items.sort(Comparator.comparingDouble(it -> {
                var loc = it.getLocation();
                if (loc == null || loc.getLatitude() == null || loc.getLongitude() == null) {
                    return Double.MAX_VALUE;
                }
                return haversineKm(cLat, cLng, loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
            }));
        }

        Pageable pageable = PageRequest.of(page, size);

        return PageResponse.of(new PageImpl<>(
                items.stream()
                        .filter(it -> keyword == null || keyword.isBlank()
                                || (it.getTitle() != null && it.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                                || (it.getNickname() != null && it.getNickname().toLowerCase().contains(keyword.toLowerCase()))
                        )
                        .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                        .limit(pageable.getPageSize())
                        .toList(),
                pageable,
                items.size()
        ));
    }

    private Set<String> parseCategories(String categoryCsv) {
        if (categoryCsv == null || categoryCsv.isBlank()) {
            return Set.of("project", "study", "player", "cafe");
        }
        return Arrays.stream(categoryCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<goorm.ddok.project.domain.TeamStatus> parseProjectTeamStatusFilter(String filterCsv) {
        if (filterCsv == null || filterCsv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(filterCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return goorm.ddok.project.domain.TeamStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<goorm.ddok.study.domain.TeamStatus> parseStudyTeamStatusFilter(String filterCsv) {
        if (filterCsv == null || filterCsv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(filterCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return goorm.ddok.study.domain.TeamStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public PinOverlayResponse getOverlay(String category, Long id, Long currentUserId) {
        if (category == null || id == null) {
            throw new GlobalException(ErrorCode.REQUIRED_PARAMETER_MISSING);
        }
        switch (category.trim().toLowerCase()) {
            case "project": return getProjectOverlay(id);
            case "study": return getStudyOverlay(id);
            case "cafe": return getCafeOverlay(id);
            case "player": return getPlayerOverlay(id, currentUserId);
            default:
                throw new GlobalException(ErrorCode.NOT_SUPPORT_CATEGORY);
        }
    }

    private PinOverlayResponse getProjectOverlay(Long id) {
        var row = projectRecruitmentRepository.findOverlayById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        List<String> positions = splitCsv(row.getPositionsCsv());

        return PinOverlayResponse.builder()
                .category("project")
                .projectId(row.getId())
                .title(row.getTitle())
                .bannerImageUrl(row.getBannerImageUrl())
                .teamStatus(normalizeProjectTeamStatus(row.getTeamStatus()))
                .positions(positions)
                .capacity(row.getCapacity())
                .mode(row.getMode().toString())
                .address(row.getAddress())
                .preferredAges(PreferredAgesDto.of(row.getAgeMin(), row.getAgeMax()))
                .expectedMonth(row.getExpectedMonth())
                .startDate(row.getStartDate())
                .build();
    }

    private PinOverlayResponse getStudyOverlay(Long id) {
        var row = studyRecruitmentRepository.findOverlayById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        return PinOverlayResponse.builder()
                .category("study")
                .studyId(row.getId())
                .title(row.getTitle())
                .bannerImageUrl(row.getBannerImageUrl())
                .teamStatus(normalizeStudyTeamStatus(row.getTeamStatus()))
                .studyType(row.getStudyType())
                .capacity(row.getCapacity())
                .mode(row.getMode().toString())
                .address(row.getAddress())
                .preferredAges(PreferredAgesDto.of(row.getAgeMin(), row.getAgeMax()))
                .expectedMonth(row.getExpectedMonth())
                .startDate(row.getStartDate())
                .build();
    }

    private PinOverlayResponse getCafeOverlay(Long id) {
        var row = cafeRepository.findOverlayById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        return PinOverlayResponse.builder()
                .category("cafe")
                .cafeId(row.getId())
                .title(row.getName())
                .bannerImageUrl(row.getBannerImageUrl())
                .rating(row.getRating() == null ? java.math.BigDecimal.ZERO : row.getRating())
                .reviewCount(row.getReviewCount() == null ? 0L : row.getReviewCount())
                .address(row.getAddress())
                .build();
    }

    private PinOverlayResponse getPlayerOverlay(Long id, Long currentUserId) {
        var row = userRepository.findOverlayById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        boolean mine = (currentUserId != null && currentUserId.equals(row.getId()));

        String profile = row.getProfileImageUrl();

        String latestProjectStatus = null;
        if (row.getLatestProjectTeamStatus() != null) {
            latestProjectStatus = normalizeProjectTeamStatus(row.getLatestProjectTeamStatus());
        }
        String latestStudyStatus = null;
        if (row.getLatestStudyTeamStatus() != null) {
            latestStudyStatus = normalizeStudyTeamStatus(row.getLatestStudyTeamStatus());
        }

        User user = userRepository.findById(row.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        BadgeDto representative = badgeService.getRepresentativeGoodBadge(user);
        PinOverlayResponse.MainBadge mainBadge = null;
        if (representative != null) {
            mainBadge = PinOverlayResponse.MainBadge.builder()
                    .type(representative.getType().name())
                    .tier(representative.getTier().name())
                    .build();
        }

        AbandonBadgeDto abandon = badgeService.getAbandonBadge(user);
        PinOverlayResponse.AbandonBadge abandonBadge =
                PinOverlayResponse.AbandonBadge.builder()
                        .IsGranted(abandon.isIsGranted())
                        .count(abandon.getCount())
                        .build();

        return PinOverlayResponse.builder()
                .category("player")
                .userId(row.getId())
                .nickname(row.getNickname())
                .profileImageUrl(profile)
                .mainBadge(mainBadge)
                .abandonBadge(abandonBadge)
                .mainPosition(row.getMainPosition())
                .address(row.getAddress())
                .latestProject(toMini(row.getLatestProjectId(), row.getLatestProjectTitle(), latestProjectStatus))
                .latestStudy(toMini(row.getLatestStudyId(), row.getLatestStudyTitle(), latestStudyStatus))
                .temperature(row.getTemperature())
                .isMine(mine)
                .build();
    }

    private PinOverlayResponse.MiniItem toMini(Long id, String title, String status) {
        if (id == null) return null;
        return PinOverlayResponse.MiniItem.builder().id(id).title(title).teamStatus(status).build();
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
