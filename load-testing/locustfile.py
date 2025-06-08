from locust import HttpUser, task, between
import json
import os

class ProxyClientUser(HttpUser):
    wait_time = between(1, 5)

    # Usar PUBLIC_IP si está disponible, sino usar localhost
    public_ip = os.environ.get('PUBLIC_IP', 'localhost')
    host = f"http://{public_ip}:8080/"
    
    def generate_unique_user_data(self):
        return {
            "firstName": "LoadTest",
            "lastName": "User",
            "email": "loadtest.user@test.com",
            "phone": "+1234567890",
            "imageUrl": "http://example.com/image.jpg",
            "credential": {
                "username": "loadtestuser",
                "password": "password123",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True
            }
        }    
        
    @task(2)  
    def create_user(self):
        user_data = self.generate_unique_user_data()
        
        headers = {
            "Content-Type": "application/json"
        }
        
        response = self.client.post(
            "user-service/api/users",
            data=json.dumps(user_data),
            headers=headers,
            name="Create User"
        )

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

    @task
    def getShippings(self):
        self.client.get("/shipping-service/api/shippings/")
    
    @task
    def getPayments(self):
        self.client.get("/payment-service/api/payments/")
