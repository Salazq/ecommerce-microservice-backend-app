package com.selimhorri.app.e2e.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for E2E tests
 */
@Configuration
@ConfigurationProperties(prefix = "e2e")
public class E2ETestProperties {

    private ApiGateway apiGateway = new ApiGateway();
    private Test test = new Test();
    private Users users = new Users();

    public ApiGateway getApiGateway() {
        return apiGateway;
    }

    public void setApiGateway(ApiGateway apiGateway) {
        this.apiGateway = apiGateway;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public static class ApiGateway {
        private String host = "localhost";        private int port = 8080;
        private String baseUrl = "http://localhost:8080";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Test {
        private Timeout timeout = new Timeout();
        private Retry retry = new Retry();
        private Cleanup cleanup = new Cleanup();
        private Auth auth = new Auth();

        public Timeout getTimeout() {
            return timeout;
        }

        public void setTimeout(Timeout timeout) {
            this.timeout = timeout;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public Cleanup getCleanup() {
            return cleanup;
        }

        public void setCleanup(Cleanup cleanup) {
            this.cleanup = cleanup;
        }

        public Auth getAuth() {
            return auth;
        }

        public void setAuth(Auth auth) {
            this.auth = auth;
        }

        public static class Timeout {
            private int defaultTimeout = 30;
            private int authentication = 10;
            private int apiCalls = 15;

            public int getDefaultTimeout() {
                return defaultTimeout;
            }

            public void setDefaultTimeout(int defaultTimeout) {
                this.defaultTimeout = defaultTimeout;
            }

            public int getAuthentication() {
                return authentication;
            }

            public void setAuthentication(int authentication) {
                this.authentication = authentication;
            }

            public int getApiCalls() {
                return apiCalls;
            }

            public void setApiCalls(int apiCalls) {
                this.apiCalls = apiCalls;
            }
        }

        public static class Retry {
            private int maxAttempts = 3;
            private int delaySeconds = 2;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public int getDelaySeconds() {
                return delaySeconds;
            }

            public void setDelaySeconds(int delaySeconds) {
                this.delaySeconds = delaySeconds;
            }
        }

        public static class Cleanup {
            private boolean enabled = true;
            private boolean onFailure = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isOnFailure() {
                return onFailure;
            }

            public void setOnFailure(boolean onFailure) {
                this.onFailure = onFailure;
            }
        }

        public static class Auth {
            private int tokenExpiryBufferMinutes = 5;

            public int getTokenExpiryBufferMinutes() {
                return tokenExpiryBufferMinutes;
            }

            public void setTokenExpiryBufferMinutes(int tokenExpiryBufferMinutes) {
                this.tokenExpiryBufferMinutes = tokenExpiryBufferMinutes;
            }
        }
    }

    public static class Users {
        private Admin admin = new Admin();
        private TestUser test = new TestUser();

        public Admin getAdmin() {
            return admin;
        }

        public void setAdmin(Admin admin) {
            this.admin = admin;
        }

        public TestUser getTest() {
            return test;
        }

        public void setTest(TestUser test) {
            this.test = test;
        }

        public static class Admin {
            private String username = "admin";
            private String password = "admin123";
            private String email = "admin@test.com";

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }
        }

        public static class TestUser {
            private String prefix = "test-user-";
            private String domain = "test.com";
            private String defaultPassword = "Password123!";

            public String getPrefix() {
                return prefix;
            }

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }

            public String getDomain() {
                return domain;
            }

            public void setDomain(String domain) {
                this.domain = domain;
            }

            public String getDefaultPassword() {
                return defaultPassword;
            }

            public void setDefaultPassword(String defaultPassword) {
                this.defaultPassword = defaultPassword;
            }
        }
    }
}
