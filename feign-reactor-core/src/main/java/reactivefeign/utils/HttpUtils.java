/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static reactivefeign.utils.HttpUtils.StatusCodeFamily.CLIENT_ERROR;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.INFORMATIONAL;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.OTHER;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.REDIRECTION;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.SERVER_ERROR;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.SUCCESSFUL;

public final class HttpUtils {

  private HttpUtils(){}

  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  public static final String TEXT = "text/plain";
  public static final String TEXT_UTF_8 = TEXT+";charset=utf-8";

  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF_8 = APPLICATION_JSON+";charset=utf-8";
  public static final String APPLICATION_STREAM_JSON = "application/stream+json";
  public static final String APPLICATION_STREAM_JSON_UTF_8 = APPLICATION_STREAM_JSON+";charset=utf-8";

  public static final String MULTIPART_FORM_DATA = "multipart/form-data";
  public static final String MULTIPART_MIXED = "multipart/mixed";
  public static final String MULTIPART_RELATED = "multipart/related";
  public static final Set<String> MULTIPART_MIME_TYPES = Collections.unmodifiableSet(new HashSet<>(
          Arrays.asList(MULTIPART_FORM_DATA, MULTIPART_MIXED, MULTIPART_RELATED)));

  public static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";

  public static final byte[] NEWLINE_SEPARATOR = {'\n'};
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
  public static final String ACCEPT_HEADER = "Accept";
  public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
  public static final String GZIP = "gzip";

  public static StatusCodeFamily familyOf(final int statusCode) {
    switch (statusCode / 100) {
      case 1:
        return INFORMATIONAL;
      case 2:
        return SUCCESSFUL;
      case 3:
        return REDIRECTION;
      case 4:
        return CLIENT_ERROR;
      case 5:
        return SERVER_ERROR;
      default:
        return OTHER;
    }
  }

  public enum StatusCodeFamily {
    INFORMATIONAL(true), SUCCESSFUL(false), REDIRECTION(true), CLIENT_ERROR(true), SERVER_ERROR(
        true), OTHER(false);

    private final boolean error;

    StatusCodeFamily(boolean error) {
      this.error = error;
    }

    public boolean isError() {
      return error;
    }
  }
}
