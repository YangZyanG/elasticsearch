package json;

import com.alibaba.fastjson.JSONObject;

public class TestJson {

    //json转换指定field
    public static void main(String[] args) {
        TestEntity entity = new TestEntity();
        entity.setId("1");
        entity.setName("yangzy");
        entity.setAge("27");
        entity.setSex("male");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", entity.getName());
        jsonObject.put("age", entity.getAge());
        System.out.println(jsonObject.toString());
    }
}
