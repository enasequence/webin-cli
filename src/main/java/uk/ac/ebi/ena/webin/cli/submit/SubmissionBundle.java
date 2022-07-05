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
package uk.ac.ebi.ena.webin.cli.submit;

import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SubmissionBundle implements Serializable {
    protected static final long serialVersionUID = 1L;

    protected final List<SubmissionXMLFile> xmlFileList;

    protected final File submitDir;

    protected final String uploadDir;

    protected final List<File> uploadFileList;

    protected final List<Long> uploadFileSize;

    protected final String manifestMd5;

    public enum SubmissionXMLFileType {
        SUBMISSION,
        ANALYSIS,
        RUN,
        EXPERIMENT
    }

    public static class SubmissionXMLFile implements Serializable {
        private static final long serialVersionUID = 1L;
        private File file;
        private SubmissionXMLFileType type;
        private String md5;
        private transient String xmlContent;

        public SubmissionXMLFile( SubmissionXMLFileType type, File file, String xmlContent) {
            this.type = type;
            this.file = file;
            this.xmlContent = xmlContent;
        }

        public String toString() {
            return String.format( "%s|%s|%s", type, file, md5 );
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public SubmissionXMLFileType getType() {
            return type;
        }

        public void setType(SubmissionXMLFileType type) {
            this.type = type;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getXmlContent() {
            try {
                if (xmlContent == null) {
                    xmlContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                }

                return xmlContent;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void setXmlContent(String xmlContent) {
            this.xmlContent = xmlContent;
        }
    }

    public SubmissionBundle(File submitDir, String uploadDir, List<File> uploadFileList,
                            List<SubmissionXMLFile> xmlFileList, String manifestMd5 ) {
        this.submitDir = submitDir;
        this.uploadDir = uploadDir;
        this.uploadFileList = uploadFileList;
        this.uploadFileSize = uploadFileList.stream().sequential().map( f -> f.length() ).collect( Collectors.toList() );
        this.xmlFileList = xmlFileList;
        this.manifestMd5 = manifestMd5;
    }

    public abstract void validate( ValidationResult result );

    public boolean equals( Object other ) {
        if( other instanceof SubmissionBundle ) {
            SubmissionBundle sb = (SubmissionBundle)other;
            return this.xmlFileList.equals( sb.xmlFileList )
                && this.submitDir.equals( sb.submitDir )
                && this.uploadDir.equals( sb.uploadDir )
                && this.uploadFileList.equals( sb.uploadFileList )
                && this.uploadFileSize.equals( sb.uploadFileSize )
                && this.manifestMd5.equals( sb.manifestMd5);
        }
        return false;
    }

    public String getManifestMd5() {
        return this.manifestMd5;
    }

    public File getSubmitDir() {
        return submitDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public List<File> getUploadFileList() {
        return uploadFileList;
    }

    public List<SubmissionXMLFile> getXMLFileList() {
        return xmlFileList;
    }

    public SubmissionXMLFile getXMLFile(SubmissionXMLFileType fileType) {
        return xmlFileList.stream().filter(file -> file.getType().equals(fileType)).findFirst().get();
    }
}
