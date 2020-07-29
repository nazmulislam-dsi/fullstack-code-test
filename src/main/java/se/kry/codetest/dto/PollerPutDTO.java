package se.kry.codetest.dto;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@DataObject(generateConverter = true)
@NoArgsConstructor
@EqualsAndHashCode
@Data
public
class PollerPutDTO {

    private String name;

    public PollerPutDTO(JsonObject obj) {
        PollerPutDTOConverter.fromJson(obj, this);
    }

    public PollerPutDTO(String jsonStr) {
        PollerPutDTOConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        PollerPutDTOConverter.toJson(this, json);
        return json;
    }


}
