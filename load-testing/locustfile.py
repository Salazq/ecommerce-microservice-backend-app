from locust import HttpUser, task, between

class ProxyClientUser(HttpUser):
    wait_time = between(1, 5)  # Wait time between tasks

    # Define the base host for the proxy-client service
    # The API Gateway routes /app/** to the proxy-client
    host = "http://localhost:8080/"

    @task
    def get_users(self):
        self.client.get("user-service/api/users")

    @task
    def get_products(self):
        self.client.get("product-service/api/products")

    @task
    def get_categories(self):
        self.client.get("product-service/api/categories")

    @task
    def get_orders(self):
        self.client.get("order-service/api/orders")

    @task
    def get_credentials(self):
        self.client.get("user-service/api/credentials")
