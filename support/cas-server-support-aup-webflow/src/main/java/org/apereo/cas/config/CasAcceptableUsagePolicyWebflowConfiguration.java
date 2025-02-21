package org.apereo.cas.config;

import org.apereo.cas.audit.AuditActionResolvers;
import org.apereo.cas.audit.AuditResourceResolvers;
import org.apereo.cas.audit.AuditTrailConstants;
import org.apereo.cas.audit.AuditTrailRecordResolutionPlanConfigurer;
import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.aup.AcceptableUsagePolicyRepository;
import org.apereo.cas.aup.DefaultAcceptableUsagePolicyRepository;
import org.apereo.cas.aup.GroovyAcceptableUsagePolicyRepository;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.scripting.WatchableGroovyScriptResource;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;
import org.apereo.cas.web.flow.AcceptableUsagePolicySubmitAction;
import org.apereo.cas.web.flow.AcceptableUsagePolicyVerifyAction;
import org.apereo.cas.web.flow.AcceptableUsagePolicyVerifyServiceAction;
import org.apereo.cas.web.flow.AcceptableUsagePolicyWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.actions.ConsumerExecutionAction;
import org.apereo.cas.web.support.WebUtils;

import lombok.val;
import org.apereo.inspektr.audit.spi.AuditResourceResolver;
import org.apereo.inspektr.audit.spi.support.DefaultAuditActionResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link CasAcceptableUsagePolicyWebflowConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Configuration(value = "CasAcceptableUsagePolicyWebflowConfiguration", proxyBeanMethods = false)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.AcceptableUsagePolicy)
public class CasAcceptableUsagePolicyWebflowConfiguration {

    @Configuration(value = "CasAcceptableUsagePolicyWebflowCoreConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasAcceptableUsagePolicyWebflowCoreConfiguration {

        @ConditionalOnMissingBean(name = "acceptableUsagePolicyWebflowConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public CasWebflowConfigurer acceptableUsagePolicyWebflowConfigurer(
            final CasConfigurationProperties casProperties, final ConfigurableApplicationContext applicationContext,
            @Qualifier(CasWebflowConstants.BEAN_NAME_LOGIN_FLOW_DEFINITION_REGISTRY)
            final FlowDefinitionRegistry loginFlowDefinitionRegistry,
            @Qualifier(CasWebflowConstants.BEAN_NAME_FLOW_BUILDER_SERVICES)
            final FlowBuilderServices flowBuilderServices) {
            return BeanSupplier.of(CasWebflowConfigurer.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() -> new AcceptableUsagePolicyWebflowConfigurer(flowBuilderServices,
                    loginFlowDefinitionRegistry, applicationContext, casProperties))
                .otherwiseProxy()
                .get();
        }
    }

    @Configuration(value = "CasAcceptableUsagePolicyWebflowRepositoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasAcceptableUsagePolicyWebflowRepositoryConfiguration {
        @ConditionalOnMissingBean(name = AcceptableUsagePolicyRepository.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AcceptableUsagePolicyRepository acceptableUsagePolicyRepository(
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(TicketRegistrySupport.BEAN_NAME)
            final TicketRegistrySupport ticketRegistrySupport) throws Exception {
            return BeanSupplier.of(AcceptableUsagePolicyRepository.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() -> {
                    val groovy = casProperties.getAcceptableUsagePolicy().getGroovy();
                    if (groovy.getLocation() != null) {
                        return new GroovyAcceptableUsagePolicyRepository(ticketRegistrySupport, casProperties.getAcceptableUsagePolicy(),
                            new WatchableGroovyScriptResource(groovy.getLocation()), applicationContext);
                    }
                    return new DefaultAcceptableUsagePolicyRepository(ticketRegistrySupport, casProperties.getAcceptableUsagePolicy());
                })
                .otherwise(AcceptableUsagePolicyRepository::noOp)
                .get();
        }
    }

    @Configuration(value = "CasAcceptableUsagePolicyWebflowPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasAcceptableUsagePolicyWebflowPlanConfiguration {
        @ConditionalOnMissingBean(name = "casAcceptableUsagePolicyWebflowExecutionPlanConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public CasWebflowExecutionPlanConfigurer casAcceptableUsagePolicyWebflowExecutionPlanConfigurer(
            @Qualifier("acceptableUsagePolicyWebflowConfigurer")
            final CasWebflowConfigurer acceptableUsagePolicyWebflowConfigurer) {
            return plan -> plan.registerWebflowConfigurer(acceptableUsagePolicyWebflowConfigurer);
        }
    }

    @Configuration(value = "CasAcceptableUsagePolicyWebflowActionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasAcceptableUsagePolicyWebflowActionConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "acceptableUsagePolicySubmitAction")
        public Action acceptableUsagePolicySubmitAction(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(AcceptableUsagePolicyRepository.BEAN_NAME)
            final AcceptableUsagePolicyRepository acceptableUsagePolicyRepository) throws Exception {
            return BeanSupplier.of(Action.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() -> new AcceptableUsagePolicySubmitAction(acceptableUsagePolicyRepository))
                .otherwise(() -> ConsumerExecutionAction.NONE)
                .get();
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "acceptableUsagePolicyVerifyAction")
        public Action acceptableUsagePolicyVerifyAction(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(AcceptableUsagePolicyRepository.BEAN_NAME)
            final AcceptableUsagePolicyRepository acceptableUsagePolicyRepository,
            @Qualifier("registeredServiceAccessStrategyEnforcer")
            final AuditableExecution registeredServiceAccessStrategyEnforcer) throws Exception {
            return BeanSupplier.of(Action.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() -> new AcceptableUsagePolicyVerifyAction(acceptableUsagePolicyRepository, registeredServiceAccessStrategyEnforcer))
                .otherwise(() -> ConsumerExecutionAction.NONE)
                .get();
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "acceptableUsagePolicyRenderAction")
        public Action acceptableUsagePolicyRenderAction(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(AcceptableUsagePolicyRepository.BEAN_NAME)
            final AcceptableUsagePolicyRepository acceptableUsagePolicyRepository) throws Exception {
            return BeanSupplier.of(Action.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() -> new ConsumerExecutionAction(requestContext -> acceptableUsagePolicyRepository.fetchPolicy(requestContext)
                    .ifPresent(policy -> WebUtils.putAcceptableUsagePolicyTermsIntoFlowScope(requestContext, policy))))
                .otherwise(() -> ConsumerExecutionAction.NONE)
                .get();
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "acceptableUsagePolicyVerifyServiceAction")
        public Action acceptableUsagePolicyVerifyServiceAction(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(AcceptableUsagePolicyRepository.BEAN_NAME)
            final AcceptableUsagePolicyRepository acceptableUsagePolicyRepository,
            @Qualifier("registeredServiceAccessStrategyEnforcer")
            final AuditableExecution registeredServiceAccessStrategyEnforcer) throws Exception {
            return BeanSupplier.of(Action.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() -> new AcceptableUsagePolicyVerifyServiceAction(acceptableUsagePolicyRepository, registeredServiceAccessStrategyEnforcer))
                .otherwise(() -> ConsumerExecutionAction.NONE)
                .get();
        }
    }

    @Configuration(value = "CasAcceptableUsagePolicyWebflowAuditConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasAcceptableUsagePolicyWebflowAuditConfiguration {
        @ConditionalOnMissingBean(name = "casAcceptableUsagePolicyAuditTrailRecordResolutionPlanConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuditTrailRecordResolutionPlanConfigurer casAcceptableUsagePolicyAuditTrailRecordResolutionPlanConfigurer(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier("nullableReturnValueResourceResolver")
            final AuditResourceResolver resourceResolver) throws Exception {

            return BeanSupplier.of(AuditTrailRecordResolutionPlanConfigurer.class)
                .when(AcceptableUsagePolicyRepository.CONDITION_AUP_ENABLED.given(applicationContext.getEnvironment()))
                .supply(() ->
                    plan -> {
                        plan.registerAuditResourceResolver(resourceResolver,
                            AuditResourceResolvers.AUP_SUBMIT_RESOURCE_RESOLVER,
                            AuditResourceResolvers.AUP_VERIFY_RESOURCE_RESOLVER);
                        val resolver = new DefaultAuditActionResolver(AuditTrailConstants.AUDIT_ACTION_POSTFIX_TRIGGERED);
                        plan.registerAuditActionResolvers(resolver,
                            AuditActionResolvers.AUP_VERIFY_ACTION_RESOLVER,
                            AuditActionResolvers.AUP_SUBMIT_ACTION_RESOLVER);
                    })
                .otherwiseProxy()
                .get();
        }
    }
}
