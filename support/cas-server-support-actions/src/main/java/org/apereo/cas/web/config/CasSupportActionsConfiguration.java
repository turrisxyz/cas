package org.apereo.cas.web.config;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.PrincipalElectionStrategy;
import org.apereo.cas.authentication.adaptive.AdaptiveAuthenticationPolicy;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.logout.LogoutExecutionPlan;
import org.apereo.cas.logout.LogoutManager;
import org.apereo.cas.logout.slo.SingleLogoutRequestExecutor;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;
import org.apereo.cas.web.FlowExecutionExceptionResolver;
import org.apereo.cas.web.cookie.CasCookieBuilder;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.GatewayServicesManagementCheckAction;
import org.apereo.cas.web.flow.GenerateServiceTicketAction;
import org.apereo.cas.web.flow.PopulateSpringSecurityContextAction;
import org.apereo.cas.web.flow.ServiceAuthorizationCheckAction;
import org.apereo.cas.web.flow.SingleSignOnParticipationStrategy;
import org.apereo.cas.web.flow.account.PrepareAccountProfileViewAction;
import org.apereo.cas.web.flow.actions.FetchTicketGrantingTicketAction;
import org.apereo.cas.web.flow.actions.InitialAuthenticationAction;
import org.apereo.cas.web.flow.login.CreateTicketGrantingTicketAction;
import org.apereo.cas.web.flow.login.GenericSuccessViewAction;
import org.apereo.cas.web.flow.login.InitialAuthenticationRequestValidationAction;
import org.apereo.cas.web.flow.login.InitialFlowSetupAction;
import org.apereo.cas.web.flow.login.InitializeLoginAction;
import org.apereo.cas.web.flow.login.RedirectUnauthorizedServiceUrlAction;
import org.apereo.cas.web.flow.login.RenderLoginAction;
import org.apereo.cas.web.flow.login.SendTicketGrantingTicketAction;
import org.apereo.cas.web.flow.login.ServiceWarningAction;
import org.apereo.cas.web.flow.login.SetServiceUnauthorizedRedirectUrlAction;
import org.apereo.cas.web.flow.login.TicketGrantingTicketCheckAction;
import org.apereo.cas.web.flow.login.VerifyRequiredServiceAction;
import org.apereo.cas.web.flow.logout.ConfirmLogoutAction;
import org.apereo.cas.web.flow.logout.FinishLogoutAction;
import org.apereo.cas.web.flow.logout.FrontChannelLogoutAction;
import org.apereo.cas.web.flow.logout.LogoutAction;
import org.apereo.cas.web.flow.logout.LogoutViewSetupAction;
import org.apereo.cas.web.flow.logout.TerminateSessionAction;
import org.apereo.cas.web.flow.resolver.CasDelegatingWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.CasWebflowEventResolutionConfigurationContext;
import org.apereo.cas.web.support.ArgumentExtractor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link CasSupportActionsConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration(value = "CasSupportActionsConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableTransactionManagement(proxyTargetClass = false)
public class CasSupportActionsConfiguration {

    @Configuration(value = "CasSupportActionsExceptionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasSupportActionsExceptionConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public HandlerExceptionResolver errorHandlerResolver() {
            return new FlowExecutionExceptionResolver();
        }
    }

    @Configuration(value = "CasSupportActionsExecutionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasSupportActionsExecutionConfiguration {

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_FETCH_TICKET_GRANTING_TICKET)
        public Action fetchTicketGrantingTicketAction(
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService) {
            return new FetchTicketGrantingTicketAction(centralAuthenticationService, ticketGrantingTicketCookieGenerator);
        }

        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_AUTHENTICATION_VIA_FORM_ACTION)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action authenticationViaFormAction(
            @Qualifier("serviceTicketRequestWebflowEventResolver")
            final CasWebflowEventResolver serviceTicketRequestWebflowEventResolver,
            @Qualifier("initialAuthenticationAttemptWebflowEventResolver")
            final CasDelegatingWebflowEventResolver initialAuthenticationAttemptWebflowEventResolver,
            @Qualifier("adaptiveAuthenticationPolicy")
            final AdaptiveAuthenticationPolicy adaptiveAuthenticationPolicy) {
            return new InitialAuthenticationAction(initialAuthenticationAttemptWebflowEventResolver, serviceTicketRequestWebflowEventResolver, adaptiveAuthenticationPolicy);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_SERVICE_AUTHZ_CHECK)
        @Bean
        public Action serviceAuthorizationCheck(
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(AuthenticationServiceSelectionPlan.BEAN_NAME)
            final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies) {
            return new ServiceAuthorizationCheckAction(servicesManager, authenticationRequestServiceSelectionStrategies);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_SEND_TICKET_GRANTING_TICKET)
        @Bean
        public Action sendTicketGrantingTicketAction(
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier("singleSignOnParticipationStrategy")
            final SingleSignOnParticipationStrategy webflowSingleSignOnParticipationStrategy) {
            return new SendTicketGrantingTicketAction(centralAuthenticationService,
                ticketGrantingTicketCookieGenerator, webflowSingleSignOnParticipationStrategy);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_CREATE_TICKET_GRANTING_TICKET)
        @Bean
        public Action createTicketGrantingTicketAction(
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new CreateTicketGrantingTicketAction(casWebflowConfigurationContext);
        }

        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_FINISH_LOGOUT)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action finishLogoutAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor,
            @Qualifier("logoutExecutionPlan")
            final LogoutExecutionPlan logoutExecutionPlan) {
            return new FinishLogoutAction(centralAuthenticationService, ticketGrantingTicketCookieGenerator,
                argumentExtractor, servicesManager, logoutExecutionPlan, casProperties);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_LOGOUT)
        public Action logoutAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor,
            @Qualifier("logoutExecutionPlan")
            final LogoutExecutionPlan logoutExecutionPlan) {
            return new LogoutAction(centralAuthenticationService, ticketGrantingTicketCookieGenerator,
                argumentExtractor, servicesManager, logoutExecutionPlan, casProperties);
        }

        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_INIT_LOGIN_ACTION)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action initializeLoginAction(final CasConfigurationProperties casProperties,
                                            @Qualifier(ServicesManager.BEAN_NAME)
                                            final ServicesManager servicesManager) {
            return new InitializeLoginAction(servicesManager, casProperties);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "setServiceUnauthorizedRedirectUrlAction")
        @Bean
        public Action setServiceUnauthorizedRedirectUrlAction(
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager) {
            return new SetServiceUnauthorizedRedirectUrlAction(servicesManager);
        }

        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_RENDER_LOGIN_FORM)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action renderLoginFormAction(
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager) {
            return new RenderLoginAction(servicesManager, casProperties, applicationContext);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_INITIAL_FLOW_SETUP)
        public Action initialFlowSetupAction(
            final CasConfigurationProperties casProperties,
            @Qualifier("authenticationEventExecutionPlan")
            final AuthenticationEventExecutionPlan authenticationEventExecutionPlan,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier("warnCookieGenerator")
            final CasCookieBuilder warnCookieGenerator,
            @Qualifier(TicketRegistrySupport.BEAN_NAME)
            final TicketRegistrySupport ticketRegistrySupport,
            @Qualifier(AuthenticationServiceSelectionPlan.BEAN_NAME)
            final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies,
            @Qualifier("singleSignOnParticipationStrategy")
            final SingleSignOnParticipationStrategy webflowSingleSignOnParticipationStrategy,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor) {
            return new InitialFlowSetupAction(CollectionUtils.wrap(argumentExtractor), servicesManager,
                authenticationRequestServiceSelectionStrategies, ticketGrantingTicketCookieGenerator,
                warnCookieGenerator, casProperties, authenticationEventExecutionPlan,
                webflowSingleSignOnParticipationStrategy, ticketRegistrySupport);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_VERIFY_REQUIRED_SERVICE)
        public Action verifyRequiredServiceAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(TicketRegistrySupport.BEAN_NAME)
            final TicketRegistrySupport ticketRegistrySupport) {
            return new VerifyRequiredServiceAction(servicesManager,
                ticketGrantingTicketCookieGenerator, casProperties, ticketRegistrySupport);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_INITIAL_AUTHN_REQUEST_VALIDATION)
        public Action initialAuthenticationRequestValidationAction(
            @Qualifier("rankedAuthenticationProviderWebflowEventResolver")
            final CasWebflowEventResolver rankedAuthenticationProviderWebflowEventResolver) {
            return new InitialAuthenticationRequestValidationAction(rankedAuthenticationProviderWebflowEventResolver);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = "genericSuccessViewAction")
        public Action genericSuccessViewAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(WebApplicationService.BEAN_NAME_FACTORY)
            final ServiceFactory<WebApplicationService> webApplicationServiceFactory,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService) {
            return new GenericSuccessViewAction(centralAuthenticationService, servicesManager,
                webApplicationServiceFactory, casProperties);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = "redirectUnauthorizedServiceUrlAction")
        public Action redirectUnauthorizedServiceUrlAction(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager) {
            return new RedirectUnauthorizedServiceUrlAction(servicesManager, applicationContext);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_GENERATE_SERVICE_TICKET)
        public Action generateServiceTicketAction(
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(AuthenticationSystemSupport.BEAN_NAME)
            final AuthenticationSystemSupport authenticationSystemSupport,
            @Qualifier(TicketRegistrySupport.BEAN_NAME)
            final TicketRegistrySupport ticketRegistrySupport,
            @Qualifier(AuthenticationServiceSelectionPlan.BEAN_NAME)
            final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies,
            @Qualifier(PrincipalElectionStrategy.BEAN_NAME)
            final PrincipalElectionStrategy principalElectionStrategy) {
            return new GenerateServiceTicketAction(authenticationSystemSupport, centralAuthenticationService,
                ticketRegistrySupport, authenticationRequestServiceSelectionStrategies,
                servicesManager, principalElectionStrategy);
        }

        @Bean
        @ConditionalOnMissingBean(name = "gatewayServicesManagementCheck")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action gatewayServicesManagementCheck(
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(AuthenticationServiceSelectionPlan.BEAN_NAME)
            final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies) {
            return new GatewayServicesManagementCheckAction(servicesManager, authenticationRequestServiceSelectionStrategies);
        }

        @Bean
        @ConditionalOnMissingBean(name = "frontChannelLogoutAction")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action frontChannelLogoutAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor,
            @Qualifier("logoutExecutionPlan")
            final LogoutExecutionPlan logoutExecutionPlan) {
            return new FrontChannelLogoutAction(centralAuthenticationService,
                ticketGrantingTicketCookieGenerator, argumentExtractor,
                servicesManager, logoutExecutionPlan, casProperties);
        }

        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_TICKET_GRANTING_TICKET_CHECK)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action ticketGrantingTicketCheckAction(
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService) {
            return new TicketGrantingTicketCheckAction(centralAuthenticationService);
        }

        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_TERMINATE_SESSION)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action terminateSessionAction(
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(LogoutManager.DEFAULT_BEAN_NAME)
            final LogoutManager logoutManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier("warnCookieGenerator")
            final CasCookieBuilder warnCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier("defaultSingleLogoutRequestExecutor")
            final SingleLogoutRequestExecutor defaultSingleLogoutRequestExecutor) {
            return new TerminateSessionAction(centralAuthenticationService, ticketGrantingTicketCookieGenerator,
                warnCookieGenerator, casProperties.getLogout(), logoutManager,
                applicationContext, defaultSingleLogoutRequestExecutor);
        }

        @Bean
        @ConditionalOnMissingBean(name = "confirmLogoutAction")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action confirmLogoutAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor,
            @Qualifier("logoutExecutionPlan")
            final LogoutExecutionPlan logoutExecutionPlan) {
            return new ConfirmLogoutAction(centralAuthenticationService, ticketGrantingTicketCookieGenerator,
                argumentExtractor, servicesManager, logoutExecutionPlan, casProperties);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_LOGOUT_VIEW_SETUP)
        public Action logoutViewSetupAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
            final CasCookieBuilder ticketGrantingTicketCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor,
            @Qualifier("logoutExecutionPlan")
            final LogoutExecutionPlan logoutExecutionPlan) {
            return new LogoutViewSetupAction(centralAuthenticationService,
                ticketGrantingTicketCookieGenerator, argumentExtractor,
                servicesManager, logoutExecutionPlan, casProperties);
        }

        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_SERVICE_WARNING)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action serviceWarningAction(
            @Qualifier("warnCookieGenerator")
            final CasCookieBuilder warnCookieGenerator,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService,
            @Qualifier(AuthenticationSystemSupport.BEAN_NAME)
            final AuthenticationSystemSupport authenticationSystemSupport,
            @Qualifier(TicketRegistrySupport.BEAN_NAME)
            final TicketRegistrySupport ticketRegistrySupport,
            @Qualifier(PrincipalElectionStrategy.BEAN_NAME)
            final PrincipalElectionStrategy principalElectionStrategy) {
            return new ServiceWarningAction(centralAuthenticationService, authenticationSystemSupport,
                ticketRegistrySupport, warnCookieGenerator, principalElectionStrategy);
        }

        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_POPULATE_SECURITY_CONTEXT)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public Action populateSpringSecurityContextAction() {
            return new PopulateSpringSecurityContextAction();
        }
    }

    @Configuration(value = "CasSupportActionsAccountProfileConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.AccountManagement, enabledByDefault = false)
    public static class CasSupportActionsAccountProfileConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_PREPARE_ACCOUNT_PROFILE)
        public Action prepareAccountProfileViewAction(
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            final CasConfigurationProperties casProperties,
            @Qualifier(CentralAuthenticationService.BEAN_NAME)
            final CentralAuthenticationService centralAuthenticationService) {
            return new PrepareAccountProfileViewAction(centralAuthenticationService, servicesManager, casProperties);
        }
    }
}
