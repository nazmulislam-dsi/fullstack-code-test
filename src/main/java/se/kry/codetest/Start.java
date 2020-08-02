package se.kry.codetest;

import io.vertx.core.Vertx;

public class Start {

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle(),event -> {
            if(event.failed()){
                System.exit(1);
            }
        });
    }
}
