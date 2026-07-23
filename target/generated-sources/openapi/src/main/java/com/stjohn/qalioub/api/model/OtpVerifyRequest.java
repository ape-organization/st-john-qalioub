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
 * OtpVerifyRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-23T20:15:53.304222100+03:00[Africa/Cairo]", comments = "Generator version: 7.5.0")
public class OtpVerifyRequest {

  private String phone;

  private String otp;

  public OtpVerifyRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OtpVerifyRequest(String phone, String otp) {
    this.phone = phone;
    this.otp = otp;
  }

  public OtpVerifyRequest phone(String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Phone number used during OTP request
   * @return phone
  */
  @NotNull 
  @Schema(name = "phone", example = "+201234567890", description = "Phone number used during OTP request", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("phone")
  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public OtpVerifyRequest otp(String otp) {
    this.otp = otp;
    return this;
  }

  /**
   * 6-digit OTP received on WhatsApp
   * @return otp
  */
  @NotNull 
  @Schema(name = "otp", example = "123456", description = "6-digit OTP received on WhatsApp", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("otp")
  public String getOtp() {
    return otp;
  }

  public void setOtp(String otp) {
    this.otp = otp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OtpVerifyRequest otpVerifyRequest = (OtpVerifyRequest) o;
    return Objects.equals(this.phone, otpVerifyRequest.phone) &&
        Objects.equals(this.otp, otpVerifyRequest.otp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phone, otp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OtpVerifyRequest {\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    otp: ").append(toIndentedString(otp)).append("\n");
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

