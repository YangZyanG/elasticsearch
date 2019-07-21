package service.impl;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.ElasticSearchService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

    @Autowired
    private RestHighLevelClient client;

    public void createIndex(String indexName, String jsonSource) {
        IndexRequest indexRequest = new IndexRequest(indexName);
        indexRequest.id(getId()).source(jsonSource, XContentType.JSON).opType(DocWriteRequest.OpType.CREATE);
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, getListener());
    }

    public void createIndex(String indexName, Map<String, Object> mapSource) {
        IndexRequest indexRequest = new IndexRequest(indexName);
        indexRequest.id(getId()).source(mapSource).opType(DocWriteRequest.OpType.CREATE);
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, getListener());
    }

    public void createIndex(String indexName, XContentBuilder builderSource) {
        IndexRequest indexRequest = new IndexRequest(indexName);
        indexRequest.id(getId()).source(builderSource).opType(DocWriteRequest.OpType.CREATE);
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, getListener());
    }

    public void createIndex(String indexName, String id, String jsonSource) {
        IndexRequest indexRequest = new IndexRequest(indexName);
        indexRequest.id(id).source(jsonSource, XContentType.JSON).opType(DocWriteRequest.OpType.CREATE);
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, getListener());
    }

    public void createIndex(String indexName, String id, Map<String, Object> mapSource) {
        IndexRequest indexRequest = new IndexRequest(indexName);
        indexRequest.id(id).source(mapSource).opType(DocWriteRequest.OpType.CREATE);
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, getListener());
    }

    /***
     * 返回指定索引、id的_source内容
     * FetchSourceContext.DO_NOT_FETCH_SOURCE表示不能检索_source内容，这里暂时不设置
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    public Map<String, Object> get(String indexName, String id) throws IOException {
        GetRequest request = new GetRequest(indexName, id);
        GetResponse response = null;
        try {
            response = client.get(request, RequestOptions.DEFAULT);
        }catch (ElasticsearchException e){
            if (e.status() == RestStatus.NOT_FOUND){
                System.out.println("the [" + indexName + "] is not found");
            }
        }
        return response.getSourceAsMap();
    }

    /***
     * 返回指定索引、id的指定字段的_source内容
     * @param indexName
     * @param id
     * @param includes
     * @return
     * @throws IOException
     */
    public Map<String, Object> getIncludes(String indexName, String id, String[] includes) throws IOException {
        GetRequest request = new GetRequest(indexName, id);
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, Strings.EMPTY_ARRAY);
        request.fetchSourceContext(fetchSourceContext);
        GetResponse response = null;
        try {
            response = client.get(request, RequestOptions.DEFAULT);
        }catch (ElasticsearchException e){
            if (e.status() == RestStatus.NOT_FOUND){
                System.out.println("the [" + indexName + "] is not found");
            }
        }
        return response.getSourceAsMap();
    }

    /***
     * 返回指定索引、id的排除指定字段的_source内容
     * @param indexName
     * @param id
     * @param excludes
     * @return
     * @throws IOException
     */
    public Map<String, Object> getExcludes(String indexName, String id, String[] excludes) throws IOException {
        GetRequest request = new GetRequest(indexName, id);
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, Strings.EMPTY_ARRAY, excludes);
        request.fetchSourceContext(fetchSourceContext);
        GetResponse response = null;
        try {
            response = client.get(request, RequestOptions.DEFAULT);
        }catch (ElasticsearchException e){
            if (e.status() == RestStatus.NOT_FOUND){
                System.out.println("the [" + indexName + "] is not found");
            }
        }
        return response.getSourceAsMap();
    }

    /***
     * 返回指定索引、id是否存在
     * 因为这里方法只返回true/false，我们建议关闭提取_source以及禁用提取储存的字段
     * @param indexName
     * @param id
     * @return
     */
    public boolean exists(String indexName, String id) throws IOException {
        GetRequest request = new GetRequest(indexName, id);
        request.fetchSourceContext(new FetchSourceContext(false));
        request.storedFields("_none_");
        return client.exists(request, RequestOptions.DEFAULT);
    }

    public void del(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        client.delete(request, RequestOptions.DEFAULT);

    }

    public void update(String indexName, String id, String jsonUpdate) throws IOException {
        UpdateRequest request = new UpdateRequest(indexName, id);
        request.doc(jsonUpdate, XContentType.JSON);
        client.update(request, RequestOptions.DEFAULT);
    }

    public void update(String indexName, String id, Map<String, Object> mapUpdate) throws IOException {
        UpdateRequest request = new UpdateRequest(indexName, id);
        request.doc(mapUpdate);
        client.update(request, RequestOptions.DEFAULT);
    }

    public void bulkIndex(String indexName, List<String> jsonList) {
        BulkRequest request = new BulkRequest();
        for (String json : jsonList){

        }
    }

    private ActionListener<IndexResponse> getListener(){
        return new ActionListener<IndexResponse>() {

            public void onResponse(IndexResponse indexResponse) {
                if (indexResponse.getResult() == DocWriteResponse.Result.CREATED){
                    System.out.println("create [" + indexResponse.getIndex() + "] success");
                }else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED){
                    System.out.println("update [" + indexResponse.getIndex() + "] success");
                }
            }

            public void onFailure(Exception e) {
                System.out.println("create index failure：" + e);
            }
        };
    }

    private String getId(){
        return UUID.randomUUID().toString().replace("-", "");
    }
}
