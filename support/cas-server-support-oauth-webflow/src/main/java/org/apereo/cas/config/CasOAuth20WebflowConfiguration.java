package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationServiceSelectionStrategy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.web.flow.OAuth20RegisteredServiceUIAction;
import org.apereo.cas.support.oauth.web.flow.OAuth20WebflowConfigurer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;
import org.apereo.cas.validation.CasProtocolViewFactory;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.login.SessionStoreTicketGrantingTicketAction;

import lombok.val;
import org.pac4j.core.context.session.SessionStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.servlet.View;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link CasOAuth20WebflowConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration(value = "CasOAuth20WebflowConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.OAuth)
public class CasOAuth20WebflowConfiguration {

    @Configuration(value = "CasOAuth20WebflowActionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasOAuth20WebflowActionConfiguration {
        @ConditionalOnMissingBean(name = "oauth20RegisteredServiceUIAction")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action oauth20RegisteredServiceUIAction(
            @Qualifier("oauth20AuthenticationRequestServiceSelectionStrategy")
            final AuthenticationServiceSelectionStrategy oauth20AuthenticationServiceSelectionStrategy,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager) {
            return new OAuth20RegisteredServiceUIAction(servicesManager, oauth20AuthenticationServiceSelectionStrategy);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "oauth20SessionStoreTicketGrantingTicketAction")
        public Action oauth20SessionStoreTicketGrantingTicketAction(
            @Qualifier("oauthDistributedSessionStore")
            final SessionStore oauthDistributedSessionStore) {
            return new SessionStoreTicketGrantingTicketAction(oauthDistributedSessionStore);
        }
    }

    @Configuration(value = "CasOAuth20ViewsConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasOAuth20ViewsConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public View oauthConfirmView(
            @Qualifier("casProtocolViewFactory")
            final CasProtocolViewFactory casProtocolViewFactory,
            final ConfigurableApplicationContext applicationContext) {
            return casProtocolViewFactory.create(applicationContext, "protocol/oauth/confirm");
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public View oauthDeviceCodeApprovalView(
            @Qualifier("casProtocolViewFactory")
            final CasProtocolViewFactory casProtocolViewFactory,
            final ConfigurableApplicationContext applicationContext) {
            return casProtocolViewFactory.create(applicationContext, "protocol/oauth/deviceCodeApproval");
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public View oauthDeviceCodeApprovedView(
            @Qualifier("casProtocolViewFactory")
            final CasProtocolViewFactory casProtocolViewFactory,
            final ConfigurableApplicationContext applicationContext) {
            return casProtocolViewFactory.create(applicationContext, "protocol/oauth/deviceCodeApproved");
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public View oauthSessionStaleMismatchErrorView(
            @Qualifier("casProtocolViewFactory")
            final CasProtocolViewFactory casProtocolViewFactory,
            final ConfigurableApplicationContext applicationContext) {
            return casProtocolViewFactory.create(applicationContext, "protocol/oauth/sessionStaleMismatchError");
        }
    }

    @Configuration(value = "CasOAuth20WebflowLogoutConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasOAuth20WebflowLogoutConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "oauth20CasLogoutWebflowExecutionPlanConfigurer")
        public CasWebflowExecutionPlanConfigurer oauth20CasLogoutWebflowExecutionPlanConfigurer(
            @Qualifier("oauth20LogoutWebflowConfigurer")
            final CasWebflowConfigurer oauth20LogoutWebflowConfigurer) {
            return plan -> plan.registerWebflowConfigurer(oauth20LogoutWebflowConfigurer);
        }

        @ConditionalOnMissingBean(name = "oauth20LogoutWebflowConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public CasWebflowConfigurer oauth20LogoutWebflowConfigurer(
            @Qualifier(CasWebflowConstants.BEAN_NAME_LOGOUT_FLOW_DEFINITION_REGISTRY)
            final FlowDefinitionRegistry logoutFlowDefinitionRegistry,
            @Qualifier(CasWebflowConstants.BEAN_NAME_FLOW_BUILDER_SERVICES)
            final FlowBuilderServices flowBuilderServices,
            @Qualifier(CasWebflowConstants.BEAN_NAME_LOGIN_FLOW_DEFINITION_REGISTRY)
            final FlowDefinitionRegistry loginFlowDefinitionRegistry,
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext) {
            val c = new OAuth20WebflowConfigurer(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties);
            c.setLogoutFlowDefinitionRegistry(logoutFlowDefinitionRegistry);
            return c;
        }
    }
}
