package se.kry.codetest.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;

@DataObject(generateConverter = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
public class User {

    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private long id;
    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String username;
    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String password;

    public User(JsonObject obj) {
        UserConverter.fromJson(obj, this);
    }

    public User(String jsonStr) {
        UserConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UserConverter.toJson(this, json);
        return json;
    }
}
