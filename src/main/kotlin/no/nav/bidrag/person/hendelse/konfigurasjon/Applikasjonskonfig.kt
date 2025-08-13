package no.nav.bidrag.person.hendelse.konfigurasjon

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.commons.web.config.RestTemplateBuilderBean
import no.nav.bidrag.person.hendelse.konfigurasjon.Applikasjonskonfig.Companion.PROFIL_I_SKY
import no.nav.bidrag.person.hendelse.konfigurasjon.Applikasjonskonfig.Companion.PROFIL_LOKAL_POSTGRES
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@EnableRetry
@Configuration
@EnableSecurityConfiguration
@ConfigurationPropertiesScan
@ComponentScan("no.nav.bidrag.person.hendelse")
@EntityScan("no.nav.bidrag.person.hendelse.database")
@Import(RestTemplateBuilderBean::class, RestOperationsAzure::class)
class Applikasjonskonfig {
    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory = JettyServletWebServerFactory()

    companion object {
        const val PROFIL_I_SKY = "nais"
        const val PROFIL_LOKAL_POSTGRES = "lokal-postgres"
    }

    @Bean
    fun exceptionLogger(): ExceptionLogger? = ExceptionLogger(this::class.java.simpleName)

    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()
}

@Configuration
@Profile(PROFIL_I_SKY, PROFIL_LOKAL_POSTGRES)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
class SchedulerConfiguration {
    @Bean
    fun lockProvider(dataSource: DataSource): JdbcTemplateLockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration
                .builder()
                .withTableName("shedlock")
                .withColumnNames(JdbcTemplateLockProvider.ColumnNames("name", "lock_until", "locked_at", "locked_by"))
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .build(),
        )
}
