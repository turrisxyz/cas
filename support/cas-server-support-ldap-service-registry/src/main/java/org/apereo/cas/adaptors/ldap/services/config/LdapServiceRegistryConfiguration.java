package org.apereo.cas.adaptors.ldap.services.config;

import org.apereo.cas.adaptors.ldap.services.DefaultLdapRegisteredServiceMapper;
import org.apereo.cas.adaptors.ldap.services.LdapRegisteredServiceMapper;
import org.apereo.cas.adaptors.ldap.services.LdapServiceRegistry;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This is {@link LdapServiceRegistryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration(value = "LdapServiceRegistryConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.ServiceRegistry, module = "ldap")
public class LdapServiceRegistryConfiguration {
    private static final BeanCondition CONDITION = BeanCondition.on("cas.service-registry.ldap.ldap-url");

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "ldapServiceRegistryMapper")
    public LdapRegisteredServiceMapper ldapServiceRegistryMapper(
        final ConfigurableApplicationContext applicationContext,
        final CasConfigurationProperties casProperties) throws Exception {
        return BeanSupplier.of(LdapRegisteredServiceMapper.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> new DefaultLdapRegisteredServiceMapper(casProperties.getServiceRegistry().getLdap()))
            .otherwiseProxy()
            .get();
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "ldapServiceRegistry")
    public ServiceRegistry ldapServiceRegistry(
        @Qualifier("ldapServiceRegistryMapper")
        final LdapRegisteredServiceMapper ldapServiceRegistryMapper,
        final CasConfigurationProperties casProperties,
        final ConfigurableApplicationContext applicationContext,
        final ObjectProvider<List<ServiceRegistryListener>> serviceRegistryListeners) throws Exception {
        return BeanSupplier.of(ServiceRegistry.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> {
                val ldap = casProperties.getServiceRegistry().getLdap();
                val connectionFactory = LdapUtils.newLdaptiveConnectionFactory(ldap);
                LOGGER.debug("Configured LDAP service registry search filter to [{}] and load filter to [{}]",
                    ldap.getSearchFilter(), ldap.getLoadFilter());
                return new LdapServiceRegistry(connectionFactory, ldapServiceRegistryMapper,
                    ldap, applicationContext,
                    Optional.ofNullable(serviceRegistryListeners.getIfAvailable()).orElseGet(ArrayList::new));
            })
            .otherwiseProxy()
            .get();
    }

    @Bean
    @ConditionalOnMissingBean(name = "ldapServiceRegistryExecutionPlanConfigurer")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public ServiceRegistryExecutionPlanConfigurer ldapServiceRegistryExecutionPlanConfigurer(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("ldapServiceRegistry")
        final ServiceRegistry ldapServiceRegistry) throws Exception {
        return BeanSupplier.of(ServiceRegistryExecutionPlanConfigurer.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> plan -> plan.registerServiceRegistry(ldapServiceRegistry))
            .otherwiseProxy()
            .get();
    }
}
