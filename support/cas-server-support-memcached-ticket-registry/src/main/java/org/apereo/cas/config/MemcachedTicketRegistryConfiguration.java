package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.memcached.MemcachedPooledClientConnectionFactory;
import org.apereo.cas.memcached.MemcachedUtils;
import org.apereo.cas.ticket.registry.MemcachedTicketRegistry;
import org.apereo.cas.ticket.registry.NoOpTicketRegistryCleaner;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistryCleaner;
import org.apereo.cas.util.CoreTicketUtils;
import org.apereo.cas.util.serialization.ComponentSerializationPlan;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import lombok.val;
import net.spy.memcached.transcoders.Transcoder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link MemcachedTicketRegistryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Configuration(value = "MemcachedConfiguration", proxyBeanMethods = false)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.TicketRegistry, module = "memcached")
public class MemcachedTicketRegistryConfiguration {

    @ConditionalOnMissingBean(name = "memcachedTicketRegistryTranscoder")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public Transcoder memcachedTicketRegistryTranscoder(
        final CasConfigurationProperties casProperties,
        @Qualifier("componentSerializationPlan")
        final ComponentSerializationPlan componentSerializationPlan) {
        val memcached = casProperties.getTicket()
            .getRegistry()
            .getMemcached();
        return MemcachedUtils.newTranscoder(memcached, componentSerializationPlan.getRegisteredClasses());
    }

    @ConditionalOnMissingBean(name = "memcachedPooledClientConnectionFactory")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public MemcachedPooledClientConnectionFactory memcachedPooledClientConnectionFactory(
        final CasConfigurationProperties casProperties,
        @Qualifier("memcachedTicketRegistryTranscoder")
        final Transcoder memcachedTicketRegistryTranscoder) {
        val memcached = casProperties.getTicket()
            .getRegistry()
            .getMemcached();
        return new MemcachedPooledClientConnectionFactory(memcached, memcachedTicketRegistryTranscoder);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TicketRegistry ticketRegistry(final CasConfigurationProperties casProperties,
                                         @Qualifier("memcachedTicketRegistryTranscoder")
                                         final Transcoder memcachedTicketRegistryTranscoder) {
        val memcached = casProperties.getTicket()
            .getRegistry()
            .getMemcached();
        val factory = new MemcachedPooledClientConnectionFactory(memcached, memcachedTicketRegistryTranscoder);
        val registry = new MemcachedTicketRegistry(factory.getObjectPool());
        val cipherExecutor = CoreTicketUtils.newTicketRegistryCipherExecutor(memcached.getCrypto(), "memcached");
        registry.setCipherExecutor(cipherExecutor);
        return registry;
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TicketRegistryCleaner ticketRegistryCleaner() {
        return NoOpTicketRegistryCleaner.getInstance();
    }
}
