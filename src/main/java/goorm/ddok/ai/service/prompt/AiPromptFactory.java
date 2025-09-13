package goorm.ddok.ai.service.prompt;

import goorm.ddok.global.dto.LocationDto;
import java.util.List;
import java.util.Objects;

public final class AiPromptFactory {

    private AiPromptFactory() {}

    private static String joinComma(List<String> list) {
        if (list == null || list.isEmpty()) return "-";
        return String.join(", ", list);
    }

    private static String orDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s.trim();
    }

    /* =========================
     * 프로젝트 상세 프롬프트 (줄글, 반복 방지 강화)
     * ========================= */
    public static String buildProjectPrompt(
            String title,
            String expectedStart,
            Integer expectedMonth,
            String mode,
            LocationDto loc,
            Integer cap,
            List<String> traits,
            List<String> positions,
            String leaderPosition,
            String detail
    ) {
        String address = (loc == null) ? null : loc.getAddress();
        String scheduleStart = orDash(expectedStart);
        String scheduleMonths = (expectedMonth == null) ? "-" : expectedMonth + "개월";
        String modeKo = "online".equalsIgnoreCase(mode) ? "온라인" : "오프라인";

        // ⚠️ 반복 금지, 1회 출력, 길이 가이드, 불필요한 접두/접미 금지
        return """
            너는 한국어로 프로젝트 모집글을 **순수 줄글**로 작성하는 어시스턴트다.

            [출력 규칙]
            - 출력은 **본문 1회만** 작성한다. 같은 내용을 다시 요약하거나 반복해서 붙이지 마라.
            - **동일하거나 매우 유사한 문장/문단을 반복**하지 마라. 재진술, 말 바꾸기 반복 금지.
            - 각 문단은 **빈 줄 1개(Enter 1회)** 로만 구분한다. 목록/헤딩/체크박스/마크다운 사용 금지.
            - 과장·차별 표현 없이 **존중하는 어조**로, 간결하고 구체적으로 쓴다.
            - 제공된 사실(제목, 일정, 모집역할, 진행방식, 위치, 팀 특성, 리더 포지션, 메모)만 자연스럽게 녹여라.
            - **미제공 정보는 추정하거나 채우지 말고 생략**하라.
            - “요약/마무리/감사/서명/재요약” 같은 **불필요한 접두사·접미사**를 덧붙이지 마라.
            - 문장은 자연스럽게 연결하되, **문단 간에는 새로운 정보**를 제시하라(같은 포인트를 반복 금지).

            [입력 데이터]
            - 제목: %s
            - 시작일: %s
            - 예상 기간: %s
            - 진행 방식: %s
            - 위치(주소): %s
            - 모집 정원: %s명
            - 팀 특성: %s
            - 모집 역할: %s
            - 리더 포지션: %s
            - 작성자 메모(선택): %s
            """
                .formatted(
                        orDash(title),
                        scheduleStart,
                        scheduleMonths,
                        modeKo,
                        orDash(address),
                        (cap == null ? "-" : String.valueOf(cap)),
                        joinComma(traits),
                        joinComma(positions),
                        orDash(leaderPosition),
                        orDash(detail)
                );
    }

    /* =========================
     * 스터디 상세 프롬프트 (줄글, 반복 방지 강화)
     * ========================= */
    public static String buildStudyPrompt(
            String title,
            String expectedStart,
            Integer expectedMonth,
            String mode,
            LocationDto loc,
            Integer cap,
            List<String> traits,
            String studyType,
            String detail
    ) {
        String address = (loc == null) ? null : loc.getAddress();
        String scheduleStart = orDash(expectedStart);
        String scheduleMonths = (expectedMonth == null) ? "-" : expectedMonth + "개월";
        String modeKo = "online".equalsIgnoreCase(mode) ? "온라인" : "오프라인";

        return """
            너는 한국어로 스터디 모집글을 **순수 줄글**로 작성하는 어시스턴트다.

            [출력 규칙]
            - 출력은 **본문 1회만** 작성한다. 같은 내용을 다시 요약/재진술하지 마라.
            - **동일/유사 문장·문단 반복 금지**. 문단마다 새로운 정보를 담아라.
            - **3~4개의 문단**, 문단당 **2~4문장**. 문단 구분은 **빈 줄 1개**로만 한다.
            - 목록/헤딩/체크박스/마크다운 금지. 간결하고 자연스러운 서술문으로만 작성.
            - 제공된 사실(제목, 일정, 정원, 진행방식, 위치, 팀 특성, 스터디 유형)만 반영.
            - **미제공 정보는 추정/생성 금지**. 생략해라.
            - “요약/마무리/감사/서명/재요약” 등 불필요한 접두사·접미사 금지.

            [입력 데이터]
            - 제목: %s
            - 시작일: %s
            - 예상 기간: %s
            - 진행 방식: %s
            - 위치(주소): %s
            - 모집 정원: %s명
            - 팀 특성: %s
            - 스터디 유형: %s
            - 작성자 메모(선택): %s
            """
                .formatted(
                        orDash(title),
                        scheduleStart,
                        scheduleMonths,
                        modeKo,
                        orDash(address),
                        (cap == null ? "-" : String.valueOf(cap)),
                        joinComma(traits),
                        orDash(studyType),
                        orDash(detail)
                );
    }

    // 남겨두었지만 현재 사용 안 함(체크박스 템플릿 제거)
    private static String pick(List<String> list, int idx) {
        if (list == null || list.size() <= idx) return "-";
        String v = Objects.toString(list.get(idx), "").trim();
        return v.isBlank() ? "-" : v;
    }
}