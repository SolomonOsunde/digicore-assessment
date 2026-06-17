package com.example.digicoreassessment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Reconciliation Service")
                        .version("1.0.0")
                        .description("""
                                Compares internal payment records against a provider's settlement batch and surfaces discrepancies.

                                **Workflow:**
                                1. Register internal payments via `POST /internal/payments`
                                2. Upload the provider's settlement batch via `POST /provider/settlements`
                                3. Run reconciliation via `POST /reconciliation/run`
                                4. Review results via `GET /reconciliation/latest`

                                **Status compatibility rules:**
                                - Internal `SUCCESS` is compatible with provider `SETTLED`
                                - Internal `FAILED` is compatible with provider `REVERSED`
                                - Internal `PENDING` is compatible with provider `PENDING`
                                - Any other combination is a `STATUS_MISMATCH`
                                """));
    }
}
