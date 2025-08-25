package goorm.ddok.member.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface TechStackSearchRepository extends ElasticsearchRepository<TechStackDocument, String> {
    List<TechStackDocument> findByNameContainingIgnoreCase(String keyword);
}
