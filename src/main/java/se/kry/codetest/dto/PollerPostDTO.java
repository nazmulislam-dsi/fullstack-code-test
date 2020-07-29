package se.kry.codetest.dto;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@DataObject(generateConverter = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
public
class PollerPostDTO {

    private String name;
    private String url;
    private String status;

    public PollerPostDTO(JsonObject obj) {
        PollerPostDTOConverter.fromJson(obj, this);
    }

    public PollerPostDTO(String jsonStr) {
        PollerPostDTOConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        PollerPostDTOConverter.toJson(this, json);
        return json;
    }


}
