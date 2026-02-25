package com.boogle.boogle.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.List;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.boogle.boogle.book.infra")
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class ElasticSearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private List<String> esUris;

    @Override
    public ClientConfiguration clientConfiguration() {
        // http:// 제거 로직
        String[] hosts = esUris.stream()
                .map(uri -> uri.replace("http://", ""))
                .toArray(String[]::new);

        return ClientConfiguration.builder()
                .connectedTo(hosts)
                .withConnectTimeout(5000)
                .withSocketTimeout(3000)
                .build();
    }
}
