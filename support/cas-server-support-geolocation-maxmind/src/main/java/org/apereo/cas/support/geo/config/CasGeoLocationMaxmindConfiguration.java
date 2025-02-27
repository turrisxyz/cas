package org.apereo.cas.support.geo.config;

import org.apereo.cas.authentication.adaptive.geo.GeoLocationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.support.geo.GeoLocationServiceConfigurer;
import org.apereo.cas.support.geo.maxmind.MaxmindDatabaseGeoLocationService;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * This is {@link CasGeoLocationMaxmindConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration(value = "CasGeoLocationMaxmindConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.GeoLocation)
public class CasGeoLocationMaxmindConfiguration {

    private static DatabaseReader readDatabase(final Resource maxmindDatabase) throws IOException {
        if (ResourceUtils.doesResourceExist(maxmindDatabase)) {
            return new DatabaseReader.Builder(maxmindDatabase.getFile()).fileMode(Reader.FileMode.MEMORY).withCache(new CHMCache()).build();
        }
        return null;
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "maxMindGeoLocationService")
    public GeoLocationService maxMindGeoLocationService(final CasConfigurationProperties casProperties)
        throws Exception {
        val properties = casProperties.getGeoLocation().getMaxmind();
        val cityDatabase = readDatabase(properties.getCityDatabase());
        val countryDatabase = readDatabase(properties.getCountryDatabase());
        return new MaxmindDatabaseGeoLocationService(cityDatabase, countryDatabase);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "maxMindGeoLocationServiceConfigurer")
    public GeoLocationServiceConfigurer maxMindGeoLocationServiceConfigurer(
        @Qualifier("maxMindGeoLocationService")
        final GeoLocationService maxMindGeoLocationService) {
        return () -> maxMindGeoLocationService;
    }
}
