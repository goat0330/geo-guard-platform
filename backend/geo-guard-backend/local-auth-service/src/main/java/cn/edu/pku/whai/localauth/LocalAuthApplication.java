package cn.edu.pku.whai.localauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;

// This gateway relays multipart bytes; the RAG service parses and validates uploads.
@SpringBootApplication(exclude = MultipartAutoConfiguration.class)
public class LocalAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalAuthApplication.class, args);
    }
}
