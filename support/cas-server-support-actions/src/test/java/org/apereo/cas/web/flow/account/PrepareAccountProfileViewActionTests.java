package org.apereo.cas.web.flow.account;

import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.web.flow.AbstractWebflowActionsTests;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.config.CasWebflowAccountProfileConfiguration;
import org.apereo.cas.web.support.WebUtils;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.RequestContextHolder;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockRequestContext;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link PrepareAccountProfileViewActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@Tag("WebflowActions")
@TestPropertySource(properties = {
    "CasFeatureModule.AccountManagement.enabled=true",
    "cas.view.authorized-services-on-successful-login=true"
})
@Import(CasWebflowAccountProfileConfiguration.class)
public class PrepareAccountProfileViewActionTests extends AbstractWebflowActionsTests {
    @Autowired
    @Qualifier(CasWebflowConstants.ACTION_ID_PREPARE_ACCOUNT_PROFILE)
    private Action prepareAccountProfileViewAction;

    @Test
    public void verifyOperation() throws Exception {
        val registeredService1 = RegisteredServiceTestUtils.getRegisteredService(UUID.randomUUID().toString(), Map.of());
        getServicesManager().save(registeredService1);

        val context = new MockRequestContext();
        val tgt = new MockTicketGrantingTicket("casuser");
        WebUtils.putTicketGrantingTicketInScopes(context, tgt);
        getCentralAuthenticationService().addTicket(tgt);

        context.setExternalContext(new MockExternalContext());
        RequestContextHolder.setRequestContext(context);
        ExternalContextHolder.setExternalContext(context.getExternalContext());

        val result = prepareAccountProfileViewAction.execute(context);
        assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, result.getId());
        assertNotNull(WebUtils.getAuthorizedServices(context));
        val list = WebUtils.getAuthorizedServices(context);
        assertFalse(list.isEmpty());
        assertNotNull(WebUtils.getAuthentication(context));
    }
}
