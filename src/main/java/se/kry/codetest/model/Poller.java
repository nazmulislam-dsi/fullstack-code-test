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
public
class Poller {;
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

    public Poller(JsonObject obj) {
        PollerConverter.fromJson(obj, this);
    }

    public Poller(String jsonStr) {
        PollerConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        PollerConverter.toJson(this, json);
        return json;
    }

}
