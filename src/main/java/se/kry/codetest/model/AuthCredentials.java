package se.kry.codetest.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;
import se.kry.codetest.utils.HashUtils;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DataObject(generateConverter = true)
@Data
public class AuthCredentials {

  @Accessors(chain = true)
  @Setter(onMethod = @__({@Fluent}))
  private String username;
  @Accessors(chain = true)
  @Setter(onMethod = @__({@Fluent}))
  private String password;

  public AuthCredentials(JsonObject json) {
    AuthCredentialsConverter.fromJson(json, this);
  }

  public AuthCredentials(String jsonStr) {
    AuthCredentialsConverter.fromJson(new JsonObject(jsonStr), this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AuthCredentialsConverter.toJson(this, json);
    return json;
  }

  @GenIgnore
  @Fluent
  public AuthCredentials hashPassword() {
    this.setPassword(HashUtils.createHash(this.getPassword()));
    return this;
  }
}
