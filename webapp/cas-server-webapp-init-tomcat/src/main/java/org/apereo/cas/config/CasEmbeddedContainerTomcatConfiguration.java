package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.tomcat.CasTomcatServletWebServerFactory;
import org.apereo.cas.tomcat.CasTomcatServletWebServerFactoryCustomizer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * This is {@link CasEmbeddedContainerTomcatConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration(value = "CasEmbeddedContainerTomcatConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnClass(value = {Tomcat.class, Http2Protocol.class})
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.ApacheTomcat)
public class CasEmbeddedContainerTomcatConfiguration {

    @ConditionalOnMissingBean(name = "casServletWebServerFactory")
    @Bean
    public ConfigurableServletWebServerFactory casServletWebServerFactory(
        final ServerProperties serverProperties,
        final CasConfigurationProperties casProperties) {
        return new CasTomcatServletWebServerFactory(casProperties, serverProperties);
    }

    @ConditionalOnMissingBean(name = "casTomcatEmbeddedServletContainerCustomizer")
    @Bean
    public ServletWebServerFactoryCustomizer casTomcatEmbeddedServletContainerCustomizer(
        final ServerProperties serverProperties,
        final CasConfigurationProperties casProperties) {
        return new CasTomcatServletWebServerFactoryCustomizer(serverProperties, casProperties);
    }
}
