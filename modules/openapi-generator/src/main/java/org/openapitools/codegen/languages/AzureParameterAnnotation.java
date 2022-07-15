package org.openapitools.codegen.languages;

import java.util.Map;

public class AzureParameterAnnotation {
  public String type;
  public Map<String, Object> parameters;

  public AzureParameterAnnotation() {
  }

  public AzureParameterAnnotation(String type, Map<String, Object> parameters) {
    this.type = type;
    this.parameters = parameters;
  }

  @Override
  public String toString() {
    return "AzureParameterAnnotation{" +
      "type='" + type + '\'' +
      ", parameters=" + parameters +
      '}';
  }
}
