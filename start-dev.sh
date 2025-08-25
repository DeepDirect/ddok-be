set -e

# =========================
# 환경 변수 (옵션)
# =========================
# Kibana도 같이 올릴지 여부 (template에 kibana 서비스가 없으면 자동 스킵)
START_KIBANA="${START_KIBANA:-true}"
# 기존 tech_stacks 인덱스를 강제 재생성할지 여부 (true면 삭제 후 재생성)
FORCE_RECREATE_TECHSTACKS="${FORCE_RECREATE_TECHSTACKS:-true}"

# 0. application-env.properties 경로 변수
ENV_FILE="src/main/resources/application-env.properties"

# 1. 랜덤 비밀번호 생성
REDIS_PASSWORD=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)
export REDIS_PASSWORD

# 2. docker-compose.yml 생성
envsubst < docker-compose-template.yml > docker-compose.yml

# 3. application-env.properties에 redis 비번 삽입/치환
if grep -q '^spring.data.redis.password=' "$ENV_FILE"; then
  sed -i '' "s/^spring\.data\.redis\.password=.*/spring.data.redis.password=$REDIS_PASSWORD/" "$ENV_FILE"
else
  echo "spring.data.redis.password=$REDIS_PASSWORD" >> "$ENV_FILE"
fi

# -------------------------
# 유틸 함수
# -------------------------
wait_for_es() {
  echo "🔎 Elasticsearch health 대기 중..."
  for i in {1..60}; do
    # 200 응답 여부
    if curl -sSf "http://localhost:9200/" >/dev/null 2>&1; then
      STATUS=$(curl -s "http://localhost:9200/_cluster/health" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
      if [ "$STATUS" = "green" ] || [ "$STATUS" = "yellow" ]; then
        echo "✅ Elasticsearch health: $STATUS"
        return 0
      fi
      echo "⏳ Elasticsearch status: ${STATUS:-unknown} (재시도)"
    else
      echo "⏳ Elasticsearch 응답 대기..."
    fi
    sleep 2
  done
  echo "❌ Elasticsearch health 확인 실패"
  return 1
}

wait_for_kibana() {
  echo "🔎 Kibana status 대기 중..."
  for i in $(seq 1 60); do
    # /api/status 호출 (200/JSON이면 파일 저장), 실패해도 계속
    CODE=$(curl -s -o /tmp/kbn_status.json -w '%{http_code}' http://localhost:5601/api/status || echo 000)

    if [ "$CODE" = "200" ]; then
      # Kibana 8.x: overall.level.id 가 'available' 이면 준비 완료
      if grep -q '"overall"' /tmp/kbn_status.json && grep -q '"id":"available"' /tmp/kbn_status.json; then
        echo "✅ Kibana: available"
        return 0
      fi
      # 구버전/다른 포맷 대비: 본문에 'available' 또는 'green' 이라는 단어가 있으면 통과
      if grep -q '"available"' /tmp/kbn_status.json || grep -q '"green"' /tmp/kbn_status.json; then
        echo "✅ Kibana: available"
        return 0
      fi
      echo "⏳ Kibana 응답 200 (아직 준비 중)"
    else
      # 보안/리다이렉트 등으로 /api/status가 안 열려도, 루트가 200이면 UI는 뜬 것
      ROOT_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:5601/ || echo 000)
      if [ "$ROOT_CODE" = "200" ]; then
        echo "✅ Kibana UI 응답 200 → 진행"
        return 0
      fi
      echo "⏳ Kibana 응답 코드: $CODE"
    fi

    sleep 2
  done
  echo "⚠️ Kibana 상태 확인 실패(계속 진행)"
  return 0
}

create_or_recreate_techstacks_index() {
  # 인덱스 존재 확인
  HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:9200/tech_stacks")
  if [ "$HTTP_CODE" = "200" ]; then
    if [ "$FORCE_RECREATE_TECHSTACKS" = "true" ]; then
      echo "🧨 기존 tech_stacks 인덱스 삭제(강제 재생성 모드)"
      curl -s -XDELETE "http://localhost:9200/tech_stacks" >/dev/null
    else
      echo "ℹ️ tech_stacks 인덱스가 이미 존재합니다 (재생성 안 함)"
      return 0
    fi
  fi

  echo "🧱 tech_stacks 인덱스 생성(안전한 분석기/매핑)"
  cat > /tmp/tech_stacks_mapping.json <<'JSON'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "char_filter": {
        "remove_spaces_cf": {
          "type": "pattern_replace",
          "pattern": "\\s+",
          "replacement": ""
        }
      },
      "filter": {
        "tech_lc": { "type": "lowercase" },
        "tech_ascii": { "type": "asciifolding" },

        "edge_ngram_filter": { "type": "edge_ngram", "min_gram": 1, "max_gram": 20 },

        "ko_en_syn_index": {
          "type": "synonym",
          "expand": true,
          "synonyms": [
            "spring boot, springboot, 스프링부트, 스프링 부트",
            "spring data jpa, springdatajpa, 스프링데이터 jpa, 스프링 데이터 jpa, 스프링데이터 제이피에이, 스프링 데이터 제이피에이, jpa, 제이피에이",
            "spring security, 스프링시큐리티, 스프링 시큐리티",
            "java, 자바",
            "kotlin, 코틀린",
            "python, 파이썬",
            "node, node js, nodejs, node.js, 노드, 노드제이에스",
            "express, 익스프레스, 엑스프레스",
            "typescript, ts, 타입스크립트",
            "react, 리액트",
            "next, next js, nextjs, next.js, 넥스트, 넥스트 js",
            "vue, vue js, vuejs, vue.js, 뷰, 뷰 js",
            "angular, 앵귤러",
            "mysql, my sql, 마이에스큐엘",
            "postgresql, postgres, psql, 포스트그레스, 포스트그레스큐엘",
            "redis, 레디스",
            "mongodb, mongo, 몽고디비, 몽고 db",
            "docker, 도커",
            "kubernetes, k8s, 쿠버네티스",
            "aws, 아마존웹서비스, 아마존 웹 서비스",
            "github actions, gh actions, 깃허브액션, 깃허브 액션, 깃헙액션, 깃헙 액션"
          ]
        },

        "ko_en_syn": {
          "type": "synonym_graph",
          "synonyms": [
            "spring boot, springboot, 스프링부트, 스프링 부트",
            "spring data jpa, springdatajpa, 스프링데이터 jpa, 스프링 데이터 jpa, 스프링데이터 제이피에이, 스프링 데이터 제이피에이, jpa, 제이피에이",
            "spring security, 스프링시큐리티, 스프링 시큐리티",
            "java, 자바",
            "kotlin, 코틀린",
            "python, 파이썬",
            "node, node js, nodejs, node.js, 노드, 노드제이에스",
            "express, 익스프레스, 엑스프레스",
            "typescript, ts, 타입스크립트",
            "react, 리액트",
            "next, next js, nextjs, next.js, 넥스트, 넥스트 js",
            "vue, vue js, vuejs, vue.js, 뷰, 뷰 js",
            "angular, 앵귤러",
            "mysql, my sql, 마이에스큐엘",
            "postgresql, postgres, psql, 포스트그레스, 포스트그레스큐엘",
            "redis, 레디스",
            "mongodb, mongo, 몽고디비, 몽고 db",
            "docker, 도커",
            "kubernetes, k8s, 쿠버네티스",
            "aws, 아마존웹서비스, 아마존 웹 서비스",
            "github actions, gh actions, 깃허브액션, 깃허브 액션, 깃헙액션, 깃헙 액션"
          ]
        }
      },

      "normalizer": {
        "tech_normalizer": { "type": "custom", "filter": ["tech_lc", "tech_ascii"] }
      },

      "analyzer": {
        "tech_index_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [ "tech_lc", "tech_ascii", "ko_en_syn_index", "edge_ngram_filter" ]
        },
        "tech_search_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [ "tech_lc", "tech_ascii", "ko_en_syn" ]
        },
        "tech_keyword_like_analyzer": {
          "type": "custom",
          "tokenizer": "keyword",
          "char_filter": ["remove_spaces_cf"],
          "filter": [ "tech_lc", "tech_ascii" ]
        }
      }
    }
  },

  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "tech_index_analyzer",
        "search_analyzer": "tech_search_analyzer",
        "fields": {
          "kw":   { "type": "keyword", "normalizer": "tech_normalizer" },
          "norm": { "type": "text", "analyzer": "tech_keyword_like_analyzer", "search_analyzer": "tech_keyword_like_analyzer" },
          "suggest": { "type": "completion" }
        }
      },
      "category":   { "type": "keyword" },
      "popularity": { "type": "integer" }
    }
  }
}
JSON

  curl -s -XPUT "http://localhost:9200/tech_stacks" \
    -H 'Content-Type: application/json' \
    --data-binary @/tmp/tech_stacks_mapping.json >/dev/null

  echo "✅ tech_stacks 인덱스 생성 완료"
}

# -------------------------
# 컨테이너 기동
# -------------------------
echo "🧱 Redis 컨테이너 실행 중..."
docker-compose up -d redis || { echo "❌ Redis 실행 실패"; exit 1; }
echo "✅ Redis 실행 완료."

echo "🔎 Elasticsearch 컨테이너 실행 중..."
docker-compose up -d elasticsearch || { echo "❌ Elasticsearch 실행 실패"; exit 1; }
echo "⏳ Elasticsearch health 확인 중..."
wait_for_es

if [ "$START_KIBANA" = "true" ]; then
  echo "📊 Kibana 컨테이너 실행 중..."
  # kibana 서비스가 template에 없으면 실패하므로, 실패해도 계속 진행
  docker-compose up -d kibana || true
  # healthcheck 없는 경우 대비하여 수동 상태 대기
  wait_for_kibana || true
fi

# -------------------------
# ES 인덱스 준비
# -------------------------
create_or_recreate_techstacks_index

# (옵션) 샘플 한 건 색인해 간단 스모크
# curl -s -XPOST "http://localhost:9200/tech_stacks/_doc" \
#   -H 'Content-Type: application/json' \
#   -d '{ "name": "Spring Boot", "category": "backend", "popularity": 100 }' >/dev/null
# curl -s -XPOST "http://localhost:9200/tech_stacks/_refresh" >/dev/null

# -------------------------
# 애플리케이션 기동
# -------------------------
echo "🚀 Spring Boot 서버 실행 중..."
./gradlew bootRun
