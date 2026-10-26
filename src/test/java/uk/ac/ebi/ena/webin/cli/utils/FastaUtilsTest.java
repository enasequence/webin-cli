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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;

public class FastaUtilsTest {

  private static boolean hasJsonFastaHeader(String contents) {
    return FastaUtils.hasJsonFastaHeader(TempFileBuilder.file("test.fasta", contents).toFile());
  }

  @Test
  public void testJsonFastaHeader() {
    assertThat(
            hasJsonFastaHeader(
                ">AF123456.1 | {\"description\":\"Pinus sativa\",\"molecule_type\":\"genomic\"}\n"
                    + "ACGT\n"))
        .isTrue();
    assertThat(hasJsonFastaHeader("> test1|{\"topology\":\"circular\"}\nACGT\n")).isTrue();
    assertThat(hasJsonFastaHeader("\n>AF123456.1 |{}\nACGT\n")).isTrue();
    assertThat(hasJsonFastaHeader(">AF1\nACGT\n>AF2 | {\"topology\":\"linear\"}\nACGT\n")).isTrue();
  }

  @Test
  public void testJsonFastaHeaderOnlySearchedInFirst200Lines() {
    String sequenceLines = String.join("", java.util.Collections.nCopies(198, "ACGT\n"));
    assertThat(hasJsonFastaHeader(">AF1\n" + sequenceLines + ">AF2 | {}\nACGT\n")).isTrue();
    assertThat(hasJsonFastaHeader(">AF1\n" + sequenceLines + "ACGT\n>AF2 | {}\nACGT\n")).isFalse();
  }

  @Test
  public void testNonJsonFastaHeader() {
    assertThat(hasJsonFastaHeader(">AF123456.1\nACGT\n")).isFalse();
    assertThat(hasJsonFastaHeader(">AF123456.1 some description\nACGT\n")).isFalse();
    assertThat(hasJsonFastaHeader(">AF123456.1 | not json\nACGT\n")).isFalse();
    assertThat(hasJsonFastaHeader(">AF123456.1 | {\"description\":\"x\"\nACGT\n")).isFalse();
    assertThat(hasJsonFastaHeader("")).isFalse();
  }
}
