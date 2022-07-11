/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.CDATA;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

public class SubmissionXmlWriter<M extends Manifest, R extends ValidationResponse> {

    public Map<SubmissionBundle.SubmissionXMLFileType, String> createXml(
        R response,
        String centerName,
        String submissionTool,
        String manifestFileContent,
        String manifestFileMd5) {

        Element submissionSetE = new Element("SUBMISSION_SET");

        Element submissionE = new Element("SUBMISSION");
        if (centerName != null && !centerName.isEmpty()) {
            submissionE.setAttribute("center_name", centerName);
        }
        submissionSetE.addContent(submissionE);

        Element actionsE = new Element("ACTIONS");
        submissionE.addContent(actionsE);

        Element actionE = new Element("ACTION");
        actionsE.addContent(actionE);

        Element addE = new Element("ADD");
        actionE.addContent(addE);

        Element submissionAttributesE = new Element("SUBMISSION_ATTRIBUTES");
        submissionE.addContent(submissionAttributesE);

        addAttribute(submissionAttributesE, "ENA-SUBMISSION-TOOL", new Text(submissionTool));
        addAttribute(submissionAttributesE, "ENA-MANIFEST-FILE", new CDATA(manifestFileContent));
        addAttribute(submissionAttributesE, "ENA-MANIFEST-FILE-MD5", new Text(manifestFileMd5));

        Format format = Format.getPrettyFormat();

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(format);
        StringWriter stringWriter = new StringWriter();

        try {
            xmlOutput.output(submissionSetE, stringWriter);
        } catch (IOException ex) {
            throw WebinCliException.systemError(ex);
        }

        Map<SubmissionBundle.SubmissionXMLFileType, String> xmls = new HashMap<>();
        xmls.put(SubmissionBundle.SubmissionXMLFileType.SUBMISSION, stringWriter.toString());
        return xmls;
    }

    private void addAttribute(Element submissionAttributesE, String tag, Content value) {
        Element attributeE = new Element("SUBMISSION_ATTRIBUTE");
        submissionAttributesE.addContent(attributeE);

        Element tagE = new Element("TAG");
        tagE.addContent(tag);
        attributeE.addContent(tagE);

        Element valueE = new Element("VALUE");
        valueE.addContent(value);
        attributeE.addContent(valueE);
    }
}
