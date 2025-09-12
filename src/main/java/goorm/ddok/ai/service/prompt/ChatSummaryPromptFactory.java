package goorm.ddok.ai.service.prompt;

import goorm.ddok.chat.domain.ChatMessage;
import goorm.ddok.member.domain.User;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ChatSummaryPromptFactory {
    private ChatSummaryPromptFactory() {}

    public static String build(String roomTitle, List<ChatMessage> texts) {
        // HH:mm [닉네임] 내용  형태의 전사
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.of("Asia/Seoul"));

        StringBuilder transcript = new StringBuilder();
        for (ChatMessage m : texts) {
            String t = (m.getCreatedAt() == null) ? "--:--" : fmt.format(m.getCreatedAt());
            User sender = m.getSender();
            String nick = (sender == null || sender.getNickname() == null || sender.getNickname().isBlank())
                    ? "구성원" : sender.getNickname().trim();

            String content = sanitize(m.getContentText());
            if (!content.isBlank()) {
                transcript.append(t).append(" [").append(nick).append("] ")
                        .append(content).append("\n");
            }
        }

        // 요약 지시 (마크다운 섹션 고정)
        return """
                너는 한국어 요약 비서다. 아래 ‘채팅 전사’를 읽고 간결하고 구조화된 요약을 작성하라.

                [지시]
                - 핵심 요점/결정/할 일을 위주로 정리하라.
                - 인명/개인정보는 역할/구성원으로 일반화하라.
                - 중복·잡담은 제거하라.
                - 불확실한 내용은 추정하지 말고 생략하라.
                - 출력 형식의 제목과 공백/개행을 반드시 그대로 지켜라.

                [출력 형식(마크다운)]
                ## 요약
                - (핵심 요점 2~6개)

                ## 결정 사항
                - (결정 0~6개, 없으면 생략)

                ## 다음 액션
                - (담당자 없이 액션 중심으로 1~6개)

                [채팅방] %s
                [채팅 전사]
                ----
                %s
                ----
                """.formatted(roomTitle, transcript.toString());
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        // 줄바꿈은 한 칸 개행으로 통일, 기타 공백 압축
        String normalized = s.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\t\\f]+", " ")
                .replaceAll(" +", " ");
        // 너무 긴 연속 개행 방지
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }
}