package org.openapitools.codegen.languages;

import io.swagger.models.HttpMethod;
import java.util.List;

public class AzureOperationExtension {
  public String path;
  public HttpMethod method;
  public List<AzureParameterExtension> extensions;

  public AzureOperationExtension() {
  }

  public AzureOperationExtension(String path, HttpMethod method, List<AzureParameterExtension> extensions) {
    this.path = path;
    this.method = method;
    this.extensions = extensions;
  }

  @Override
  public String toString() {
    return "OperationExtension{" +
      "path='" + path + '\'' +
      ", method=" + method +
      ", extensions=" + extensions +
      '}';
  }
}
