package uk.ac.ebi.ena.service.utils;

import uk.ac.ebi.ena.webin.cli.WebinCliConfig;

public class UriUtils {

    public static String getWebinRestUri(WebinCliConfig config, String uri, boolean test) {
        return (test) ?
                config.getWebinRestUriTest() + uri :
                config.getWebinRestUriProd() + uri;
    }
}
