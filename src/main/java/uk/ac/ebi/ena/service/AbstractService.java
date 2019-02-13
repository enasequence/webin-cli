package uk.ac.ebi.ena.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

abstract class AbstractService {

    static {
        try {
            serviceMessages = PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource("/ServiceMessages.properties"));
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final static String webinRestUriTest = "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/";
    private final static String webinRestUriProd = "https://www.ebi.ac.uk/ena/submit/drop-box/";
    private final static Properties serviceMessages;

    final String getServiceMessage(String messageKey) {
        return serviceMessages.getProperty(messageKey);
    }

    final String getWebinRestUri(String uri, boolean test) {
        return (test) ?
                webinRestUriTest + uri :
                webinRestUriProd + uri;
    }
}
