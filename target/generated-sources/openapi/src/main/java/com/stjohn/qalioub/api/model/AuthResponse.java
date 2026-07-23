package com.stjohn.qalioub.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.stjohn.qalioub.api.model.UserDto;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AuthResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-23T20:15:53.304222100+03:00[Africa/Cairo]", comments = "Generator version: 7.5.0")
public class AuthResponse {

  private String token;

  private Boolean firstLogin;

  private UserDto user;

  public AuthResponse token(String token) {
    this.token = token;
    return this;
  }

  /**
   * JWT access token
   * @return token
  */
  
  @Schema(name = "token", description = "JWT access token", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("token")
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public AuthResponse firstLogin(Boolean firstLogin) {
    this.firstLogin = firstLogin;
    return this;
  }

  /**
   * True if this is the user's first login (name not yet set)
   * @return firstLogin
  */
  
  @Schema(name = "firstLogin", description = "True if this is the user's first login (name not yet set)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("firstLogin")
  public Boolean getFirstLogin() {
    return firstLogin;
  }

  public void setFirstLogin(Boolean firstLogin) {
    this.firstLogin = firstLogin;
  }

  public AuthResponse user(UserDto user) {
    this.user = user;
    return this;
  }

  /**
   * Get user
   * @return user
  */
  @Valid 
  @Schema(name = "user", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("user")
  public UserDto getUser() {
    return user;
  }

  public void setUser(UserDto user) {
    this.user = user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthResponse authResponse = (AuthResponse) o;
    return Objects.equals(this.token, authResponse.token) &&
        Objects.equals(this.firstLogin, authResponse.firstLogin) &&
        Objects.equals(this.user, authResponse.user);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, firstLogin, user);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthResponse {\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    firstLogin: ").append(toIndentedString(firstLogin)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
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

