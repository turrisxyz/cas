package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthAccessTokenExpirationPolicy;
import org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthCodeExpirationPolicy;
import org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthRefreshTokenExpirationPolicy;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessTokenExpirationPolicy;
import org.apereo.cas.ticket.accesstoken.OAuth20DefaultAccessToken;
import org.apereo.cas.ticket.code.OAuth20CodeExpirationPolicy;
import org.apereo.cas.ticket.code.OAuth20DefaultCode;
import org.apereo.cas.ticket.device.OAuth20DefaultDeviceToken;
import org.apereo.cas.ticket.device.OAuth20DefaultDeviceUserCode;
import org.apereo.cas.ticket.refreshtoken.OAuth20DefaultRefreshToken;
import org.apereo.cas.ticket.refreshtoken.OAuth20RefreshTokenExpirationPolicy;
import org.apereo.cas.util.serialization.ComponentSerializationPlanConfigurer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CasOAuth20ComponentSerializationConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Configuration(value = "CasOAuth20ComponentSerializationConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.OAuth)
public class CasOAuth20ComponentSerializationConfiguration {

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "oauthComponentSerializationPlanConfigurer")
    public ComponentSerializationPlanConfigurer oauthComponentSerializationPlanConfigurer() {
        return plan -> {
            plan.registerSerializableClass(OAuth20AccessTokenExpirationPolicy.class);
            plan.registerSerializableClass(OAuth20RefreshTokenExpirationPolicy.class);
            plan.registerSerializableClass(OAuth20RefreshTokenExpirationPolicy.OAuthRefreshTokenStandaloneExpirationPolicy.class);
            plan.registerSerializableClass(OAuth20CodeExpirationPolicy.class);

            plan.registerSerializableClass(OAuthRegisteredService.class);

            plan.registerSerializableClass(DefaultRegisteredServiceOAuthCodeExpirationPolicy.class);
            plan.registerSerializableClass(DefaultRegisteredServiceOAuthAccessTokenExpirationPolicy.class);
            plan.registerSerializableClass(DefaultRegisteredServiceOAuthRefreshTokenExpirationPolicy.class);

            plan.registerSerializableClass(OAuth20DefaultCode.class);
            plan.registerSerializableClass(OAuth20DefaultAccessToken.class);
            plan.registerSerializableClass(OAuth20DefaultRefreshToken.class);
            plan.registerSerializableClass(OAuth20DefaultDeviceToken.class);
            plan.registerSerializableClass(OAuth20DefaultDeviceUserCode.class);

            plan.registerSerializableClass(OAuth20GrantTypes.class);
            plan.registerSerializableClass(OAuth20ResponseTypes.class);
        };
    }
}
