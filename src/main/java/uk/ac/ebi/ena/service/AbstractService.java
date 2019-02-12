package uk.ac.ebi.ena.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

public abstract class AbstractService {

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

    protected final String getServiceMessage(String messageKey) {
        return serviceMessages.getProperty(messageKey);
    }

    protected final String getWebinRestUri(String uri, boolean test) {
        return (test) ?
                webinRestUriTest + uri :
                webinRestUriProd + uri;
    }
}
