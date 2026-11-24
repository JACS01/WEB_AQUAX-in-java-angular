# Diagnosticar la API de AquaX

Primero identifica el tramo que falla: cliente → CORS → autenticación → permisos → validación → servicio → base de datos. Reproduce la misma solicitud en Thunder Client con su método, URL, autenticación y JSON; compara después el estado, los headers y el cuerpo de respuesta.

## Contrato de errores

Los errores de la API utilizan `Content-Type: application/problem+json` y los campos `status`, `title`, `detail` e `instance`. `instance` identifica la ruta; no incluye los parámetros de consulta. Una validación de campos agrega `errores`, con elementos `campo` y `mensaje`. El campo estándar `type` puede aparecer, según la serialización de ProblemDetail.

Ejemplo de autenticación ausente:

```json
{
  "status": 401,
  "title": "No autorizado",
  "detail": "Se requieren credenciales válidas para acceder a este recurso.",
  "instance": "/api/personal/pedidos"
}
```

Los detalles de un `400` de validación o un `409` de negocio pueden ser más específicos. Un `500` utiliza un mensaje público genérico; el diagnóstico técnico se consulta en la consola de Spring.

| Resultado | Qué revisar | Respuesta del futuro Angular |
| --- | --- | --- |
| 200 con lista o página vacía | Puede ser correcto: no hay registros que cumplan el filtro. | Mostrar estado vacío. |
| 400 | Tipos JSON, campos obligatorios, parámetros y rango de fechas. `page` comienza en 0; `size` debe ser positivo. | Mostrar `detail` y los mensajes de `errores` junto a los campos. |
| 401 | Credenciales, cuenta activa, token vigente y cambio reciente de contraseña. | Revisar la sesión; volver a iniciar sesión cuando corresponda. |
| 403 | La cuenta está autenticada, pero su rol no permite la operación. Un rechazo CORS también puede responder 403. | Distinguir permiso insuficiente de un bloqueo CORS en Network; no cerrar automáticamente una sesión válida por falta de rol. |
| 404 | Ruta e identificador del recurso. | Informar que el recurso no existe. |
| 405 | Método HTTP de la solicitud. | Corregir el método usado por el servicio. |
| 409 | Usuario, correo, DNI o vínculo duplicados; reglas como conservar un administrador activo. | Mostrar el conflicto y permitir corregir los datos. |
| 415 | `Content-Type` y formato del cuerpo. | Enviar JSON para los endpoints JSON. |
| 500 | Consola del servidor y clase/lugar del fallo registrado. | Informar un fallo del servidor. Evitar repetir automáticamente una creación. |
| 503 en login | Configuración de `AQUAX_JWT_SECRET`. | Solicitar corregir la configuración del servidor. |
| Status 0 en el cliente | Servidor accesible, URL, HTTPS y bloqueo CORS. No es un código HTTP emitido por Spring. | Terminar el estado de carga y mostrar un error de conexión. |

## Pruebas rápidas con Thunder Client

Después de actualizar el proyecto, detén y vuelve a ejecutar AquaX. Usa una cuenta existente y su contraseña actual; con JWT, pega únicamente el token en **Auth > Bearer**.

| Método y URL | Autenticación | Resultado esperado |
| --- | --- | --- |
| `GET http://localhost:8080/api/productos` | None | 200 con el catálogo. |
| `GET http://localhost:8080/api/personal/pedidos` | None | 401 con el contrato común. |
| `GET http://localhost:8080/api/auth/me` | Bearer con el texto `token-invalido` | 401 con la ruta en `instance`. |
| `GET http://localhost:8080/api/admin/usuarios` | Cuenta con rol VENDEDOR | 403 con el contrato común. |
| `GET http://localhost:8080/api/admin/usuarios/pagina?page=abc` | Administrador | 400 con una explicación en español del parámetro inválido. |
| `GET http://localhost:8080/api/admin/empleados/999999999` | Administrador | 404, siempre que ese empleado no exista. |

No es necesario provocar un fallo real del servidor para comprobar el `500`: las pruebas automatizadas incluyen un fallo controlado disponible solo en el contexto de prueba.

## CORS y preflight

El origen permitido por defecto es `http://localhost:4200`. Un origen incluye protocolo, host y puerto: `http://127.0.0.1:4200` es diferente. Para admitir otro origen, configura `AQUAX_CORS_ALLOWED_ORIGINS`; admite valores separados por comas.

En Thunder Client puedes inspeccionar el preflight con **OPTIONS** a `http://localhost:8080/api/personal/pedidos/1/pago`, **Auth > None**, cuerpo vacío y estos headers:

```text
Origin: http://localhost:4200
Access-Control-Request-Method: PATCH
Access-Control-Request-Headers: authorization,content-type
```

Debe responder satisfactoriamente y devolver `Access-Control-Allow-Origin`, los métodos admitidos y los headers permitidos. No necesita un token ni un pedido existente porque el preflight comprueba permisos de comunicación, no ejecuta la operación PATCH.

En el navegador, revisa **DevTools > Network**:

1. Identifica si salió `OPTIONS` y si fue aceptado.
2. Comprueba la URL y el origen exactos.
3. Revisa `Authorization: Bearer ...` en la solicitud real.
4. Comprueba `Access-Control-Allow-Origin` también en una respuesta 400, 401 o 403.
5. Para descargar CSV, usa una respuesta de archivo y lee `Content-Disposition`; el servidor expone ese header.

Thunder Client permite comprobar las cabeceras, pero no impone las restricciones de CORS de un navegador. La comprobación completa con Angular se hará al construir el frontend. Un origen rechazado puede recibir una respuesta CORS de infraestructura sin el contrato JSON de la API; el navegador precisamente impide que ese origen la lea.

Si un POST salió pero Angular no pudo leer su respuesta, comprueba el listado antes de repetirlo: la creación pudo haberse guardado.

## Logs de desarrollo

La consola indica la clase y el lugar de un fallo inesperado. Revisa también los errores de conexión al arrancar: URL de SQLite, permisos del archivo y directorio de ejecución. La base predeterminada es `aquax.db` en ese directorio.

Si necesitas inspeccionar el SQL durante el desarrollo, añade temporalmente `SPRING_JPA_SHOW_SQL=true` en **Run Configurations > Environment**, reinicia y repite una sola solicitud. Retira la variable al terminar. Evita activar el registro de parámetros enlazados, cuerpos de login o cabeceras Authorization.

Swagger, disponible en `http://localhost:8080/swagger-ui/index.html` durante el desarrollo, permite comparar la solicitud con el contrato esperado. El perfil `prod` deshabilita Swagger y OpenAPI.

## Repetir las pruebas del backend

Desde la carpeta que contiene `pom.xml`, con JDK 21 y Maven:

```powershell
mvn test
```

En Eclipse usa **Run As > Maven test**. Las pruebas de integración crean bases SQLite temporales en `target`, con cuentas y claves propias. Las pruebas unitarias comprueban reglas y exportación sin iniciar Spring. Ninguna necesita abrir la base habitual `aquax.db`.

Para generar el ejecutable después de las pruebas:

```powershell
mvn package
```

El resultado está en `target/aquax-backend-0.0.1-SNAPSHOT.jar`. Las pruebas de navegador Angular/API son una etapa posterior; las del backend no sustituyen ese recorrido completo.
