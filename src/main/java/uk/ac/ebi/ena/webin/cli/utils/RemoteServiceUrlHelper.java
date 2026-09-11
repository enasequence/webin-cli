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

public class RemoteServiceUrlHelper {
  public static final String WEBIN_REST_V1_TEST_URL =
      "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/";
  public static final String WEBIN_REST_V1_PROD_URL = "https://www.ebi.ac.uk/ena/submit/drop-box/";

  public static final String WEBIN_REST_V2_TEST_URL =
      "https://wwwdev.ebi.ac.uk/ena/submit/webin-v2/";
  public static final String WEBIN_REST_V2_PROD_URL = "https://www.ebi.ac.uk/ena/submit/webin-v2/";

  public static final String WEBIN_AUTH_TEST_URL =
      "https://wwwdev.ebi.ac.uk/ena/submit/webin/auth/token";
  public static final String WEBIN_AUTH_PROD_URL =
      "https://www.ebi.ac.uk/ena/submit/webin/auth/token";

  public static final String BIOSAMPLES_TEST_URL = "https://wwwdev.ebi.ac.uk/biosamples/";
  public static final String BIOSAMPLES_PROD_URL = "https://www.ebi.ac.uk/biosamples/";

  public static String getWebinRestV1Url(boolean isTestMode) {
    return isTestMode ? WEBIN_REST_V1_TEST_URL : WEBIN_REST_V1_PROD_URL;
  }

  public static String getWebinRestV2Url(boolean isTestMode) {
    return isTestMode ? WEBIN_REST_V2_TEST_URL : WEBIN_REST_V2_PROD_URL;
  }

  public static String getWebinAuthUrl(boolean isTestMode) {
    return isTestMode ? WEBIN_AUTH_TEST_URL : WEBIN_AUTH_PROD_URL;
  }

  public static String getBiosamplesUrl(boolean isTestMode) {
    return isTestMode ? BIOSAMPLES_TEST_URL : BIOSAMPLES_PROD_URL;
  }
}
