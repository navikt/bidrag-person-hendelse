package no.nav.bidrag.person.hendelse.konfigurasjon

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class Cachekonfig {
    @Bean
    fun cacheManager(): CacheManager =
        object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val concurrentMap =
                    Caffeine
                        .newBuilder()
                        .initialCapacity(100)
                        .maximumSize(1000)
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .recordStats()
                        .build<Any, Any>()
                        .asMap()
                return ConcurrentMapCache(name, concurrentMap, true)
            }
        }
}
