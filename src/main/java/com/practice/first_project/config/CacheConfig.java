package com.practice.first_project.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on @Cacheable / @CacheEvict. Kept OUT of FirstProjectApplication on purpose:
 * test slices (@WebMvcTest, @DataJpaTest) always load the main class's annotations but skip
 * @Configuration classes like this one, and they have no CacheManager to go with @EnableCaching.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
