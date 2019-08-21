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
package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsWebinCli;

public enum WebinCliContext {
  genome(GenomeAssemblyWebinCli.class, "Genome assembly"),
  transcriptome(TranscriptomeAssemblyWebinCli.class, "Transcriptome assembly"),
  sequence(SequenceAssemblyWebinCli.class, "Sequence assembly"),
  reads(RawReadsWebinCli.class, "Raw reads");

  private final Class<? extends AbstractWebinCli> validatorClass;
  private final String titlePrefix;

  WebinCliContext(Class<? extends AbstractWebinCli> validatorClass, String titlePrefix) {
    this.validatorClass = validatorClass;
    this.titlePrefix = titlePrefix;
  }

  public Class<? extends AbstractWebinCli> getValidatorClass() {
    return this.validatorClass;
  }

  public AbstractWebinCli createValidator(WebinCliParameters parameters) {
    return createValidator(validatorClass, parameters);
  }

  public static <T extends AbstractWebinCli> T createValidator(
      Class<T> validatorClass, WebinCliParameters parameters) {
    try {
      return validatorClass.getConstructor(WebinCliParameters.class).newInstance(parameters);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getTitlePrefix() {
    return titlePrefix;
  }
}
