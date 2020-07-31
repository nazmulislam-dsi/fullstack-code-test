package se.kry.codetest.dto;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;

@DataObject(generateConverter = true)
@NoArgsConstructor
@EqualsAndHashCode
@Data
public
class ServicePutDTO {
    @Accessors(chain = true)
    @Setter(onMethod = @__({@Fluent}))
    private String name;

    public ServicePutDTO(JsonObject obj) {
        ServicePutDTOConverter.fromJson(obj, this);
    }

    public ServicePutDTO(String jsonStr) {
        ServicePutDTOConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ServicePutDTOConverter.toJson(this, json);
        return json;
    }


}
