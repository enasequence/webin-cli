package uk.ac.ebi.ena.webin.cli;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

public class WebinCliConfig {

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

    public final static String getServiceMessage(String messageKey) {
        return serviceMessages.getProperty(messageKey);
    }

    public static String getWebinRestUri(WebinCliConfig config, String uri, boolean test) {
        return (test) ?
                webinRestUriTest + uri :
                webinRestUriProd + uri;
    }
}
