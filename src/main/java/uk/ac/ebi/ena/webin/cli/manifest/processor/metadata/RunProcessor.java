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

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorParameters;
import uk.ac.ebi.ena.webin.cli.service.RunService;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RunProcessor implements ManifestFieldProcessor {
  private final MetadataProcessorParameters parameters;
  private ManifestFieldProcessor.Callback<List<Run>> callback;

  public RunProcessor(
      MetadataProcessorParameters parameters, ManifestFieldProcessor.Callback<List<Run>> callback) {
    this.parameters = parameters;
    this.callback = callback;
  }

  public RunProcessor(MetadataProcessorParameters parameters) {
    this.parameters = parameters;
  }

  public void setCallback(Callback<List<Run>> callback) {
    this.callback = callback;
  }

  @Override
  public void process(ValidationResult result, ManifestFieldValue fieldValue) {
    String value = fieldValue.getValue();
    String[] ids = value.split(", *");
    Set<String> idsSet = new HashSet<>();
    List<Run> run_list = new ArrayList<>(ids.length);

    for (String r : ids) {
      String id = r.trim();
      if (id.isEmpty()) continue;

      if (!idsSet.add(id)) continue;
      try {
        RunService runService =
            new RunService.Builder()
                .setCredentials(parameters.getWebinServiceUserName(), parameters.getPassword())
                .setTest(parameters.isTest())
                .build();
        run_list.add(runService.getRun(id));

      } catch (WebinCliException e) {
        result.add(ValidationMessage.error(e));
      }
    }

    if (result.isValid()) {
      fieldValue.setValue(
          run_list.stream().map(e -> e.getRunId()).collect(Collectors.joining(", ")));

      callback.notify(run_list);
    }
  }
}
