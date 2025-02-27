package org.apereo.cas.config;

import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.audit.CouchbaseAuditTrailManager;
import org.apereo.cas.audit.spi.AuditActionContextJsonSerializer;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.couchbase.core.CouchbaseClientFactory;
import org.apereo.cas.couchbase.core.DefaultCouchbaseClientFactory;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import lombok.val;
import org.apereo.inspektr.audit.AuditTrailManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CasSupportCouchbaseAuditConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Configuration(value = "CasSupportCouchbaseAuditConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.Audit, module = "couchbase")
public class CasSupportCouchbaseAuditConfiguration {

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "auditsCouchbaseClientFactory")
    public CouchbaseClientFactory auditsCouchbaseClientFactory(final CasConfigurationProperties casProperties) {
        val cb = casProperties.getAudit().getCouchbase();
        return new DefaultCouchbaseClientFactory(cb);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "couchbaseAuditTrailManager")
    public AuditTrailManager couchbaseAuditTrailManager(
        @Qualifier("auditsCouchbaseClientFactory")
        final CouchbaseClientFactory auditsCouchbaseClientFactory,
        final CasConfigurationProperties casProperties) {
        val cb = casProperties.getAudit().getCouchbase();
        return new CouchbaseAuditTrailManager(auditsCouchbaseClientFactory,
            new AuditActionContextJsonSerializer(), cb.isAsynchronous());
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "couchbaseAuditTrailExecutionPlanConfigurer")
    public AuditTrailExecutionPlanConfigurer couchbaseAuditTrailExecutionPlanConfigurer(
        @Qualifier("couchbaseAuditTrailManager")
        final AuditTrailManager couchbaseAuditTrailManager) {
        return plan -> plan.registerAuditTrailManager(couchbaseAuditTrailManager);
    }
}
