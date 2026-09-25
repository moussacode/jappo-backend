package sn.jappo.jappo_backend.abonnement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "paydunya")
@Getter
@Setter
public class PaydunyaProperties {

    private String masterKey;
    private String privateKey;
    private String token;

    private String baseUrl;

    private String callbackUrl;
    private String returnUrl;
    private String cancelUrl;
}