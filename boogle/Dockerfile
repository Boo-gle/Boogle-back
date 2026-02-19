FROM docker.elastic.co/elasticsearch/elasticsearch:9.2.3

# 한글 검색을 위한 Nori 플러그인 설치
RUN elasticsearch-plugin install analysis-nori

# 파일을 복사한 후 소유권을 elasticsearch 유저로 변경
COPY --chown=elasticsearch:elasticsearch synonyms.txt /usr/share/elasticsearch/config/