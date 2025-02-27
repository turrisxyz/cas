package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.ticket.artifact.SamlArtifactTicket;
import org.apereo.cas.ticket.artifact.SamlArtifactTicketImpl;
import org.apereo.cas.ticket.query.SamlAttributeQueryTicket;
import org.apereo.cas.ticket.query.SamlAttributeQueryTicketImpl;
import org.apereo.cas.ticket.serialization.TicketSerializationExecutionPlanConfigurer;
import org.apereo.cas.util.serialization.AbstractJacksonBackedStringSerializer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link SamlIdPTicketSerializationConfiguration}.
 *
 * @author Bob Sandiford
 * @since 5.2.0
 */
@Configuration(value = "SamlIdpTicketSerializationConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.SAMLIdentityProvider)
public class SamlIdPTicketSerializationConfiguration {

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TicketSerializationExecutionPlanConfigurer samlIdPTicketSerializationExecutionPlanConfigurer() {
        return plan -> {
            plan.registerTicketSerializer(new SamlArtifactTicketStringSerializer());
            plan.registerTicketSerializer(new SamlAttributeQueryTicketStringSerializer());

            plan.registerTicketSerializer(SamlArtifactTicket.class.getName(), new SamlArtifactTicketStringSerializer());
            plan.registerTicketSerializer(SamlAttributeQueryTicket.class.getName(), new SamlAttributeQueryTicketStringSerializer());
        };
    }
    
    private static class SamlArtifactTicketStringSerializer extends AbstractJacksonBackedStringSerializer<SamlArtifactTicketImpl> {
        private static final long serialVersionUID = -2198623586274810263L;

        @Override
        public Class<SamlArtifactTicketImpl> getTypeToSerialize() {
            return SamlArtifactTicketImpl.class;
        }
    }

    private static class SamlAttributeQueryTicketStringSerializer extends AbstractJacksonBackedStringSerializer<SamlAttributeQueryTicketImpl> {
        private static final long serialVersionUID = -2198623586274810263L;

        @Override
        public Class<SamlAttributeQueryTicketImpl> getTypeToSerialize() {
            return SamlAttributeQueryTicketImpl.class;
        }
    }
}
