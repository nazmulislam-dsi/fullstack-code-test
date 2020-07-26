package se.kry.codetest.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public
class Poller {
    private long id;
    private String name;
    private String url;
    private String status;
    private String userId;

    public Poller(long id, String name, String url, String status, String userId) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.status = status;
        this.userId = userId;
    }

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
