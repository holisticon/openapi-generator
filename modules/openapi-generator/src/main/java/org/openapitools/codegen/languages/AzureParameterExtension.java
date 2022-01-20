package org.openapitools.codegen.languages;

public class AzureParameterExtension {
  public String parameterName;
  public String parameterType;

  public AzureParameterAnnotation annotation;

  public AzureParameterExtension(
  ) {
  }

  public AzureParameterExtension(String parameterName, String parameterType, AzureParameterAnnotation annotation) {
    this.parameterName = parameterName;
    this.parameterType = parameterType;
    this.annotation = annotation;
  }
}
