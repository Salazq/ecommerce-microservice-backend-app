# Manual de Operación y Mantenimiento

## Integrantes:
- Juan Camilo Salazar
- Samuel Gutiérrez

## Arquitectura Implementada

Este proyecto es una aplicación de microservicios basada en el ejemplo de [SelimHorri](https://github.com/SelimHorri/ecommerce-microservice-backend-app). Se realizaron modificaciones para implementar tres patrones de nube; test unitatios, de integración, E2E, de rendimeinto y seguridad; y para permitir el despliegue local con contenedores de Docekr, minikube y en Azure mediante pipelines de infraestructura y desarrollo.

![image](https://github.com/user-attachments/assets/00ce7511-55c1-45e8-9930-c2be8cd4a80d)

## Componentes del Sistema

El sistema está compuesto por los siguientes microservicios:

*   **api-gateway**: Punto de entrada único para todas las solicitudes del cliente.
*   **cloud-config**: Servidor de configuración para los microservicios.
*   **favourite-service**: Gestiona los productos favoritos de los usuarios.
*   **order-service**: Procesa los pedidos de los usuarios.
*   **payment-service**: Gestiona los pagos de los pedidos.
*   **product-service**: Administra el catálogo de productos.
*   **proxy-client**: Cliente proxy para la comunicación entre servicios.
*   **service-discovery**: Registro y descubrimiento de servicios (Eureka).
*   **shipping-service**: Gestiona el envío de los pedidos.
*   **user-service**: Administra la información de los usuarios.
*   **nginx**: Servidor web utilizado como API Gateway y para la limitación de tasa.
*   **zipkin**: Sistema de trazabilidad distribuida.

Cada microservicio se encuentra en su respectiva carpeta dentro del repositorio y contiene su propio `Dockerfile` para la contenerización.

## Despliegue

### Local (Docker Engine)

1.  Asegúrate de tener Docker y Docker Compose instalados.
2.  Clona este repositorio.
3.  En la raíz del proyecto, ejecuta el script build-all.ps1 para buildear todo, despúes ejecuta el siguiente comando para levantar todos los servicios:
    ```bash
    docker-compose up -d
    ```
4.  La aplicación estará accesible a través del puerto 80 (nginx).

### Local (Minikube)

1.  Asegúrate de tener Docker, Minikube y kubectl instalados.
2.  Inicia Minikube:
    ```bash
    minikube start
    ```
3.  Ejecuta el script build-all.ps1 para buildear todo y aplica los manifiestos de Kubernetes ubicados en la carpeta `k8s/`:
    ```bash
    kubectl apply -f k8s/
    ```
4.  La aplicación estará accesible a través del puerto 80 (nginx).

### Azure 

El despliegue en Azure se gestiona a través de Azure Pipelines.

1.  **Configuración Inicial:**
    *   Se requiere un pipeline para subir la infraestructura, el script para ello se llama up.yml y se encuentra en la carpeta pipeline en [infrastructure repository](https://github.com/Salazq/ecommerce-microservice-backend-app-setup) que utiliza Terraform para crear un cluster de kubernets en aks.
    * Se requiere un pipeline para bajar la infraestructura, el script para ello se llama down.yml y se encuentra en en el mismo lugar que el anterior.
    *   Se definen tres pipelines para cada uno de los ambientes(dev, stage,prod) en la carpeta pipelines de este repositorio, los scripts se llaman dev-full.yml, stage-full.yml y master.yml.
          * Dev: buildea todos los microservicios, ejecuta los test unitarios y los de integración y sube las imagenes a DockerHub con la etiqueta “dev”
          * Stage: realiza lo anterior, además de ejecutar SonarQube y Trivy (más el resto de test que se explican en la siguiente sección), y el deploy de la pods.
          * master (prod): solo se realiza el buildeo, análisis de SonarQube, confirmación manual antes de hacer deploy,  pusheo de imágenes y versionado semántico que se utiliza para taggear las release notes.


2.  **Variables de Pipeline:**
    *   Crea un grupo de variables en Azure DevOps llamado `variable-group-taller`.
    *   Define las siguientes variables en el grupo:
        *   `AZURE_ACCOUNT`
        *   `GITHUB_TOKEN`
    * Define un secure file llamado .grafana con el script de moniotreo generado por configuración de Grafana Cloud.


3.  **Ejecución:**
    *   Para subir la infraestructura, ejecuta la pipeline que contiente `up.yml` (con dev, stage o prod dependiendo del entorno que se va a ejecutar).
    *   Ejecuta la pipelenie que contiene uno de los entornos (dev, stage, master) o espera a que se corran automáticamente al hacer push hacia las ramas del mismo nombre.
    *   Para remover la infraestructura, ejecuta el pipeline que contiene `down.yml`.
  
## Ejecución de tests:
* Unitatios y de integración: `mvnw.cmd verify`
* E2E: `powershell -ExecutionPolicy Bypass -File run-all-tests.ps1 -BaseUrl <IP_TARGET>`
* Locust: `powershell -ExecutionPolicy Bypass -File run-locust.ps1 -TargetIP <IP_TARGET>`

## Dashboards y Puertos de Acceso

*   **Aplicación Principal (Nginx)**:
    *   Docker Engine y Minikube: `http://localhost:80`
    *   : `http://localhost:80`
    *   Azure: IP pública de la VM en el puerto 80.
*   **Zipkin (Trazabilidad)**:
    *   Docker Engine y Minikube: `http://localhost:9411`
    *   Azure: No expuesto públicamente por defecto, accesible desde dentro de la red virtual.
*   **Service Discovery (Eureka)**:
    *   Docker Engine y Minikube: `http://localhost:8761`
    *   Azure:`http://{{ip-pública}}/eureka`
*   **Otros Microservicios (no recomendado para uso normal)**:
    *   `cloud-config`: `http://localhost:9296`
    *   `proxy-client`: `http://localhost:8900`
    *   `order-service`: `http://localhost:8300`
    *   `product-service`: `http://localhost:8500`
    *   `user-service`: `http://localhost:8700`

## Ejemplos de Consulta (a través de Nginx)

Asumiendo que Nginx está en `http://localhost` (o la IP correspondiente en Azure).

*   **Obtener todos los productos:**
    ```
    GET /product-service/api/products
    ```
*   **Obtener todos los usuarios:**
    ```
    GET /user-service/api/users/
    ```
*   **Obtener todas la categorías:**
    ```
    GET /product-service/api/categories
    ```
*   **Crear un usuario:**
    ```
    POST /user-service/api/users
    Content-Type: application/json

    {
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane.smith@test.com",
      "phone": "+1234567890",
      "imageUrl": "http://example.com/image.jpg",
      "credential": {
        "username": "janesmith",
        "password": "password123",
        "roleBasedAuthority": "ROLE_USER",
        "isEnabled": true,
        "isAccountNonExpired": true,
        "isAccountNonLocked": true,
        "isCredentialsNonExpired": true
      }
    }
    ```
*   **Crear una categoría:**
```
POST /product-service/api/categories
Content-Type: application/json

{
    "categoryTitle": "Smartphones",
    "imageUrl": "https://example.com/smartphones.jpg",
    "parentCategory": {
        "categoryId": 1
    }
}
```
*   **Crear un producto:**
```
POST /product-service/api/products
Content-Type: application/json

{
  "productTitle": "Samsung Galaxy S24",
  "imageUrl": "https://example.com/galaxys24.jpg",
  "sku": "SAM24GAL001",
  "priceUnit": 849.99,
  "quantity": 30,
  "category": {
        "categoryId": {{categoryId}},
        "categoryTitle": "{{categoryTitle}}",
        "imageUrl": "{{imageUrl}}"
    }
}
```


## Mantenimiento

### Actualización de Microservicios

*   **Local (Docker Engine)**:
    1.  Realiza los cambios en el código del microservicio.
    2.  Reconstruye la imagen del microservicio específico:
        ```bash
        docker-compose build <nombre-del-servicio>
        ```
    3.  Reinicia el servicio:
        ```bash
        docker-compose up -d --no-deps <nombre-del-servicio>
        ```
*   **Local (Minikube)**:
    1.  Realiza los cambios en el código y reconstruye la imagen Docker localmente.
    2.  Asegúrate de que Minikube pueda acceder a tus imágenes locales.
    3.  Actualiza el deployment correspondiente:
        ```bash
        kubectl rollout restart deployment/<nombre-del-deployment>
        ```
*   **Azure**:
    1.  Realiza los cambios en el código y haz push a la rama correspondiente (develop, stage, master).
    2.  El pipeline de Azure DevOps configurado para ese microservicio se disparará automáticamente, construirá la nueva imagen y la desplegará en la VM.



### Configuración de Nginx (API Gateway y Rate Limiting)

*   Los archivos de configuración de Nginx se encuentran en la carpeta `nginx/`.
*   Para modificar el enrutamiento o las reglas de limitación de tasa, edita los archivos `.conf` correspondientes.
*   Después de modificar la configuración de Nginx:
    *   **Local (Docker Engine)**: Reconstruye y reinicia el contenedor de Nginx:
        ```bash
        docker-compose build nginx
        docker-compose up -d --no-deps nginx
        ```
    *   **Azure**: El pipeline de Nginx debería encargarse de redesplegar los cambios.

