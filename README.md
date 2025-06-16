# Manual de Operación y Mantenimiento

## Arquitectura Implementada

Este proyecto es una aplicación de microservicios basada en el ejemplo de [bortizf](https://github.com/bortizf/microservice-app-example). Se realizaron modificaciones para implementar patrones de nube como API Gateway y limitación de tasa (rate limiting), y para permitir el despliegue en Azure mediante pipelines de infraestructura y desarrollo.

![image](https://github.com/user-attachments/assets/7ba03009-f55b-4fb0-a5e2-1bd406691fdf)

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
3.  En la raíz del proyecto, ejecuta el siguiente comando para levantar todos los servicios:
    ```bash
    docker-compose up -d
    ```
4.  La aplicación estará accesible a través del puerto 80 (nginx).

### Local (Minikube)

1.  Asegúrate de tener Minikube y kubectl instalados.
2.  Inicia Minikube:
    ```bash
    minikube start
    ```
3.  Aplica los manifiestos de Kubernetes ubicados en la carpeta `k8s/`:
    ```bash
    kubectl apply -f k8s/
    ```
4.  Obtén la IP del servicio `nginx-proxy` para acceder a la aplicación:
    ```bash
    minikube service nginx-proxy --url
    ```
    Esto te dará una URL para acceder a la aplicación. Generalmente, Nginx estará expuesto en el puerto 80.

### Azure (Mediante Azure Pipelines)

El despliegue en Azure se gestiona a través de Azure Pipelines.

1.  **Configuración Inicial:**
    *   Se requiere un pipeline de infraestructura ([infrastructure repository](https://github.com/Salazq/microservice-app-example-deployments)) que utiliza Terraform para aprovisionar una máquina virtual en Azure y Ansible para instalar Docker y clonar el repositorio.
    *   Se definen pipelines de desarrollo para cada microservicio y para Nginx. Estos pipelines construyen la aplicación, acceden a la VM vía SSH, detienen el contenedor correspondiente, lo reconstruyen y lo vuelven a levantar. El pipeline principal para la aplicación se encuentra en `azure-pipelines.yml`.

2.  **Variables de Pipeline:**
    *   Crea un grupo de variables en Azure DevOps llamado `variable-group-taller`.
    *   Define las siguientes variables en el grupo:
        *   `AZURE_ACCOUNT`
        *   `RESOURCE_GROUP`
        *   `VM_NAME`
        *   `VM_PASSWORD`
        *   `VM_USERNAME`

3.  **Ejecución:**
    *   Ejecuta el pipeline `Infrastructure-up` para desplegar la infraestructura y la aplicación.
    *   Para remover la infraestructura, ejecuta el pipeline `Infrastructure-down`.
    *   Cada vez que se realice un cambio en un microservicio, el pipeline correspondiente se disparará automáticamente (si está configurado con triggers para las ramas adecuadas).

## Dashboards y Puertos de Acceso

*   **Aplicación Principal (Nginx)**:
    *   Docker Engine: `http://localhost:80`
    *   Minikube: IP y puerto obtenidos con `minikube service nginx-proxy --url` (generalmente puerto 80).
    *   Azure: IP pública de la VM en el puerto 80.
*   **Zipkin (Trazabilidad)**:
    *   Docker Engine: `http://localhost:9411`
    *   Minikube: `kubectl port-forward svc/zipkin 9411:9411` y luego accede a `http://localhost:9411`.
    *   Azure: No expuesto públicamente por defecto, accesible desde dentro de la red virtual.
*   **Service Discovery (Eureka)**:
    *   Docker Engine: `http://localhost:8761`
    *   Minikube: `kubectl port-forward svc/service-discovery 8761:8761` y luego accede a `http://localhost:8761`.
    *   Azure: No expuesto públicamente por defecto.
*   **API Gateway (Spring Cloud Gateway)**:
    *   Docker Engine: `http://localhost:8080` (acceso directo, aunque se recomienda a través de Nginx).
*   **Otros Microservicios (acceso directo para depuración, no recomendado para uso normal)**:
    *   `cloud-config`: `http://localhost:9296`
    *   `proxy-client`: `http://localhost:8900`
    *   `order-service`: `http://localhost:8300`
    *   `product-service`: `http://localhost:8500`
    *   `user-service`: `http://localhost:8700`
    *   (Los puertos para `favourite-service`, `payment-service`, `shipping-service` se pueden encontrar en sus respectivos `compose.yml` o `deployment.yaml` si se exponen directamente, aunque típicamente se accede a ellos a través del API Gateway).

## Ejemplos de Consulta (a través de Nginx)

Asumiendo que Nginx está en `http://localhost` (o la IP correspondiente en Minikube/Azure).

*   **Obtener todos los productos:**
    ```
    GET /product-service/api/v1/products
    ```
*   **Crear un usuario:**
    ```
    POST /user-service/api/v1/users
    Content-Type: application/json

    {
      "name": "John Doe",
      "email": "john.doe@example.com",
      "password": "securepassword"
    }
    ```
*   **Realizar un pedido:**
    ```
    POST /order-service/api/v1/orders
    Content-Type: application/json

    {
      "userId": 1,
      "productIds": [101, 102],
      "totalAmount": 150.75
    }
    ```
    *(Nota: Los endpoints exactos y los cuerpos de las solicitudes pueden variar. Consulta la documentación o el código de cada microservicio para obtener detalles precisos. Las rutas a través de Nginx dependerán de la configuración del API Gateway y de Nginx).*

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
    2.  Asegúrate de que Minikube pueda acceder a tus imágenes locales (ej. `eval $(minikube docker-env)` o subiendo a un registro).
    3.  Actualiza el deployment correspondiente:
        ```bash
        kubectl rollout restart deployment/<nombre-del-deployment>
        ```
        O, si cambiaste la etiqueta de la imagen, edita el manifiesto del deployment y aplica los cambios.
*   **Azure**:
    1.  Realiza los cambios en el código y haz push a la rama correspondiente (develop, stage, master).
    2.  El pipeline de Azure DevOps configurado para ese microservicio se disparará automáticamente, construirá la nueva imagen y la desplegará en la VM.

### Escalado

*   **Local (Docker Engine)**:
    ```bash
    docker-compose up -d --scale <nombre-del-servicio>=<numero-de-instancias>
    ```
*   **Local (Minikube)/Azure (AKS si se usara)**:
    Modifica el campo `replicas` en el archivo `deployment.yaml` del servicio y aplica los cambios:
    ```bash
    kubectl apply -f k8s/<servicio>-deployment.yaml
    ```
    O usa el comando:
    ```bash
    kubectl scale deployment/<nombre-del-deployment> --replicas=<numero-de-instancias>
    ```

### Monitorización y Logs

*   **Logs de Contenedores (Docker Engine)**:
    ```bash
    docker-compose logs -f <nombre-del-servicio>
    ```
*   **Logs de Pods (Minikube)**:
    ```bash
    kubectl logs -f <nombre-del-pod>
    ```
    Para ver los logs de un deployment:
    ```bash
    kubectl logs -f deployment/<nombre-del-deployment>
    ```
*   **Trazabilidad (Zipkin)**: Accede al dashboard de Zipkin (ver sección "Dashboards y Puertos de Acceso") para visualizar las trazas de las solicitudes entre microservicios.
*   **Métricas (Prometheus/Grafana - si estuvieran configurados)**: Este proyecto base no incluye Prometheus/Grafana, pero `prometheus-patch.json` y `grafana-config-patch.yaml` sugieren intenciones de integrarlos. Si se implementan, sus dashboards serían los puntos de acceso para métricas.

### Configuración de Nginx (API Gateway y Rate Limiting)

*   Los archivos de configuración de Nginx se encuentran en la carpeta `nginx/`.
*   Para modificar el enrutamiento o las reglas de limitación de tasa, edita los archivos `.conf` correspondientes.
*   Después de modificar la configuración de Nginx:
    *   **Local (Docker Engine)**: Reconstruye y reinicia el contenedor de Nginx:
        ```bash
        docker-compose build nginx
        docker-compose up -d --no-deps nginx
        ```
    *   **Local (Minikube)**: Si la configuración está en un ConfigMap, actualiza el ConfigMap y reinicia los pods de Nginx. Si está en la imagen, reconstruye la imagen y actualiza el deployment.
    *   **Azure**: El pipeline de Nginx debería encargarse de redesplegar los cambios.

### Gestión de Dependencias (Maven)

*   El proyecto utiliza Maven para la gestión de dependencias Java. El archivo principal es `pom.xml` en la raíz y en cada microservicio.
*   Para actualizar dependencias, modifica el `pom.xml` correspondiente y reconstruye el artefacto/imagen.

### Estrategia de Ramificación

*   **Desarrollo (GitFlow)**: Se utiliza para la gestión estructurada de microservicios, con ramas de feature, develop y main.
*   **Operaciones (GitHub Flow)**: Para la infraestructura y procesos de despliegue, con una rama principal `main` y ramas de feature de corta duración.

Este manual proporciona una visión general. Para detalles específicos de cada microservicio o componente, consulta su documentación individual o código fuente.
