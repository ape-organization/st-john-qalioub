package com.stjohn.qalioub.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateNameRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-23T20:15:53.304222100+03:00[Africa/Cairo]", comments = "Generator version: 7.5.0")
public class UpdateNameRequest {

  private String name;

  public UpdateNameRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateNameRequest(String name) {
    this.name = name;
  }

  public UpdateNameRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The user's full name
   * @return name
  */
  @NotNull 
  @Schema(name = "name", example = "John Doe", description = "The user's full name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateNameRequest updateNameRequest = (UpdateNameRequest) o;
    return Objects.equals(this.name, updateNameRequest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateNameRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

