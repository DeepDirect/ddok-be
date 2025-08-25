package goorm.ddok.member.service;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import goorm.ddok.member.dto.response.TechStackResponse;
import goorm.ddok.member.repository.TechStackRepository;
import goorm.ddok.member.search.TechStackDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechStackSearchService {

    private final ElasticsearchOperations operations;
    private final TechStackRepository jpaRepo;

    public TechStackResponse searchTechStacks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new TechStackResponse(List.of());
        }

        String original = keyword.trim();
        String normalized = normalizeKeyword(keyword);

        NativeQuery query = buildOptimizedQuery(original, normalized);

        try {
            var hits = operations.search(query, TechStackDocument.class);

            List<String> names = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(TechStackDocument::getName)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .limit(10)
                    .toList();

            if (names.isEmpty()) {
                names = jpaRepo.findNamesByKeyword(original, org.springframework.data.domain.PageRequest.of(0, 10));
            }
            return new TechStackResponse(names);
        } catch (DataAccessResourceFailureException e) {
            // ES down 등 연결 문제
            return new TechStackResponse(List.of());
        } catch (org.springframework.data.elasticsearch.UncategorizedElasticsearchException |
                 co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            // 매핑/필드 오류 포함 → 폴백
            return new TechStackResponse(List.of());
        }
    }

    private String normalizeKeyword(String keyword) {

        String trimmed = keyword == null ? "" : keyword.strip();
        if (trimmed.isEmpty()) return "";

        return trimmed
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static int codePointLen(String s) {
        return (s == null) ? 0 : s.codePointCount(0, s.length());
    }

    private NativeQuery buildOptimizedQuery(String original, String normalized) {
        // 길이가 1~2면 편집거리 1을 강제, 그 외는 AUTO
        String fuzziness = codePointLen(original) <= 2 ? "1" : "AUTO";

        var bool = QueryBuilders.bool()
                // 정확/접두 (공백 제거 단일 토큰)
                .should(QueryBuilders.term()
                        .field("name.norm")
                        .value(normalized)
                        .boost(10.0f)
                        .build()._toQuery())
                .should(QueryBuilders.prefix()
                        .field("name.norm")
                        .value(normalized)
                        .boost(5.0f)
                        .build()._toQuery())

                // 자연어 매치(동의어 적용됨: tech_search_analyzer)
                .should(QueryBuilders.match()
                        .field("name")
                        .query(original)
                        .boost(3.0f)
                        .build()._toQuery())

                // 오타 허용 매치 (길이에 따라 fuzziness 동적 적용)
                .should(QueryBuilders.match()
                        .field("name")
                        .query(original)
                        .fuzziness(fuzziness)
                        .fuzzyTranspositions(true)
                        .prefixLength(0)
                        .boost(2.0f)
                        .build()._toQuery())

                .minimumShouldMatch("1")
                .build();

        return NativeQuery.builder()
                .withQuery(bool._toQuery())
                .withPageable(org.springframework.data.domain.PageRequest.of(0, 20))
                .build();
    }
}
