package valkyrie.core.utils;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class JSONUtils
{
        /**
         * 转 JSON 字符串
         */
        public static String toJSONString(Object object, SerializationFeature...features)
        {
                try {
                        ObjectMapper objectMapper = new ObjectMapper();

                        for (SerializationFeature feature : features)
                                objectMapper.enable(feature);

                        return objectMapper.writeValueAsString(object);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }
        }

        public static <T> T toJavaObject(String json, Class<T> aClass)
        {
                return JSONObject.parseObject(json, aClass);
        }

        public static <T> List<T> toJavaList(String jsonArray, Class<T> aClass)
        {
                try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        
                        CollectionType collectionType = objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, aClass);

                        return objectMapper.readValue(jsonArray, collectionType);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }
        }

        @SuppressWarnings("unchecked")
        public static <T> T deepCopy(T src)
        {
                return (T) toJavaObject(toJSONString(src), src.getClass());
        }
}
