package com.selimhorri.app.config.features;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component; // Or use @EnableConfigurationProperties on a @Configuration class

@Component // Make it a Spring bean
@ConfigurationProperties(prefix = "features")
public class FeatureToggleProperties {

    private Toggle newCheckoutProcess = new Toggle();
    private Toggle experimentalDashboard = new Toggle();

    public Toggle getNewCheckoutProcess() {
        return newCheckoutProcess;
    }

    public void setNewCheckoutProcess(Toggle newCheckoutProcess) {
        this.newCheckoutProcess = newCheckoutProcess;
    }

    public Toggle getExperimentalDashboard() {
        return experimentalDashboard;
    }

    public void setExperimentalDashboard(Toggle experimentalDashboard) {
        this.experimentalDashboard = experimentalDashboard;
    }

    public static class Toggle {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}
