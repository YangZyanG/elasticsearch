import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import service.ElasticSearchService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class test {

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext-elasticsearch.xml");
        ElasticSearchService es = (ElasticSearchService) applicationContext.getBean("elasticSearchServiceImpl");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("content", "能插入吗？");
        es.createIndex("ik_test", "5", map);
    }
}
