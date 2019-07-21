package config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfiguration implements InitializingBean, DisposableBean, FactoryBean<RestHighLevelClient> {

    private RestHighLevelClient client;

    public void destroy() throws Exception {
        if (client != null){
            client.close();
        }
    }

    public void afterPropertiesSet() throws Exception {
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("192.168.0.100", 9200, "http")
                )
        );
    }

    public RestHighLevelClient getObject() throws Exception {
        return client;
    }

    public Class<?> getObjectType() {
        return RestHighLevelClient.class;
    }
}
