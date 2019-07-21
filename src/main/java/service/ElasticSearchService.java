package service;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ElasticSearchService {

    void createIndex(String indexName, String jsonSource);

    void createIndex(String indexName, Map<String, Object> mapSource);

    void createIndex(String indexName, XContentBuilder builderSource);

    void createIndex(String indexName, String id, String jsonSource);

    void createIndex(String indexName, String id, Map<String, Object> mapSource);

    Map<String, Object> get(String indexName, String id) throws IOException;

    Map<String, Object> getIncludes(String indexName, String id, String[] includes) throws IOException;

    Map<String, Object> getExcludes(String indexName, String id, String[] excludes) throws IOException;

    boolean exists(String indexName, String id) throws IOException;

    void del(String indexName, String id) throws IOException;

    void update(String indexName, String id, String jsonUpdate) throws IOException;

    void update(String indexName, String id, Map<String, Object> mapUpdate) throws IOException;

    void bulkIndex(String indexName, List<String> jsonList);
}
