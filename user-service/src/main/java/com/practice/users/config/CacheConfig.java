package com.practice.users.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on @Cacheable / @CacheEvict. Kept OUT of UserServiceApplication on purpose:
 * test slices (@WebMvcTest, @DataJpaTest) always load the main class's annotations but skip
 * @Configuration classes like this one, and they have no CacheManager to go with @EnableCaching.
 *
 *   cache "posts"  : filled by PostService.getPost() (@Cacheable), emptied by UserService.updateUser()/deleteUser()
 *   settings       : spring.cache.* in application.properties (Caffeine, 500 entries, 60 s)
 *   test           : PostCachingTest
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
