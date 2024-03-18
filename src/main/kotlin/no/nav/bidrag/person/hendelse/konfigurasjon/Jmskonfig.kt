package no.nav.bidrag.person.hendelse.konfigurasjon

import com.ibm.msg.client.jakarta.jms.JmsConnectionFactory
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import jakarta.jms.JMSException
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.connection.CachingConnectionFactory
import org.springframework.jms.core.JmsTemplate

@EnableJms
@Configuration
class Jmskonfig(var wmq: Wmq) {
    fun createCachingConnectionFactory(): CachingConnectionFactory {
        val cachingConnectionFactory = CachingConnectionFactory()
        cachingConnectionFactory.sessionCacheSize = 1
        cachingConnectionFactory.targetConnectionFactory = forbindelsefabrikk()
        return cachingConnectionFactory
    }

    @Bean
    fun jmsTemplate(): JmsTemplate {
        return JmsTemplate(createCachingConnectionFactory())
    }

    @Throws(JMSException::class)
    fun forbindelsefabrikk(): JmsConnectionFactory {
        val fabrikkfabrikk = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER)
        val forbindelsefabrikk = fabrikkfabrikk.createConnectionFactory()

        forbindelsefabrikk.setStringProperty(WMQConstants.WMQ_HOST_NAME, wmq.host)
        forbindelsefabrikk.setIntProperty(WMQConstants.WMQ_PORT, wmq.port)
        forbindelsefabrikk.setStringProperty(WMQConstants.WMQ_CHANNEL, wmq.channel)
        forbindelsefabrikk.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
        forbindelsefabrikk.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, wmq.queueManager)
        forbindelsefabrikk.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, wmq.applicationName)
        forbindelsefabrikk.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
        forbindelsefabrikk.setStringProperty(WMQConstants.USERID, wmq.username)
        forbindelsefabrikk.setStringProperty(WMQConstants.PASSWORD, wmq.password)

        return forbindelsefabrikk
    }
}
