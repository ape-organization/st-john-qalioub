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
 * OtpRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-23T20:15:53.304222100+03:00[Africa/Cairo]", comments = "Generator version: 7.5.0")
public class OtpRequest {

  private String phone;

  public OtpRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OtpRequest(String phone) {
    this.phone = phone;
  }

  public OtpRequest phone(String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Phone number in international format
   * @return phone
  */
  @NotNull 
  @Schema(name = "phone", example = "+201234567890", description = "Phone number in international format", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("phone")
  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OtpRequest otpRequest = (OtpRequest) o;
    return Objects.equals(this.phone, otpRequest.phone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OtpRequest {\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
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

