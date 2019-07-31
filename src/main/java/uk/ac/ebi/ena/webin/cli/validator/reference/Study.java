/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.validator.reference;

import java.util.List;

/**
 * Study reference.
 */
public class Study {

    private String bioProjectId;
    private List<String> locusTags;

    public String getBioProjectId() {
        return bioProjectId;
    }

    public void setBioProjectId(String bioProjectId) {
        this.bioProjectId = bioProjectId;
    }

    public List<String> getLocusTags() {
        return locusTags;
    }

    public void setLocusTags(List<String> locusTags) {
        this.locusTags = locusTags;
    }

    public void addLocusTag(String locusTag) {
        locusTags.add(locusTag);
    }
}
