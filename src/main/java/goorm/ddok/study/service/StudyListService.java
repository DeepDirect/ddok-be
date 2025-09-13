package goorm.ddok.study.service;

import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.project.domain.TeamStatus;
import goorm.ddok.study.domain.StudyMode;
import goorm.ddok.study.domain.StudyRecruitment;
import goorm.ddok.study.domain.StudyType;
import goorm.ddok.study.dto.response.StudyListResponse;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
public class StudyListService {

    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private static final Map<String, StudyType> TYPE_ALIAS = createTypeAlias();

    private static Map<String, StudyType> createTypeAlias() {
        Map<String, StudyType> m = new HashMap<>();

        putAlias(m, "자격증", StudyType.CERTIFICATION);
        putAlias(m, "자격증 취득", StudyType.CERTIFICATION);

        putAlias(m, "취업", StudyType.JOB_INTERVIEW);
        putAlias(m, "면접", StudyType.JOB_INTERVIEW);
        putAlias(m, "취업/면접", StudyType.JOB_INTERVIEW);

        putAlias(m, "자기 개발", StudyType.SELF_DEV);
        putAlias(m, "자기개발", StudyType.SELF_DEV);

        putAlias(m, "어학", StudyType.LANGUAGE);

        putAlias(m, "생활", StudyType.LIFE);

        putAlias(m, "취미", StudyType.HOBBY);
        putAlias(m, "교양", StudyType.HOBBY);
        putAlias(m, "취미/교양", StudyType.HOBBY);

        putAlias(m, "기타", StudyType.ETC);

        return m;
    }

    private static void putAlias(Map<String, StudyType> m, String key, StudyType val) {
        m.put(normalizeAlias(key), val);
    }

    private static String normalizeAlias(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[\\s/_-]+", "");
    }



    @Transactional(readOnly = true)
    public Page<StudyListResponse> getStudies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StudyRecruitment> rows = studyRecruitmentRepository.findByDeletedAtIsNull(pageable);
        return rows.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudyListResponse> searchStudies(
            String keyword,
            String statusCsv,
            String typeCsv,
            Integer capacity,
            String modeCsv,
            Integer ageMin,
            Integer ageMax,
            Integer expectedMonth,
            LocalDate startDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<StudyRecruitment> spec = Specification.where(isNotDeleted());

        // 키워드(제목 + 주소요소, 콤마/공백 단위 토큰)
        if (hasText(keyword)) {
            spec = spec.and(keywordSpec(keyword));
        }

        // 상태 IN
        var statusSet = parseTeamStatuses(statusCsv);
        if (!statusSet.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("teamStatus").in(statusSet));
        }

        // 스터디 유형 IN (enum name 기준; "취업/면접"처럼 들어오면 매핑이 필요)
        var typeSet = parseStudyTypes(typeCsv);
        if (!typeSet.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("studyType").in(typeSet));
        }

        // 모드 IN
        var modeSet = parseModes(modeCsv);
        if (!modeSet.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("mode").in(modeSet));
        }

        // 정원
        if (capacity != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("capacity"), capacity));
        }

        // 연령 필터
        if (ageMin != null || ageMax != null) {
            spec = spec.and((root, q, cb) -> {
                Expression<Integer> sMin = root.get("ageMin");
                Expression<Integer> sMax = root.get("ageMax");

                Integer reqMin = ageMin;
                Integer reqMax = ageMax;
                if (reqMin != null && reqMax != null && reqMin > reqMax) {
                    int tmp = reqMin; reqMin = reqMax; reqMax = tmp;
                }

                Predicate openMin = cb.or(cb.isNull(sMin), cb.equal(sMin, 0));
                Predicate openMax = cb.or(cb.isNull(sMax), cb.equal(sMax, 0));

                List<Predicate> ands = new ArrayList<>();

                if (reqMin != null && reqMax != null) {
                    Predicate coversLower = cb.or(openMin, cb.lessThanOrEqualTo(sMin, reqMin));
                    Predicate coversUpper = cb.or(openMax, cb.greaterThanOrEqualTo(sMax, reqMax));
                    ands.add(cb.and(coversLower, coversUpper));
                } else if (reqMin != null) {
                    Predicate covers = cb.or(openMax, cb.greaterThanOrEqualTo(sMax, reqMin));
                    ands.add(covers);
                } else {
                    Predicate covers = cb.or(openMin, cb.lessThanOrEqualTo(sMin, reqMax));
                    ands.add(covers);
                }

                return cb.and(ands.toArray(new Predicate[0]));
            });
        }

        // 예상 개월 수(정확히 일치)
        if (expectedMonth != null) {
            spec = spec.and((root, q, cb) -> {
                if (expectedMonth >= 5) {
                    return cb.greaterThanOrEqualTo(root.get("expectedMonths"), 5);
                } else {
                    return cb.equal(root.get("expectedMonths"), expectedMonth);
                }
            });
        }

        // 시작일(이후)
        if (startDate != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
        }

        Page<StudyRecruitment> rows = studyRecruitmentRepository.findAll(spec, pageable);
        return rows.map(this::toResponse);
    }

    private Specification<StudyRecruitment> isNotDeleted() {
        return (root, q, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private Specification<StudyRecruitment> keywordSpec(String keyword) {
        List<String> tokens = splitTokens(keyword);
        return (root, q, cb) -> {
            List<Predicate> andByToken = new ArrayList<>();
            for (String token : tokens) {
                String like = "%" + token + "%";
                Predicate orPerToken = cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("region1depthName"), like),
                        cb.like(root.get("region2depthName"), like),
                        cb.like(root.get("region3depthName"), like),
                        cb.like(root.get("roadName"), like),
                        cb.like(root.get("mainBuildingNo"), like),
                        cb.like(root.get("subBuildingNo"), like),
                        cb.like(cb.concat(cb.concat(root.get("region1depthName"), " "),
                                root.get("region2depthName")), like)
                );
                andByToken.add(orPerToken);
            }
            return cb.and(andByToken.toArray(new Predicate[0]));
        };
    }

    private Set<TeamStatus> parseTeamStatuses(String csv) {
        if (!hasText(csv)) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(val -> {
                    try { return TeamStatus.valueOf(val); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TeamStatus.class)));
    }

    private Set<StudyType> parseStudyTypes(String csv) {
        if (!hasText(csv)) return Collections.emptySet();
        Set<StudyType> set = EnumSet.noneOf(StudyType.class);

        for (String token : csv.split(",")) {
            String raw = token.trim();
            if (raw.isEmpty()) continue;

            String enumCandidate = raw.replaceAll("[\\s/-]+", "_").toUpperCase(Locale.ROOT);
            try {
                set.add(StudyType.valueOf(enumCandidate));
                continue;
            } catch (IllegalArgumentException ignored) {}

            StudyType byAlias = TYPE_ALIAS.get(normalizeAlias(raw));
            if (byAlias != null) set.add(byAlias);
        }
        return set;
    }

    private Set<StudyMode> parseModes(String csv) {
        if (!hasText(csv)) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .map(val -> {
                    try { return StudyMode.valueOf(val); } // enum이 소문자 online/offline
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(StudyMode.class)));
    }

    private List<String> splitTokens(String raw) {
        return Arrays.stream(raw.split("[,\\s]+"))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
    }


    private StudyListResponse toResponse(StudyRecruitment s) {
        String address = (s.getMode() == StudyMode.online)
                ? "online"
                : shortAddress(s.getRegion1depthName(), s.getRegion2depthName());

        return StudyListResponse.builder()
                .studyId(s.getId())
                .title(s.getTitle())
                .teamStatus(String.valueOf(s.getTeamStatus()))
                .bannerImageUrl(s.getBannerImageUrl())
                .capacity(s.getCapacity())
                .mode(s.getMode())
                .address(address)
                .studyType(s.getStudyType())
                .preferredAges(PreferredAgesDto.of(
                        (s.getAgeMin() != null && s.getAgeMin() == 0) ? null : s.getAgeMin(),
                        (s.getAgeMax() != null && s.getAgeMax() == 0) ? null : s.getAgeMax()
                ))
                .expectedMonth(s.getExpectedMonths())
                .startDate(s.getStartDate())
                .build();
    }

    private String shortAddress(String region1, String region2) {
        String r1 = region1 == null ? "" : region1.trim();
        String r2 = region2 == null ? "" : region2.trim();
        if (r1.isEmpty() && r2.isEmpty()) return "-";
        return (r1 + " " + r2).trim();
    }

}
