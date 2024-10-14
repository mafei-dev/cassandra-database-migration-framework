package org.demo.controller;

import com.datastax.oss.driver.api.core.CqlSession;
import org.demo.util.DBValidator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class TestController {

    private final CqlSession cqlSession;

    public TestController(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }


    @RequestMapping("/test")
    public void test() {
        System.out.println("TestController.test");
        CompletableFuture.runAsync(() -> {
            try (final DBValidator dbValidator = new DBValidator(cqlSession, UUID.randomUUID().toString())) {
                dbValidator.validate();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
