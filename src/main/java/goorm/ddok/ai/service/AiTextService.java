package goorm.ddok.ai.service;

import goorm.ddok.ai.dto.request.AiProjectRequest;
import goorm.ddok.ai.dto.request.AiStudyRequest;
import goorm.ddok.ai.service.prompt.AiPromptFactory;
import goorm.ddok.ai.service.provider.AiModelClient;
import goorm.ddok.global.dto.LocationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTextService {

    private final AiModelClient model; // ClovaStreamClient 또는 ClovaModelClient 주입

    public String generateProjectDetail(AiProjectRequest req) {
        String prompt = AiPromptFactory.buildProjectPrompt(
                safe(req.getTitle()),
                toDateString(req.getExpectedStart()),
                req.getExpectedMonth(),
                toModeString(req.getMode()),
                toLocation(req.getLocation()),
                req.getCapacity(),
                safeList(req.getTraits()),
                safeList(req.getPositions()),
                safe(req.getLeaderPosition()),
                safe(req.getDetail())
        );
        // 템플릿은 충분히 길 수 있으니 maxTokens 넉넉히
        return model.generate(prompt, 800);
    }

    public String generateStudyDetail(AiStudyRequest req) {
        String prompt = AiPromptFactory.buildStudyPrompt(
                safe(req.getTitle()),
                toDateString(req.getExpectedStart()),
                req.getExpectedMonth(),
                toModeString(req.getMode()),
                toLocation(req.getLocation()),
                req.getCapacity(),
                safeList(req.getTraits()),
                safe(req.getStudyType()),
                safe(req.getDetail())
        );
        return model.generate(prompt, 800);
    }

    /* =========================
     * helpers
     * ========================= */

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static List<String> safeList(List<String> list) {
        return (list == null) ? List.of() : list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
    }

    private static String toDateString(LocalDate d) {
        return (d == null) ? null : d.toString(); // yyyy-MM-dd
    }

    /**
     * enum(ONLINE/OFFLINE) 또는 문자열("online"/"offline") 모두 대응
     */
    private static String toModeString(Object mode) {
        if (mode == null) return null;
        String v = mode.toString().trim().toLowerCase();
        if (v.equals("online") || v.equals("offline")) return v;
        // 혹시 다른 표현이면 그대로 전달
        return v;
    }

    /**
     * 요청 DTO의 location 타입이 goorm.ddok.global.dto.LocationDto와 동일하다면 그대로 캐스팅,
     * 아니라면 address만 있는 간이 LocationDto로 변환.
     */
    private static LocationDto toLocation(Object loc) {
        if (loc == null) return null;
        if (loc instanceof LocationDto l) return l;

        // reflection 없이 address 필드만 얕게 시도 (필드명이 다르면 null 처리)
        try {
            var m = loc.getClass().getMethod("getAddress");
            Object addr = m.invoke(loc);
            String address = (addr == null) ? null : addr.toString();
            return LocationDto.builder().address(address).build();
        } catch (Exception ignore) {
            // address가 없으면 null
            return null;
        }
    }
}