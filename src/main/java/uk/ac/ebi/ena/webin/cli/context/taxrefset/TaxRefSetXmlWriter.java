package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createTextElement;

public class TaxRefSetXmlWriter extends SequenceToolsXmlWriter<TaxRefSetManifest, ValidationResponse> {

    @Override
    protected Element createXmlAnalysisTypeElement(TaxRefSetManifest manifest) {
        Element e = new Element("TAXONOMIC_REFERENCE_SET");

        e.addContent(createTextElement("NAME", manifest.getName()));
        e.addContent(createTextElement("TAXONOMY_SYSTEM", manifest.getTaxonomySystem()));
        if (null != manifest.getTaxonomySystemVersion() && !manifest.getTaxonomySystemVersion().trim().isEmpty())
            e.addContent(createTextElement("TAXONOMY_SYSTEM_VERSION", manifest.getTaxonomySystemVersion()));
        if (null != manifest.getCustomFields() && !manifest.getCustomFields().isEmpty()) {

            Element customFieldsE = new Element("CUSTOM_FIELDS");

            manifest.getCustomFields().forEach((key, value) -> {
                        Element fileldsE = new Element("FIELD");
                        fileldsE.addContent(createTextElement("NAME", key));
                        fileldsE.addContent(createTextElement("DESCRIPTION", value));
                        customFieldsE.addContent(fileldsE);
                    }
            );

            e.addContent(customFieldsE);
        }

        return e;
    }

    @Override
    protected List<Element> createXmlFileElements(TaxRefSetManifest manifest, Path inputDir, Path uploadDir) {
        List<Element> list = new ArrayList<>();


        manifest.files(TaxRefSetManifest.FileType.FASTA).stream()
                .map(file -> file.getFile().toPath())
                .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "fasta")));
        manifest.files(TaxRefSetManifest.FileType.TAB).stream()
                .map(file -> file.getFile().toPath())
                .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "tab")));

        return list;
    }
}
