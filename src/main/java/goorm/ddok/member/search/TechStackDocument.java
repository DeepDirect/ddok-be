package goorm.ddok.member.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "tech_stacks")
public class TechStackDocument {

    @Id
    private String id;

    /** 검색 표기명 */
    @Field(type = FieldType.Text, analyzer = "tech_index_analyzer", searchAnalyzer = "tech_search_analyzer")
    private String name;

    /** 공백 제거 + lowercase + asciifolding 단일 토큰 검색용 */
    @Field(name = "name.norm", type = FieldType.Text, analyzer = "tech_keyword_like_analyzer", searchAnalyzer = "tech_keyword_like_analyzer")
    private String nameNorm; // 실제 인덱싱 시 동기화 용도(선택). 없더라도 검색은 필드 경로로 접근 가능.

    /** 카테고리(백엔드/프론트/데브옵스 등) */
    @Field(type = FieldType.Keyword)
    private String category;

    /** 인기도 */
    @Field(type = FieldType.Integer)
    private Integer popularity;

    /** 자동완성 추천(completion) */
    @CompletionField(maxInputLength = 100)
    private Completion suggest;
}