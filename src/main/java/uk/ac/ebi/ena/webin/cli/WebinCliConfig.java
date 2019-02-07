package uk.ac.ebi.ena.webin.cli;

import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

@Data
public class WebinCliConfig {

    public WebinCliConfig() {
        try {
            serviceMessages = PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource("/ServiceMessages.properties"));
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final String webinRestUriTest = "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/";
    private final String webinRestUriProd = "https://www.ebi.ac.uk/ena/submit/drop-box/";

    private final Properties serviceMessages;

    public String getServiceMessage(String messageKey) {
        return serviceMessages.getProperty(messageKey);
    }
}
