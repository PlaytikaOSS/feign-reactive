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

import static reactivefeign.utils.HttpUtils.StatusCodeFamily.*;

public final class HttpUtils {

  private HttpUtils(){}

  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  public static final String TEXT = "text/plain";
  public static final String TEXT_UTF_8 = TEXT+";charset=utf-8";

  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF_8 = APPLICATION_JSON+";charset=utf-8";
  public static final String APPLICATION_STREAM_JSON = "application/stream+json";
  public static final String APPLICATION_STREAM_JSON_UTF_8 = APPLICATION_STREAM_JSON+";charset=utf-8";


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
