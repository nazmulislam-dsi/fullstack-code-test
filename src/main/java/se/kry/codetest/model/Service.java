package se.kry.codetest.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;
import java.time.Instant;

@DataObject(generateConverter = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
public
class Service {;
    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private long id;

    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String name;

    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String url;

    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String status;

    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String userId;

    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private Instant createdDate;

    public Service(JsonObject obj) {
        ServiceConverter.fromJson(obj, this);
    }

    public Service(String jsonStr) {
        ServiceConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ServiceConverter.toJson(this, json);
        return json;
    }

}
