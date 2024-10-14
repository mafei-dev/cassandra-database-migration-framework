package org.demo.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

@Configuration
public class SessionConfig {
    private static final Logger log = LoggerFactory.getLogger(SessionConfig.class);
    @Value("classpath:cassandra.conf")
    private Resource resource;


    @Bean
    public CqlSession getCqlSession() throws IOException {
        try {
            String propertiesAsString = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
            DriverConfigLoader driverConfigLoader = DriverConfigLoader.fromString(
                    propertiesAsString
            );
            if (log.isDebugEnabled()) {
                driverConfigLoader.getInitialConfig().getProfiles().forEach((s, driverExecutionProfile) -> {
                    driverExecutionProfile.entrySet().forEach(stringObjectEntry -> {
                        Object value = stringObjectEntry.getValue();
                        System.out.println("Profile:" + s + " config -> " + stringObjectEntry.getKey() + " = " + value);
                    });
                });
            }
            return CqlSession
                    .builder()
                    .withConfigLoader(driverConfigLoader)
                    .build();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("demo-cassandra.conf file not found in the classpath for configuring demo cassandra CqlSession.", e);
        }
    }
}
