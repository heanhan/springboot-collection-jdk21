package com.example.ai.langchain4j.constant;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 13:30
 * @Description:
 */
public class RedisConstants {

    public static final class CacheName{
        /**
         * 聊天缓存
         */
        public static final String CHAT_CACHE = "CHAT_CACHE";

    }


    public static final class CacheManager {
        /**
         * 缓存时间永久
         */
        public static final String FOREVER = "cacheManagerForever";

        /**
         * 缓存时间30Minutes
         */
        public static final String THIRTY_MINUTES = "cacheManager30Minutes";

        /**
         * 缓存时间1天
         */
        public static final String ONE_DAY = "cacheManagerOneDay";
    }

    public static final class Ttl {
        /**
         * 缓存时间30分钟
         */
        public static final int THIRTY = 30;
    }

}
