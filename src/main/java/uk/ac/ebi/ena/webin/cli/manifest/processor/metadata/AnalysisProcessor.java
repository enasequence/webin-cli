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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorParameters;
import uk.ac.ebi.ena.webin.cli.service.AnalysisService;
import uk.ac.ebi.ena.webin.cli.utils.UrlUtils;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;

public class AnalysisProcessor implements ManifestFieldProcessor {
  private final MetadataProcessorParameters parameters;
  private ManifestFieldProcessor.Callback<List<Analysis>> callback;

  public AnalysisProcessor(
      MetadataProcessorParameters parameters,
      ManifestFieldProcessor.Callback<List<Analysis>> callback) {
    this.parameters = parameters;
    this.callback = callback;
  }

  public AnalysisProcessor(MetadataProcessorParameters parameters) {
    this.parameters = parameters;
  }

  public void setCallback(Callback<List<Analysis>> callback) {
    this.callback = callback;
  }

  @Override
  public void process(ValidationResult result, ManifestFieldValue fieldValue) {
    String value = fieldValue.getValue();
    String[] ids = value.split(", *");
    Set<String> idsSet = new HashSet<>();
    List<Analysis> analysis_list = new ArrayList<>(ids.length);

    for (String a : ids) {
      String id = a.trim();
      if (id.isEmpty()) continue;
      if (!idsSet.add(id)) continue;
      try {
        AnalysisService analysisService =
            new AnalysisService.Builder()
                .setWebinRestUri(UrlUtils.getWebinRestUrl(parameters.isTest()))
                .setCredentials(parameters.getWebinServiceUserName(), parameters.getPassword())
                .build();
        analysis_list.add(analysisService.getAnalysis(id));

      } catch (WebinCliException e) {
        result.add(ValidationMessage.error(e));
      }
    }

    if (result.isValid()) {
      fieldValue.setValue(
          analysis_list.stream().map(e -> e.getAnalysisId()).collect(Collectors.joining(", ")));
      callback.notify(analysis_list);
    }
  }
}
