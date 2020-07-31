package se.kry.codetest.dto;

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
class ServicePostDTO {
    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String name;
    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String url;

    public ServicePostDTO(JsonObject obj) {
        ServicePostDTOConverter.fromJson(obj, this);
    }

    public ServicePostDTO(String jsonStr) {
        ServicePostDTOConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ServicePostDTOConverter.toJson(this, json);
        return json;
    }


}
