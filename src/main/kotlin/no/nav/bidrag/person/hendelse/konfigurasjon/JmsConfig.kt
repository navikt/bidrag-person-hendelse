package no.nav.bidrag.person.hendelse.konfigurasjon

import com.ibm.msg.client.jms.JmsConnectionFactory
import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.core.JmsTemplate
import javax.jms.JMSException

@EnableJms
@Configuration
open class JmsConfig(var wmq: Wmq) {

    @Throws(JMSException::class)
    fun forbindelsefabrikk(): JmsConnectionFactory? {

        val fabrikkfabrikk = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER)
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

    @Bean
    open fun jmsTemplate(): JmsTemplate {
        return JmsTemplate(forbindelsefabrikk())
    }
}