package goorm.ddok.member.search;

import goorm.ddok.member.domain.TechStack;
import goorm.ddok.member.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TechStackIndexer implements ApplicationRunner {

    private final TechStackRepository jpaRepo;
    private final TechStackSearchRepository esRepo;

    @Override
    public void run(ApplicationArguments args) {
        long esCount = esRepo.count();
        if (esCount > 0) {
            log.info("[TechStackIndexer] ES already has {} docs. Skip bootstrap.", esCount);
            return;
        }

        List<TechStack> all = jpaRepo.findAll();
        if (all.isEmpty()) {
            log.info("[TechStackIndexer] No rows in DB. Nothing to index.");
            return;
        }

        List<TechStackDocument> docs = new ArrayList<>(all.size());
        for (TechStack t : all) {
            docs.add(TechStackDocument.builder()
                    .id(String.valueOf(t.getId()))
                    .name(t.getName())
                    .build());
        }
        esRepo.saveAll(docs);
        log.info("[TechStackIndexer] Indexed {} tech stacks into Elasticsearch.", docs.size());
    }
}
