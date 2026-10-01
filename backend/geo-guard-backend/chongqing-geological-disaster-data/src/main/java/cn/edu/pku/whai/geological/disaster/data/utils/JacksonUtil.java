/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.utils;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * jackson工具类
 */
public class JacksonUtil {

    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JacksonUtil.class);

    static {
        // 属性在json有, entity没有, 不抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 属性在json有, entity有, 但标记为ignore注解, 不抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        // 如果是空对象的时候,不抛异常
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 支持json中的key无双引号
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 支持带单引号的key
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // int类型为null, 则抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        // 枚举找不到值, 不抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);

        // 属性为null的转换
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // date格式化
        objectMapper.setDateFormat(new SimpleDateFormat(DatePattern.NORM_DATETIME_PATTERN));

        // 注册一个时间序列化及反序列化的处理模块，用于解决jdk8中localDateTime等的序列化问题
        final JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DatePattern.NORM_DATETIME_FORMATTER));
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DatePattern.NORM_DATETIME_FORMATTER));
        objectMapper.registerModule(module);

    }

    private JacksonUtil() {

    }

    /**
     * 对象转Json
     *
     * @param o
     * @return
     */
    public static String toJson(Object o) {
        if (o == null) {
            return "";
        }
        String json = "";
        try {
            json = objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            logger.error("JacksonUtil.toJson: error,msg:{}", e.getMessage(), e);
        }
        return json;
    }

    /**
     * Json转JavaBean
     *
     * @param json
     * @param valueTypeRef
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String json, TypeReference<T> valueTypeRef) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T t = null;
        try {
            t = objectMapper.readValue(json, valueTypeRef);
        } catch (JsonProcessingException e) {
            logger.error("JacksonUtil.toBean: error,msg:{}", e.getMessage(), e);
        }
        return t;
    }

    /**
     * Json转JavaBean
     *
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T t = null;
        try {
            t = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("JacksonUtil.toBean: error,msg:{}", e.getMessage(), e);
        }
        return t;
    }

    /**
     * JsonNode转JavaBean
     *
     * @param jsonNode
     * @return
     */
    public static <T> T parseObject(JsonNode jsonNode, Class<T> clazz) {
        String json = jsonNode.toString();
        return parseObject(json, clazz);
    }

    /**
     * Json串转Map<String, Object>
     *
     * @param json
     * @return
     */
    public static Map<String, Object> toMap(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        Map<String, Object> map;
        try {
            map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            map = Collections.emptyMap();
            logger.error("JacksonUtil.toMap: error,msg:{}", e.getMessage(), e);
        }
        return map;
    }

    /**
     * Json转 List<Bean>
     *
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        List<T> t = null;
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            t = objectMapper.readValue(json, listType);
        } catch (JsonProcessingException e) {
            t = null;
            logger.error("JacksonUtil.toList: error,msg:{}", e.getMessage(), e);
        }
        return t;
    }

    /**
     * Json转 Bean[]
     *
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T[] toArray(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T[] t = null;
        try {
            t = objectMapper.readValue(json, new TypeReference<T[]>() {
            });
        } catch (JsonProcessingException e) {
            t = null;
            logger.error("JacksonUtil.toArray: error,msg:{}", e.getMessage(), e);
        }
        return t;
    }

    /**
     * Json转JsonNode
     *
     * @param json
     * @return
     */
    public static JsonNode toJsonNode(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.error("JacksonUtil.toJsonNode: error,msg:{}", e.getMessage(), e);
        }
        return jsonNode;
    }

    /**
     * Java Object转JsonNode
     *
     * @param o
     * @return
     */
    public static JsonNode toJsonNode(Object o) {
        JsonNode jsonNode = null;
        try {
            String jsonString = objectMapper.writeValueAsString(o);
            jsonNode = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            logger.error("JacksonUtil.toJsonNode: error,msg:{}", e.getMessage(), e);
        }
        return jsonNode;
    }

    public static List<BigDecimal[]> getCoordFromGeoJson(String geoJson) {
        List<BigDecimal[]> allPoint = new ArrayList<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(geoJson);
            String type = jsonNode.get("type").asText();
            if ("MultiPolygon".equalsIgnoreCase(type)) {
                String coords = jsonNode.get("coordinates").toString();
                JsonNode coordNode = objectMapper.readTree(coords);
                for (JsonNode coord : coordNode) {
                    for (JsonNode sigle : coord) {
                        String arrayStr = sigle.toString();
                        List<BigDecimal[]> points =
                                objectMapper.readValue(arrayStr, new TypeReference<List<BigDecimal[]>>() {
                                });
                        allPoint.addAll(points);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("JacksonUtil.getCoordFromGeoJson: error,msg:{}", e.getMessage(), e);
        }
        return allPoint;
    }

    public static String toJson(Object obj, String defaultValue) {
        return toJson(obj);
    }

    /**
     * javaBean、列表数组转换为json字符串,忽略空值
     */
    public static String tojsonIgnoreNull(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(obj);
    }

    /**
     * json 转JavaBean
     */

    public static <T> T json2pojo(String jsonString, Class<T> clazz) throws IOException {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return objectMapper.readValue(jsonString, clazz);
    }

    /**
     * json字符串转换为map
     */
    public static Map json2map(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue(jsonString, Map.class);
    }

    /**
     * json字符串转换为map
     */
    public static <T> Map<String, T> json2map(String jsonString, Class<T> clazz) throws IOException {
        Map<String, Object> map = objectMapper.readValue(jsonString, new TypeReference<Map>() {
        });
        Map<String, T> result = new HashMap<String, T>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), map2pojo((Map) entry.getValue(), clazz));
        }
        return result;
    }

    /**
     * 深度转换json成map
     *
     * @param json
     * @return
     */
    public static Map<String, Object> json2mapDeeply(String json) throws Exception {
        return json2MapRecursion(json, objectMapper);
    }

    /**
     * 把json解析成list，如果list内部的元素存在jsonString，继续解析
     *
     * @param json
     * @param mapper 解析工具
     * @return
     * @throws Exception
     */
    private static List<Object> json2ListRecursion(String json, ObjectMapper mapper) throws Exception {
        if (json == null) {
            return null;
        }

        List<Object> list = mapper.readValue(json, List.class);

        for (Object obj : list) {
            if (obj != null && obj instanceof String) {
                String str = (String) obj;
                if (str.startsWith("[")) {
                    obj = json2ListRecursion(str, mapper);
                } else if (obj.toString().startsWith("{")) {
                    obj = json2MapRecursion(str, mapper);
                }
            }
        }

        return list;
    }

    /**
     * 把json解析成map，如果map内部的value存在jsonString，继续解析
     *
     * @param json
     * @param mapper
     * @return
     * @throws Exception
     */
    private static Map<String, Object> json2MapRecursion(String json, ObjectMapper mapper) throws Exception {
        if (json == null) {
            return null;
        }

        Map<String, Object> map = mapper.readValue(json, Map.class);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object obj = entry.getValue();
            if (obj != null && obj instanceof String) {
                String str = ((String) obj);

                if (str.startsWith("[")) {
                    List<?> list = json2ListRecursion(str, mapper);
                    map.put(entry.getKey(), list);
                } else if (str.startsWith("{")) {
                    Map<String, Object> mapRecursion = json2MapRecursion(str, mapper);
                    map.put(entry.getKey(), mapRecursion);
                }
            }
        }

        return map;
    }

    /**
     * 与javaBean json数组字符串转换为列表
     */
    public static List<Map<String, String>> json2list(String jsonArrayStr) {
        try {
            return objectMapper.readValue(jsonArrayStr, new TypeReference<List<Map<String, String>>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取泛型的Collection Type
     *
     * @param collectionClass 泛型的Collection
     * @param elementClasses  元素类
     * @return JavaType Java类型
     * @since 1.0
     */
    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    /**
     * map 转JavaBean
     */
    public static <T> T map2pojo(Map map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    /**
     * map 转json
     *
     * @param map
     * @return
     */
    public static String mapToJson(Map map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * map 转JavaBean
     */
    public static <T> T obj2pojo(Object obj, Class<T> clazz) {
        return objectMapper.convertValue(obj, clazz);
    }
}
