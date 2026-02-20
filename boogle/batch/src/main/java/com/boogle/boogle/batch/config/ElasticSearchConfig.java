package com.boogle.boogle.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.List;

@Profile("search")
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.boogle.boogle.book.infra")
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
