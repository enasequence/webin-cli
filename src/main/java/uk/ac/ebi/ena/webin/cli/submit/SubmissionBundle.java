/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.submit;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import uk.ac.ebi.ena.webin.cli.WebinCli;

public class SubmissionBundle implements Serializable {
    protected static final long serialVersionUID = 1L;

    private final String version;

    private final List<SubmissionXMLFile> xmlFileList;

    private final File submitDir;

    private final String uploadDir;

    private final List<File> uploadFileList;

    private final List<Long> uploadFileSize;

    private final String manifestMd5;

    public enum SubmissionXMLFileType {
        SUBMISSION,
        ANALYSIS,
        RUN,
        EXPERIMENT
    }

    public static class SubmissionXMLFile implements Serializable {
        private static final long serialVersionUID = 1L;

        /** XML written in file. */
        private final File file;
        private final SubmissionXMLFileType type;

        /** MD5 checksum for the XML file. Only meant to be used during validation of the submission bundle. */
        private String md5;

        /** XML cached in memory. No need to serialize as it gets written in an xml file separately. */
        private transient String xmlContent;

        public SubmissionXMLFile( SubmissionXMLFileType type, File file, String xmlContent) {
            this.type = type;
            this.file = file;
            this.xmlContent = xmlContent;
        }

        public String toString() {
            return String.format( "%s|%s|%s", type, file, md5 );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubmissionXMLFile xmlFile = (SubmissionXMLFile) o;
            return file.equals(xmlFile.file) && type == xmlFile.type && Objects.equals(md5, xmlFile.md5);
        }

        public File getFile() {
            return file;
        }

        public SubmissionXMLFileType getType() {
            return type;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getXmlContent() {
            return xmlContent;
        }

        public void setXmlContent(String xmlContent) {
            this.xmlContent = xmlContent;
        }
    }

    public SubmissionBundle(File submitDir, String uploadDir, List<File> uploadFileList,
                            List<SubmissionXMLFile> xmlFileList, String manifestMd5 ) {
        this.version = WebinCli.getVersion();
        this.submitDir = submitDir;
        this.uploadDir = uploadDir;
        this.uploadFileList = uploadFileList;
        this.uploadFileSize = uploadFileList.stream().sequential().map( f -> f.length() ).collect( Collectors.toList() );
        this.xmlFileList = xmlFileList;
        this.manifestMd5 = manifestMd5;
    }

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

    public String getVersion() {
        return version;
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

    public List<Long> getUploadFileSize() {
        return uploadFileSize;
    }

    public List<SubmissionXMLFile> getXMLFileList() {
        return xmlFileList;
    }

    public SubmissionXMLFile getXMLFile(SubmissionXMLFileType fileType) {
        return xmlFileList.stream().filter(file -> file.getType().equals(fileType)).findFirst().get();
    }
}
