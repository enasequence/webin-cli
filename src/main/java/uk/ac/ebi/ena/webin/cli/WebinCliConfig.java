package uk.ac.ebi.ena.webin.cli;

import lombok.Data;

@Data
public class WebinCliConfig {

    private final String webinRestUriTest = "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/";
    private final String webinRestUriProd = "https://www.ebi.ac.uk/ena/submit/drop-box/";
}
