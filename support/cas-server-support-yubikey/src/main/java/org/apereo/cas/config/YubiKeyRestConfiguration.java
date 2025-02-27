package org.apereo.cas.config;

import org.apereo.cas.adaptors.yubikey.YubiKeyRestHttpRequestCredentialFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.rest.plan.RestHttpRequestCredentialFactoryConfigurer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link YubiKeyRestConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Configuration(value = "YubiKeyRestConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.YubiKey)
@ConditionalOnClass(value = RestHttpRequestCredentialFactoryConfigurer.class)
public class YubiKeyRestConfiguration {

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "googleAuthenticatorRestHttpRequestCredentialFactoryConfigurer")
    public RestHttpRequestCredentialFactoryConfigurer googleAuthenticatorRestHttpRequestCredentialFactoryConfigurer() {
        return factory -> factory.registerCredentialFactory(new YubiKeyRestHttpRequestCredentialFactory());
    }
}
