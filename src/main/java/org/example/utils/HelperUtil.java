package org.example.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author 3590
 * @Date 2024/2/21 13:08
 * @Description 提供一些实用帮助工具
 */
@Slf4j
public class HelperUtil {
    private static final double CAPITAL_ENLARGE_FACTOR = 1.5;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()) // 手动写 LocalDateTime 的序列化和反序列化逻辑
            .setExclusionStrategies(new MyGsonStrategy()).create(); // 忽略某些字段

    private static class MyGsonStrategy implements ExclusionStrategy{
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(GsonIgnore.class) != null; // 如果某些属性上有 @GsonIgnore 注解
        }
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    /***
     * @param expireTime 待检测的时间
     * @param aliveTime 存活时间
     * @param timeUnit 时间单位
     * @return 与现在时间相比，判断是否逻辑过期
     */
    public static boolean isExpired(LocalDateTime expireTime, long aliveTime, ChronoUnit timeUnit) {
        long betweenTime = timeUnit.between(expireTime, LocalDateTime.now());// 从过去到现在的时间，按照时间先后排布
        return betweenTime > aliveTime;
    }


    /**
     * 判断当前时间是否在 开始时间 和 结束时间 之间
     */
    public static boolean timeInRange(LocalDateTime beginTime, LocalDateTime endTime) {
        return beginTime.isBefore(LocalDateTime.now()) && endTime.isAfter(LocalDateTime.now());
    }

    /***
     * 将实体类封装，添加 expire 字段，值是当前时刻
     * @param data 待包装的数据
     * @return RedisShopData，包装后的类型；如果 data == null，就 return null
     * @param <T> 数据的类型
     */
    public static <T> RedisWrapperData<T> dataWrapper(T data) {
        if (data == null) return null;
        RedisWrapperData<T> redisWrapperData = new RedisWrapperData<>();
        redisWrapperData.setData(data);
        redisWrapperData.setExpireTime(LocalDateTime.now());
        return redisWrapperData;
    }

    /***
     * bean 转为 json，调用定制化的 gson 对象，自动忽略 @GsonIgnore 字段，可以序列化 LocalDateTime
     * @param bean
     * @return
     * @param <T>
     */
    public static <T> String beanToJson(T bean) {
        return gson.toJson(bean); // 序列化时忽略加上 @GsonIgnore 的字段
    }

    public static <T> T jsonToBean(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * 给集合类进行 bean 和 json(String) 之间的转换
     * @param <E> javaBean 的类型
     * @param collection 集合类，类型是 Collection<String>
     * @param clazz bean 的类型对象
     * @return Collection<E>，即返回集合类，它的元素从 序列化的 string 转为了 bean
     */
    public static <E> Collection<E> collectionElemToBean(Collection<String> collection, Class<E> clazz)  {
        Class<? extends Collection> collectionClass = collection.getClass(); // collection<Object>
        Collection<E> newCollection = null;
        try {
            Constructor<?> constructor = collectionClass.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            newCollection = (Collection<E>) constructor.newInstance((int) Math.round(collection.size() * CAPITAL_ENLARGE_FACTOR));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            log.debug(e.getMessage());
        }
        for (String json : collection) {
            newCollection.add(jsonToBean(json, clazz));
        }
        return newCollection;
    }

    /**
     * 给集合类进行 bean 和 json(String) 之间的转换
     * @param <E> javaBean 的类型
     * @param collection 集合类，类型是 Collection<E>
     * @return Collection<String>，同时会保留 Collection 的具体实现类
     */
    public static <E> Collection<String> collectionElemToJson(Collection<E> collection) {
        Class<? extends Collection> collectionClass = collection.getClass(); // collection<Object>
        Collection<String> newCollection = null;
        try {
            Constructor<?> constructor = collectionClass.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            newCollection = (Collection<String>) constructor.newInstance((int) Math.round(collection.size() * CAPITAL_ENLARGE_FACTOR));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            log.debug(e.getMessage());
        }
        for (E elem : collection) {
            newCollection.add(beanToJson(elem));
        }
        return newCollection;
    }

    /**
     * 作用是通过反射，把每个字段名称及其String存入Map
     * @return bean 生成的键值对，key 是属性名称字符串，value 是属性值字符串；即使 object 是 null，也可以返回一个空map
     * */
    public static <T> Map<String, String> beanToStringMap(T object) {
        if (object == null) return new HashMap<>();
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        Map<String, String> map = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(object);
                map.put(field.getName(), value == null ? null : value.toString());
            } catch (IllegalAccessException e) {
                log.debug(e.getMessage());
            }
        }
        return map;
    }

}
