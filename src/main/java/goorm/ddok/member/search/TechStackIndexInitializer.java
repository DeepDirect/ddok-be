package goorm.ddok.member.search;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TechStackIndexInitializer {
    private final ElasticsearchOperations operations;

    @PostConstruct
    public void init() {
        IndexOperations io = operations.indexOps(TechStackDocument.class);
        if (!io.exists()) {
            createIndexWithSafeSettings(io);
        }
    }

    private void createIndexWithSafeSettings(IndexOperations io) {
        Map<String, Object> settings = Map.of(
                "number_of_shards", 1,
                "number_of_replicas", 0,
                "analysis", Map.of(
                        "char_filter", Map.of(
                                "remove_spaces_cf", Map.of(
                                        "type", "pattern_replace",
                                        "pattern", "\\s+",
                                        "replacement", ""
                                )
                        ),
                        "filter", Map.of(
                                "tech_lc", Map.of("type", "lowercase"),
                                "tech_ascii", Map.of("type", "asciifolding")
                        ),
                        "normalizer", Map.of(
                                "tech_normalizer", Map.of(
                                        "type", "custom",
                                        "filter", List.of("tech_lc", "tech_ascii") // ✅ char_filter 넣지 않음
                                )
                        ),
                        "tokenizer", Map.of(
                                "tech_edge_ngram", Map.of(
                                        "type", "edge_ngram",
                                        "min_gram", 1,
                                        "max_gram", 20,
                                        "token_chars", List.of("letter", "digit")
                                )
                        ),
                        "analyzer", Map.of(
                                "tech_index_analyzer", Map.of(
                                        "type", "custom",
                                        "tokenizer", "tech_edge_ngram",
                                        "filter", List.of("lowercase", "asciifolding")
                                ),
                                "tech_search_analyzer", Map.of(
                                        "type", "custom",
                                        "tokenizer", "standard",
                                        "filter", List.of("lowercase", "asciifolding")
                                ),
                                "tech_keyword_like_analyzer", Map.of(
                                        "type", "custom",
                                        "tokenizer", "keyword",
                                        "char_filter", List.of("remove_spaces_cf"),
                                        "filter", List.of("lowercase", "asciifolding")
                                )
                        )
                )
        );

        Map<String, Object> mappings = Map.of(
                "properties", Map.of(
                        "name", Map.of(
                                "type", "text",
                                "analyzer", "tech_index_analyzer",
                                "search_analyzer", "tech_search_analyzer",
                                "fields", Map.of(
                                        "kw", Map.of(
                                                "type", "keyword",
                                                "normalizer", "tech_normalizer"
                                        ),
                                        "norm", Map.of(
                                                "type", "text",
                                                "analyzer", "tech_keyword_like_analyzer",
                                                "search_analyzer", "tech_keyword_like_analyzer"
                                        ),
                                        "suggest", Map.of("type", "completion")
                                )
                        ),
                        "category", Map.of("type", "keyword"),
                        "popularity", Map.of("type", "integer")
                )
        );

        Document settingsDoc = Document.from(settings);
        Document mappingsDoc = Document.from(mappings);

        io.create(settingsDoc);
        io.putMapping(mappingsDoc);
    }
}
