# Sistema de Gestión y Facturación Electrónica - ARCA / AFIP

**Universidad Tecnológica Nacional (UTN) - Tecnicatura Universitaria en Programación**  
**Materia:** Programación III  
**Trabajo Práctico:** Operaciones de Escritura en API REST, Reportes y Facturación Electrónica ARCA

---

## 📋 Checklist de Cumplimiento de Requisitos de la Cátedra

| Requisito Solicitado                   |   Estado    | Implementación / Evidencia                                                                                                                           |
| :------------------------------------- | :---------: | :--------------------------------------------------------------------------------------------------------------------------------------------------- |
| **CRUD Factura Venta Completo**        | ✅ Cumplido | Alta, edición, cálculo automático de importes (Neto, IVA, Total).                                                                                    |
| **Anular / Eliminar según reglas**     | ✅ Cumplido | Facturas con número o CAE solo permiten **Anulación Lógica** (`ANULADA`). Si es borrador sin número se permite **Eliminación Física** (Delete).      |
| **Bloqueo tras informar a ARCA**       | ✅ Cumplido | Una vez obtenido el CAE, el backend bloquea cualquier modificación (`OperacionNoPermitidaException` / 409) y el front oculta los botones de edición. |
| **Stack Tecnológico**                  | ✅ Cumplido | **Front:** HTML5 + CSS + TypeScript modular. **Back:** Spring Boot 3 + Java 21 + Spring Data JPA.                                                    |
| **Conexión ARCA / AFIP**               | ✅ Cumplido | Integración real con WSAA (Bouncy Castle + Caché) y WSFE v1 homologado según **Resolución General 5616/2024**.                                       |
| **Filtros de Búsqueda**                | ✅ Cumplido | Filtros dinámicos por N° Factura, Fecha Desde/Hasta, Cliente y si está o no informada a ARCA.                                                        |
| **Paginación en Grilla**               | ✅ Cumplido | Paginación en servidor con DTOs de reporte ejecutivos.                                                                                               |
| **Buscar Cliente / Artículo Explorer** | ✅ Cumplido | Ventanas modales emergentes con buscador en vivo y tabla de selección.                                                                               |
| **Imprimir PDF con CAE y Vto ARCA**    | ✅ Cumplido | Endpoint `/pdf` con generación legal en iText conteniendo letra, CAE, fecha de vencimiento y código de barras.                                       |

---

## ⚙️ Configuración Inicial y Puesta en Marcha

### 1. Base de Datos (PostgreSQL)

El proyecto cuenta con configuración flexible mediante variables de entorno en `application.properties`:

- **Opción A (Recomendada con Docker):**
  ```bash
  docker compose up -d db
  ```

---

## 📌 Descripción del Proyecto

Aplicación web empresarial integral para la emisión, control y autorización de Facturas de Venta en tiempo real.

### Características Principales:

- **Backend Robusto:** Desarrollado con **Spring Boot 3** y **Java 21**, implementando persistencia con **Spring Data JPA / Hibernate**, transacciones atómicas (`@Transactional`), DTOs desacoplados para POST/PUT y reportes ejecutivos con consultas JPQL complejas.
- **Integración con ARCA / AFIP (Homologación):** Comunicación SOAP en vivo con los Web Services oficiales:
  - **WSAA (LoginCms):** Autenticación criptográfica con CMS/PKCS#7 firmado digitalmente mediante **Bouncy Castle**, incluyendo caché local persistente del Ticket de Acceso (Token y Sign) para evitar rate limits.
  - **WSFE v1 (Facturación Electrónica):** Autorización de comprobantes mediante `FECAESolicitar` cumpliendo con la reciente **Resolución General 5616/2024** (`CondicionIVAReceptorId`), y correlatividad numérica automática con `FECompUltimoAutorizado`.
- **Generación de Documentos:** Emisión del comprobante legal en **PDF (iText)** con letra del comprobante (Factura C / B / A), detalle y pie fiscal con **Código de Autorización Electrónico (CAE)** y Fecha de Vencimiento. Reportes consolidados en **Excel (Apache POI Streaming)** y **PDF**.
- **Frontend SPA:** Desarrollado en **HTML5, CSS moderno y TypeScript modular**, consumiendo la API REST con filtros combinados, paginación, cálculo automático de importes y gestión de estados (Emitida, Autorizada, Anulada).

---

## 🚀 Requisitos Previos

Asegúrate de tener instalado en tu equipo:

- **Java 21 JDK** (Eclipse Temurin, OpenJDK u Oracle).
- **Node.js** (v18 o superior) y **npm**.
- **Docker Desktop** (opcional, pero recomendado para la base de datos) o **PostgreSQL 14+** local.

---

## ⚙️ Configuración Inicial y Puesta en Marcha

### 1. Base de Datos (PostgreSQL)

#### Opción A (Recomendada con Docker):

El proyecto incluye un `docker-compose.yml` listo para usar:

```bash
docker compose up -d db
```

_Esto iniciará PostgreSQL en el puerto `5432` con usuario `postgres`, contraseña `postgres` y la base de datos `tp_spring` creada automáticamente._

#### Opción B (PostgreSQL local en Windows / Linux):

Si prefieres usar tu propio motor local de PostgreSQL o PgAdmin:

1. Crea una base de datos llamada `tp_spring`.
2. En `src/main/resources/application.properties`, ajusta la contraseña:
   ```properties
   spring.datasource.password=TU_PASSWORD_AQUI
   ```

---

### 2. Puesta en Marcha del Backend (Spring Boot)

1. Abre el proyecto en tu IDE preferido (**NetBeans**, **IntelliJ IDEA** o **VS Code**).
2. Si usas consola, puedes compilar y ejecutar con el Maven Wrapper:
   ```bash
   ./mvnw clean spring-boot:run
   ```
   _(En Windows CMD / PowerShell: `.\mvnw.cmd clean spring-boot:run`)_.
3. El servidor iniciará en el puerto **`8080`**.  
   _Al iniciar por primera vez, el componente `DataSeeder` cargará automáticamente clientes, productos, listas de precios y puntos de venta de prueba._

---

### 3. Puesta en Marcha del Frontend (TypeScript / Node.js)

1. Abre una terminal en la raíz del proyecto (`SpringTP`).
2. Instala las dependencias de Node.js:
   ```bash
   npm install
   ```
3. Compila el código TypeScript a JavaScript:
   ```bash
   npx tsc
   ```
4. Inicia el servidor web local con proxy:
   ```bash
   node server.cjs
   ```
5. Abre tu navegador en:
   👉 **`http://localhost:3000`**

---

## 🏛️ Integración con ARCA / AFIP (Testing)

El proyecto viene **100% preconfigurado con credenciales criptográficas de homologación** ubicadas en la carpeta `certs/`:

- `privada_mi_cuit.key`: Clave privada RSA.
- `certificado_mi_cuit.crt`: Certificado digital X.509 homologado por ARCA.

### ¿Cómo probar la emisión en vivo de una Factura con CAE?

1. En la web (`http://localhost:3000`), ve a **Nueva Factura de Venta**.
2. Deja el **Número Factura vacío** (para que se emita como borrador correlativo).
3. Selecciona un Cliente (ej. _Consumidor Final_).
4. En **Punto de Venta**, selecciona **`Sucursal Norte (Punto de Venta 2)`** _(autorizado para el Web Service)_.
5. Agrega uno o más artículos al detalle y haz clic en **Crear Factura**.
6. En la tabla de facturas abajo, haz clic en **Ver / Gestionar** sobre la factura creada.
7. Haz clic en el botón verde:  
   **`🏛️ Informar a ARCA`**.
8. **Resultado:** El sistema conectará con ARCA, validará la factura y te devolverá el **CAE de 14 dígitos** y el **Número oficial de comprobante** asignado secuencialmente.
9. Haz clic en **`📄 Imprimir Factura (PDF)`** para ver la factura legal emitida con CAE y código de barras.

---

## 📡 Endpoints Principales de la API REST

Base URL: `http://localhost:8080/api/facturas`

| Método     | Endpoint                           | Descripción                                                                                      |
| :--------- | :--------------------------------- | :----------------------------------------------------------------------------------------------- |
| **GET**    | `/api/facturas`                    | Consulta paginada y con filtros (`numeroFactura`, `cliente`, `fechaDesde`, `estado`, etc.).      |
| **GET**    | `/api/facturas/{id}`               | Obtiene el detalle completo de una factura específica.                                           |
| **POST**   | `/api/facturas`                    | Crea una nueva factura con detalles (calcula neto e IVA automáticamente). Retorna `201 Created`. |
| **PUT**    | `/api/facturas/{id}`               | Actualiza cabecera y detalles de una factura borrador. Retorna `200 OK`.                         |
| **PUT**    | `/api/facturas/{id}/informar-arca` | Envía la factura a ARCA (WSFE), obtiene CAE y la autoriza.                                       |
| **PUT**    | `/api/facturas/{id}/anular`        | Anulación lógica de la factura (`estado = ANULADA`).                                             |
| **DELETE** | `/api/facturas/{id}`               | Eliminación física (solo permitido si es borrador sin número oficial ni CAE).                    |
| **GET**    | `/api/facturas/{id}/pdf`           | Genera y descarga el PDF individual legal con CAE y Vencimiento.                                 |
| **GET**    | `/api/facturas/reporte.pdf`        | Descarga el reporte ejecutivo consolidado en PDF.                                                |
| **GET**    | `/api/facturas/reporte.xlsx`       | Descarga el reporte consolidado en Excel (Apache POI SXSSF).                                     |
