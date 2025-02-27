package org.apereo.cas.authentication.principal;

import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.validation.ValidationResponseType;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.utils.URIBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The {@link WebApplicationServiceFactory} is responsible for
 * creating {@link WebApplicationService} objects.
 *
 * @author Misagh Moayyed
 * @since 4.2
 */
@Slf4j
public class WebApplicationServiceFactory extends AbstractServiceFactory<WebApplicationService> {
    private static final List<String> IGNORED_ATTRIBUTES_PARAMS = List.of(
        CasProtocolConstants.PARAMETER_PASSWORD,
        CasProtocolConstants.PARAMETER_SERVICE,
        CasProtocolConstants.PARAMETER_TARGET_SERVICE,
        CasProtocolConstants.PARAMETER_TICKET,
        CasProtocolConstants.PARAMETER_FORMAT);

    /**
     * Build new web application service simple web application service.
     *
     * @param request      the request
     * @param serviceToUse the service to use
     * @return the simple web application service
     */
    protected static AbstractWebApplicationService newWebApplicationService(
        final HttpServletRequest request, final String serviceToUse) {
        val artifactId = Optional.ofNullable(request)
            .map(httpServletRequest -> httpServletRequest.getParameter(CasProtocolConstants.PARAMETER_TICKET))
            .orElse(null);
        val id = cleanupUrl(serviceToUse);
        val newService = new SimpleWebApplicationServiceImpl(id, serviceToUse, artifactId);
        determineWebApplicationFormat(request, newService);
        val source = getSourceParameter(request, CasProtocolConstants.PARAMETER_TARGET_SERVICE,
            CasProtocolConstants.PARAMETER_SERVICE);
        newService.setSource(source);
        if (request != null) {
            populateAttributes(newService, request);
        }
        return newService;
    }

    private static void populateAttributes(final AbstractWebApplicationService service, final HttpServletRequest request) {
        val attributes = request.getParameterMap()
            .entrySet()
            .stream()
            .filter(entry -> !IGNORED_ATTRIBUTES_PARAMS.contains(entry.getKey()))
            .map(entry -> Pair.of(entry.getKey(), CollectionUtils.toCollection(entry.getValue(), ArrayList.class)))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        LOGGER.trace("Collected request parameters [{}] as service attributes", attributes);
        val validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
        if (validator.isValid(service.getOriginalUrl())) {
            val queryParams = FunctionUtils.doUnchecked(() -> new URIBuilder(service.getOriginalUrl()).getQueryParams());
            queryParams.forEach(pair -> attributes.put(pair.getName(), CollectionUtils.wrapArrayList(pair.getValue())));
        }

        LOGGER.trace("Extracted attributes [{}] for service [{}]", attributes, service.getId());
        service.setAttributes(new HashMap(attributes));
    }

    private static AbstractWebApplicationService determineWebApplicationFormat(
        final HttpServletRequest request,
        final AbstractWebApplicationService webApplicationService) {
        val format = Optional.ofNullable(request)
            .map(httpServletRequest -> httpServletRequest.getParameter(CasProtocolConstants.PARAMETER_FORMAT))
            .orElse(StringUtils.EMPTY);
        try {
            if (StringUtils.isNotBlank(format)) {
                val formatType = ValidationResponseType.valueOf(Objects.requireNonNull(format).toUpperCase());
                webApplicationService.setFormat(formatType);
            }
        } catch (final Exception e) {
            LOGGER.error("Format specified in the request [{}] is not recognized", format);
        }
        return webApplicationService;
    }

    @Override
    public WebApplicationService createService(final HttpServletRequest request) {
        val serviceToUse = getRequestedService(request);
        if (StringUtils.isBlank(serviceToUse)) {
            LOGGER.trace("No service is specified in the request. Skipping service creation");
            return null;
        }
        return newWebApplicationService(request, serviceToUse);
    }

    @Override
    public WebApplicationService createService(final String id) {
        val request = HttpRequestUtils.getHttpServletRequestFromRequestAttributes();
        return newWebApplicationService(request, id);
    }

    /**
     * Gets requested service.
     *
     * @param request the request
     * @return the requested service
     */
    protected String getRequestedService(final HttpServletRequest request) {
        val targetService = request.getParameter(CasProtocolConstants.PARAMETER_TARGET_SERVICE);
        val service = request.getParameter(CasProtocolConstants.PARAMETER_SERVICE);
        val serviceAttribute = request.getAttribute(CasProtocolConstants.PARAMETER_SERVICE);

        if (StringUtils.isNotBlank(targetService)) {
            return targetService;
        }
        if (StringUtils.isNotBlank(service)) {
            return service;
        }
        if (serviceAttribute != null) {
            if (serviceAttribute instanceof Service) {
                return ((Service) serviceAttribute).getId();
            }
            return serviceAttribute.toString();
        }
        return null;
    }
}
