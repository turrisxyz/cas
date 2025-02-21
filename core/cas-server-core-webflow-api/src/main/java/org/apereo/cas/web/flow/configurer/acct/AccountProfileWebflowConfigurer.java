package org.apereo.cas.web.flow.configurer.acct;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.configurer.AbstractCasWebflowConfigurer;

import lombok.val;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;

/**
 * This is {@link AccountProfileWebflowConfigurer}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
public class AccountProfileWebflowConfigurer extends AbstractCasWebflowConfigurer {
    public AccountProfileWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                           final FlowDefinitionRegistry mainFlowDefinitionRegistry,
                                           final ConfigurableApplicationContext applicationContext,
                                           final CasConfigurationProperties casProperties) {
        super(flowBuilderServices, mainFlowDefinitionRegistry, applicationContext, casProperties);
        setOrder(casProperties.getAuthn().getPm().getWebflow().getOrder());
    }

    @Override
    protected void doInitialize() {
        val accountFlow = buildFlow(CasWebflowConfigurer.FLOW_ID_ACCOUNT);
        accountFlow.getStartActionList().add(createEvaluateAction(CasWebflowConstants.ACTION_ID_FETCH_TICKET_GRANTING_TICKET));

        val myAccountView = createViewState(accountFlow, CasWebflowConstants.STATE_ID_MY_ACCOUNT_PROFILE_VIEW, "acct/casMyAccountProfile");
        myAccountView.getRenderActionList().add(createEvaluateAction(CasWebflowConstants.ACTION_ID_PREPARE_ACCOUNT_PROFILE));
        createTransitionForState(myAccountView, CasWebflowConstants.TRANSITION_ID_RESET_PASSWORD, CasWebflowConstants.STATE_ID_PASSWORD_CHANGE_REQUEST);
        createTransitionForState(myAccountView, CasWebflowConstants.TRANSITION_ID_UPDATE_SECURITY_QUESTIONS, CasWebflowConstants.STATE_ID_UPDATE_SECURITY_QUESTIONS);

        val updateQuestions = createActionState(accountFlow, CasWebflowConstants.STATE_ID_UPDATE_SECURITY_QUESTIONS, "accountProfileUpdateSecurityQuestionsAction");
        createTransitionForState(updateQuestions, CasWebflowConstants.TRANSITION_ID_SUCCESS, CasWebflowConstants.STATE_ID_MY_ACCOUNT_PROFILE_VIEW);
        createTransitionForState(updateQuestions, CasWebflowConstants.TRANSITION_ID_ERROR, CasWebflowConstants.STATE_ID_MY_ACCOUNT_PROFILE_VIEW);

        val passwordChangeRequest = createActionState(accountFlow, CasWebflowConstants.STATE_ID_PASSWORD_CHANGE_REQUEST, "accountProfilePasswordChangeRequestAction");
        createTransitionForState(passwordChangeRequest, CasWebflowConstants.TRANSITION_ID_SUCCESS, "redirectToPasswordReset");
        createEndState(accountFlow, "redirectToPasswordReset", "requestScope.url", true);

        val validate = createActionState(accountFlow, CasWebflowConstants.STATE_ID_TICKET_GRANTING_TICKET_CHECK,
            CasWebflowConstants.ACTION_ID_TICKET_GRANTING_TICKET_CHECK);
        createTransitionForState(validate, CasWebflowConstants.TRANSITION_ID_TICKET_GRANTING_TICKET_VALID, myAccountView.getId());
        createStateDefaultTransition(validate, CasWebflowConstants.STATE_ID_REDIRECT_TO_LOGIN);

        val view = createExternalRedirectViewFactory(String.format("'%s'", casProperties.getServer().getLoginUrl()));
        createEndState(accountFlow, CasWebflowConstants.STATE_ID_REDIRECT_TO_LOGIN, view);

        accountFlow.setStartState(validate);
        mainFlowDefinitionRegistry.registerFlowDefinition(accountFlow);
    }

}
