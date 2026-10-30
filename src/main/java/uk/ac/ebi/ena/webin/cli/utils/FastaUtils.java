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
package uk.ac.ebi.ena.webin.cli.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class FastaUtils {

  /** FASTA header with a sequence id followed by a JSON object, e.g. {@code >ID | {...}}. */
  private static final Pattern JSON_FASTA_HEADER_PATTERN =
      Pattern.compile("^>\\s*\\S+\\s*\\|\\s*\\{.*\\}\\s*$");

  /** Number of FASTA lines searched for a JSON header. */
  private static final int JSON_FASTA_HEADER_MAX_LINES = 200;

  /**
   * Returns true if any FASTA header within the first {@link #JSON_FASTA_HEADER_MAX_LINES} lines is
   * in the {@code >ID | {json}} format.
   */
  public static boolean hasJsonFastaHeader(File fastaFile) {
    try (BufferedReader reader = FileUtils.getBufferedReader(fastaFile)) {
      String line;
      int lineCount = 0;
      while (lineCount++ < JSON_FASTA_HEADER_MAX_LINES && (line = reader.readLine()) != null) {
        if (line.startsWith(">") && JSON_FASTA_HEADER_PATTERN.matcher(line).matches()) {
          return true;
        }
      }
      return false;
    } catch (IOException ex) {
      throw WebinCliException.systemError(ex);
    }
  }
}
