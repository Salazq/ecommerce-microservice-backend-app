package com.selimhorri.app;

import com.selimhorri.app.config.features.FeatureToggleProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties(FeatureToggleProperties.class) // Ensure Spring processes our properties class
public class OrderServiceApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}
	
	// Simple test bean to print feature toggle values on startup
	@Bean
	CommandLineRunner commandLineRunner(FeatureToggleProperties featureToggles) {
		return args -> {
			System.out.println("**************** FEATURE TOGGLES ****************");
			System.out.println("New Checkout Process Enabled: " + featureToggles.getNewCheckoutProcess().isEnabled());
			System.out.println("Experimental Dashboard Enabled: " + featureToggles.getExperimentalDashboard().isEnabled());
			System.out.println("***************************************************");
		};
	}
	
}

@RestController
class OrderController {

	private final FeatureToggleProperties featureToggles;

	@Autowired
    public OrderController(FeatureToggleProperties featureToggles) {
        this.featureToggles = featureToggles;
    }

    @GetMapping
	public String msg() {
		String newCheckoutMsg = "New Checkout: " + (featureToggles.getNewCheckoutProcess().isEnabled() ? "ON" : "OFF");
		String experimentalDashboardMsg = "Experimental Dashboard: " + (featureToggles.getExperimentalDashboard().isEnabled() ? "ON" : "OFF");
		return "Order controller responding!! <br/>" + newCheckoutMsg + " <br/>" + experimentalDashboardMsg;
	}
	
}






