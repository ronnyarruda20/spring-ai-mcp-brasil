package io.github.ronnyarruda20.mcpbrasil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class McpBrasilApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpBrasilApplication.class, args);
    }
}
