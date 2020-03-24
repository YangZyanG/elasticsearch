package json;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.bulk.BulkRequest;

import java.io.*;

public class TestJson {

    //json转换指定field
    public static void main(String[] args) throws IOException {
//        TestEntity entity = new TestEntity();
//        entity.setId("1");
//        entity.setName("yangzy");
//        entity.setAge("27");
//        entity.setSex("male");
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("name", entity.getName());
//        jsonObject.put("age", entity.getAge());
//        System.out.println(jsonObject.toString());

        String path = "/Users/yangziyang/Desktop/dest_hotel.json";
        File file = new File(path);
        FileReader reader = new FileReader(file);//定义一个fileReader对象，用来初始化BufferedReader
        BufferedReader bReader = new BufferedReader(reader);//new一个BufferedReader对象，将文件内容读取到缓存
        StringBuilder sb = new StringBuilder();//定义一个字符串缓存，将字符串存放缓存中
        String s = "";
        while ((s =bReader.readLine()) != null) {//逐行读取文件内容，不读取换行符和末尾的空格
            sb.append(s + "\n");//将读取的字符串添加换行符后累加存放在缓存中
            System.out.println(s);
        }
        bReader.close();
        String str = sb.toString();
        System.out.println(str);


    }
}
