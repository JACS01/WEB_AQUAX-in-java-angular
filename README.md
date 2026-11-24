# AquaX Backend

Backend en Java 21 y Spring Boot 4.1.1. Incluye catálogo, pedidos, dashboard, notificaciones, ajustes empresariales, empleados, perfiles, preferencias y cuentas individuales con los roles Administrador y Vendedor.

## Ejecutar en Eclipse

1. Configura JDK 21 en Eclipse.
2. Ve a **File > Import > Maven > Existing Maven Projects** y selecciona la carpeta que contiene el `pom.xml`.
3. Abre `AquaxApplication.java` y ejecútalo con **Run As > Java Application**.
4. Consulta `http://localhost:8080/api/productos`.

El perfil predeterminado `sqlite` guarda la base de datos en `aquax.db`, dentro del directorio desde el que se ejecuta AquaX. En Eclipse o Maven, usa la carpeta del proyecto como directorio de ejecución para abrir siempre el mismo archivo.

El primer arranque crea una base nueva con el catálogo, los datos empresariales y el administrador inicial configurado. Los usuarios, contraseñas, pedidos, notificaciones y demás cambios se conservan entre reinicios.

## Configuración de SQLite

La conexión está en `src/main/resources/application-sqlite.properties`. La propiedad `spring.profiles.default=sqlite` de `application.properties` activa esta configuración por defecto.

- `spring.datasource.url` usa `jdbc:sqlite:aquax.db`. Puedes definir `AQUAX_DATABASE_URL` con una ruta absoluta si necesitas fijar otra ubicación, por ejemplo `jdbc:sqlite:C:/ruta/del/proyecto/aquax.db`.
- `spring.jpa.hibernate.ddl-auto=update` conserva las tablas y sus registros entre ejecuciones.
- `schema-sqlite.sql` crea las tablas iniciales con sus relaciones y restricciones.
- La carga inicial agrega el catálogo cuando está vacío, la empresa cuando falta y el administrador cuando su nombre de usuario aún no existe.

Después de actualizar las dependencias, usa **Maven > Update Project…** en Eclipse y vuelve a ejecutar AquaX. Conserva las variables del administrador en **Run Configurations > Environment**.

Para comprobar la persistencia, guarda preferencias o crea un pedido, detén la aplicación y vuelve a iniciarla. Una nueva consulta debe devolver los datos guardados.

El perfil opcional `local` sigue disponible para pruebas temporales con H2 en memoria, activándolo explícitamente con `--spring.profiles.active=local`.

### Perfil de producción

Activa `prod` con `SPRING_PROFILES_ACTIVE=prod` en **Run Configurations > Environment** o con `--spring.profiles.active=prod`. El archivo `application-prod.properties` importa la configuración de SQLite, conserva la persistencia y deshabilita Swagger UI y OpenAPI.

Este perfil no ejecuta la carga del catálogo ni de la empresa de demostración. Usa una base preparada con los datos reales del negocio. El administrador inicial todavía puede crearse mediante `AQUAX_PERSONAL_USUARIO` y `AQUAX_PERSONAL_CONTRASENA` cuando esa cuenta aún no existe. Configura también `AQUAX_JWT_SECRET` para habilitar el inicio de sesión con tokens.

## API disponible

- `GET /api/productos` devuelve el catálogo y los identificadores de los productos.
- `POST /api/pedidos` registra un pedido de cliente y devuelve `201 Created`.

Ejemplo de cuerpo JSON para registrar un pedido.

```json
{
  "nombreCliente": "Ana Pérez",
  "telefonoCliente": "999888777",
  "direccionEntrega": "Av. Los Álamos 123",
  "detalles": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 1 }
  ]
}
```

El servidor toma los precios del catálogo, calcula el total y asigna los estados `NUEVO` y `PENDIENTE`. El número de pedido se deriva del identificador con el formato `AQX-0001`.

## Iniciar sesión con JWT en Thunder Client

La API admite tokens JWT Bearer y mantiene la autenticación HTTP Basic de las pruebas anteriores. Ambos mecanismos usan las mismas cuentas y permisos.

1. Genera una clave aleatoria en PowerShell. Este comando produce 32 bytes criptográficos y los convierte a Base64.

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtRng.Dispose()
```

2. En Eclipse, abre **Run > Run Configurations… > aquax-backend > Environment > Add…**. Escribe `AQUAX_JWT_SECRET` en **Name** y pega la salida generada en **Value**. Conserva esa clave entre reinicios y fuera del repositorio. Debe ser Base64 válido y contener al menos 32 bytes al decodificarse. Aplica los cambios y reinicia AquaX.
3. En Thunder Client, crea una solicitud **POST** a `http://localhost:8080/api/auth/login`, selecciona **Auth > None** y envía **Body > JSON** con una cuenta existente y su contraseña actual.

```json
{
  "nombreUsuario": "personal",
  "contrasena": "TU_CONTRASENA_ACTUAL"
}
```

4. La respuesta `200 OK` contiene estos campos.

| Campo | Contenido |
| --- | --- |
| `token` | JWT firmado que debes copiar. |
| `tokenType` | `Bearer`. |
| `expiresIn` | Vigencia en segundos, `3600` por defecto. |
| `usuario` | Datos de la cuenta autenticada, con su rol y estado activo. |

5. Crea una solicitud **GET** a `http://localhost:8080/api/auth/me`. En **Auth > Bearer**, pega únicamente el valor de `token`. Thunder Client enviará `Authorization: Bearer <token>`. Debes recibir `200 OK` con tu cuenta.
6. Usa ese mismo token en las solicitudes protegidas que correspondan al rol de la cuenta. Los ejemplos posteriores con **Auth > Basic** también pueden ejecutarse con **Auth > Bearer**.

Una contraseña incorrecta o una cuenta inactiva devuelve `401 Unauthorized`. Un token vencido, inválido o revocado también devuelve `401`; inicia sesión otra vez para obtener uno nuevo. Una cuenta autenticada sin el rol requerido recibe `403 Forbidden`. En cada solicitud se consultan el rol y el estado activo actuales, de modo que sus cambios se aplican desde la siguiente solicitud aunque el token se haya emitido antes.

Cambiar la contraseña revoca todos los JWT anteriores de esa cuenta. Vuelve a iniciar sesión con la nueva contraseña. La duración puede ajustarse con `AQUAX_JWT_EXPIRATION_SECONDS`, expresada en segundos positivos.

Si falta `AQUAX_JWT_SECRET`, la aplicación puede iniciar y HTTP Basic sigue disponible, pero `POST /api/auth/login` devuelve `503 Service Unavailable` con una indicación para configurar la clave. Una clave configurada con formato o longitud inválidos impide el arranque hasta corregirla.

## Probar el acceso del personal en Thunder Client

1. En Eclipse, ve a **Run > Run Configurations…**, abre la configuración con la que ejecutas AquaX y agrega la variable `AQUAX_PERSONAL_CONTRASENA` en la pestaña **Environment**. Usa una contraseña de prueba propia. Se crea un administrador inicial con el usuario `personal` por defecto. Puedes cambiar el nombre inicial con `AQUAX_PERSONAL_USUARIO`.
2. Reinicia AquaX. Registra un pedido de prueba con `POST /api/pedidos` y copia el `id` numérico de la respuesta.
3. En Thunder Client, crea una solicitud **GET** a `http://localhost:8080/api/personal/pedidos`. Sin credenciales debe devolver `401 Unauthorized`. En **Auth > Basic**, ingresa el usuario y la contraseña configurados. La respuesta debe ser `200 OK` y mostrar el pedido.
4. Envía estas solicitudes con las mismas credenciales y, para las solicitudes **PATCH**, usa **Body > JSON**. Reemplaza `1` por el `id` del pedido.

| Método | Ruta | Cuerpo JSON | Resultado esperado |
| --- | --- | --- | --- |
| PATCH | `/api/personal/pedidos/1/entrega` | `{"estadoEntrega":"EN_CAMINO"}` | `200 OK` y el nuevo estado de entrega. |
| PATCH | `/api/personal/pedidos/1/entrega` | `{"estadoEntrega":"ENTREGADO"}` | `200 OK` con la fecha de entrega registrada. |
| PATCH | `/api/personal/pedidos/1/pago` | `{"estadoPago":"PAGADO"}` | `200 OK` y el nuevo estado de pago. |
| GET | `/api/personal/pedidos?completados=true` | | `200 OK` con los pedidos entregados y pagados. |

El listado principal muestra los pedidos que aún tienen pendiente la entrega, el pago o ambos. Cada cambio de estado guarda la cuenta responsable, su nombre de usuario, la fecha y los valores anterior y nuevo en el historial de modificaciones.

Las cuentas se guardan en SQLite, con contraseñas codificadas mediante BCrypt. El administrador inicial se crea cuando su nombre de usuario aún no existe y hay una contraseña configurada. Los cambios posteriores de contraseña quedan guardados en la base de datos.

## Crear cuentas individuales

Reinicia AquaX después de actualizar el proyecto. En Thunder Client, conserva las credenciales del administrador inicial en **Auth > Basic**.

1. Envía `GET http://localhost:8080/api/auth/me`. Debe devolver `200 OK` con los datos del usuario y el rol `ADMINISTRADOR`.
2. Envía `POST http://localhost:8080/api/admin/usuarios` con **Body > JSON**.

```json
{
  "nombreUsuario": "vendedor1",
  "nombre": "Ana",
  "apellido": "Pérez",
  "correo": "ana@example.com",
  "contrasena": "PruebaAquax2026!",
  "rol": "VENDEDOR"
}
```

La respuesta debe ser `201 Created`, con los datos de la cuenta y su `id`. La contraseña se recibe únicamente al crear la cuenta.

3. Usa las credenciales de `vendedor1` en **Auth > Basic** y consulta `GET /api/personal/pedidos`. Debe devolver `200 OK`.
4. Con esas mismas credenciales consulta `GET /api/admin/usuarios`. Debe devolver `403 Forbidden`, porque la gestión de usuarios corresponde al administrador.
5. Vuelve a las credenciales del administrador para consultar `GET /api/admin/usuarios` o cambiar el estado de una cuenta con `PATCH /api/admin/usuarios/{id}/activo`.

```json
{
  "activo": false
}
```

Una cuenta desactivada recibe `401 Unauthorized` al intentar autenticarse. Para reactivarla, envía `{"activo":true}` como administrador.

El rol `ADMINISTRADOR` permite gestionar usuarios y pedidos. El rol `VENDEDOR` permite registrar y consultar pedidos, y actualizar entrega y pago. La API devuelve `409 Conflict` al repetir un nombre de usuario o correo, o al intentar desactivar al último administrador activo.

## Registrar pedidos como personal

Los administradores y vendedores pueden registrar pedidos con `POST http://localhost:8080/api/personal/pedidos`.

En Thunder Client, usa las credenciales de la cuenta en **Auth > Basic** y selecciona **Body > JSON**.

```json
{
  "nombreCliente": "Carlos Díaz",
  "telefonoCliente": "999888777",
  "direccionEntrega": "Av. Los Álamos 123",
  "observaciones": "Pedido registrado por el personal",
  "detalles": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 3, "cantidad": 1 }
  ]
}
```

La respuesta esperada es `201 Created`, con `origen` igual a `TRABAJADOR`, `estadoEntrega` igual a `NUEVO` y `estadoPago` igual a `PENDIENTE`. Con el catálogo de prueba, el total será `37.00`. Los campos `usuarioRegistroId` y `usuarioRegistro` identifican la cuenta que creó el pedido.

La API obtiene el usuario de las credenciales verificadas. El pedido y la entrada de creación en el historial se guardan en una misma transacción.

Puedes consultar el pedido con `GET /api/personal/pedidos` usando las mismas credenciales. Los siguientes cambios de entrega y pago conservan la cuenta creadora y registran quién hizo cada cambio.

El registro público sigue disponible en `POST /api/pedidos`, con el origen `CLIENTE` y los campos de usuario de registro en `null`.

## Consultar el historial de un pedido

1. Reinicia AquaX y registra un pedido como personal con `POST /api/personal/pedidos`.
2. Conserva las credenciales en **Auth > Basic** y envía `GET http://localhost:8080/api/personal/pedidos/1/historial`, sustituyendo `1` por el `id` devuelto al crear el pedido.
3. La respuesta debe ser `200 OK` con una lista de eventos. En el pedido interno recién creado habrá un evento con `campo` igual a `creacion`, `valorAnterior` igual a `N/A` y `valorNuevo` igual a `TRABAJADOR`.
4. Cambia la entrega a `EN_CAMINO` con el endpoint de actualización y repite la consulta del historial. Debe aparecer un evento adicional con el cambio de `NUEVO` a `EN_CAMINO`.

Cada evento incluye su identificador, el identificador del pedido, el identificador y el nombre del usuario responsable, el campo modificado, los valores anterior y nuevo, y la fecha con la hora de Lima y el desplazamiento `-05:00`. Los eventos se devuelven del más antiguo al más reciente.

Administrador y Vendedor pueden consultar este historial. Un pedido existente sin eventos devuelve `200 OK` con `[]`. Un identificador de pedido inexistente devuelve `404 Not Found`. Repetir una actualización con el mismo estado conserva el historial sin agregar otro evento.

## Probar las notificaciones

Al registrar un pedido público con `POST /api/pedidos`, cada administrador y vendedor activo recibe una notificación independiente. El pedido y sus notificaciones se guardan en una misma transacción.

1. Reinicia AquaX y registra un pedido mediante el endpoint público `POST http://localhost:8080/api/pedidos`.
2. Usa las credenciales de `personal` en **Auth > Basic** y consulta `GET http://localhost:8080/api/personal/notificaciones`.
3. Debes recibir `200 OK` con una notificación de tipo `pedido_nuevo`, con el número de pedido, el cliente, la hora de Lima y `leida` igual a `false`.
4. Copia el `id` de la notificación y envía `PATCH http://localhost:8080/api/personal/notificaciones/1/leida`, sustituyendo `1` por ese identificador. Deja el cuerpo vacío. La respuesta debe tener `leida` igual a `true`.

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/personal/notificaciones` | Notificaciones sin leer del usuario autenticado, de la más reciente a la más antigua. |
| GET | `/api/personal/notificaciones?soloNoLeidas=false` | Todas las notificaciones del usuario, incluidas las leídas. |
| GET | `/api/personal/notificaciones/no-leidas` | Objeto con `cantidad` de notificaciones sin leer. |
| PATCH | `/api/personal/notificaciones/{id}/leida` | Marca la notificación propia como leída y devuelve sus datos. |
| PATCH | `/api/personal/notificaciones/leidas` | Marca todas las propias como leídas y devuelve `actualizadas`, el número de registros modificados. |

Cada cuenta consulta y marca sus propias notificaciones. Intentar marcar un identificador inexistente o perteneciente a otro usuario devuelve `404 Not Found`. Repetir el marcado de una notificación propia ya leída devuelve `200 OK`.

Los pedidos registrados desde `POST /api/personal/pedidos` quedan registrados en el historial de modificaciones. Los avisos de nuevos pedidos de clientes se generan desde el registro público.

## Consultar y actualizar los datos empresariales

Reinicia AquaX y usa las credenciales del administrador en **Auth > Basic**. El primer arranque incluye los datos empresariales configurados en el proyecto.

1. Envía `GET http://localhost:8080/api/admin/empresa`. Debe responder `200 OK` con los datos de AquaX.
2. Envía `PUT http://localhost:8080/api/admin/empresa` con **Body > JSON** y el conjunto completo de datos que quieres guardar.

```json
{
  "razonSocial": "AQUAX",
  "ruc": "10715736689",
  "direccion": "Porcon Carretera San Pablo",
  "telefono1": "916154315",
  "telefono2": "955265327",
  "correo": null,
  "registroSanitario": "P0663225N FACBSR",
  "slogan": "La Diferencia es el Origen"
}
```

La respuesta debe ser `200 OK`, con los datos guardados y `ultimaActualizacion` con la fecha actual de Lima. Repite la consulta **GET** para verificar los cambios.

La razón social, el RUC y la dirección son obligatorios. El RUC debe contener exactamente 11 dígitos. Los teléfonos, el correo, el registro sanitario y el eslogan son opcionales. En esta actualización completa, los campos opcionales omitidos o enviados como `null` se vacían.

Administrador puede consultar y modificar estos datos. Vendedor recibe `403 Forbidden` en ambas operaciones, y una solicitud sin credenciales recibe `401 Unauthorized`. Un cuerpo inválido devuelve `400 Bad Request`.

En SQLite, los datos empresariales modificados se conservan al reiniciar la aplicación.

## Consultar y editar el perfil

Administrador y Vendedor pueden gestionar su propio perfil. La cuenta se identifica con las credenciales enviadas en **Auth > Basic**.

1. Consulta `GET http://localhost:8080/api/personal/perfil`. Debe responder `200 OK` con los datos de la cuenta. Las cuentas creadas antes de completar su perfil pueden tener `telefono` en `null`.
2. Envía `PUT http://localhost:8080/api/personal/perfil` con **Body > JSON**.

```json
{
  "nombre": "Jesús",
  "apellido": "Cabrera",
  "correo": "jesus@example.com",
  "telefono": "999888777"
}
```

Los cuatro campos son obligatorios. El nombre y apellido admiten hasta 100 caracteres cada uno, el correo hasta 150 y el teléfono hasta 15. Un correo ya registrado por otra cuenta devuelve `409 Conflict`.

La respuesta debe ser `200 OK` con el perfil actualizado. Repite la consulta **GET** para verificarlo.

## Cambiar la contraseña propia

En **Auth > Basic**, conserva la contraseña actual mientras envías `PATCH http://localhost:8080/api/personal/perfil/contrasena` con **Body > JSON**. Reemplaza `TU_CONTRASENA_ACTUAL` por la que usas para esa cuenta.

```json
{
  "contrasenaActual": "TU_CONTRASENA_ACTUAL",
  "nuevaContrasena": "AquaX2026Nueva!",
  "confirmarContrasena": "AquaX2026Nueva!"
}
```

La nueva contraseña debe tener al menos 8 caracteres y ocupar como máximo 72 bytes en UTF-8. La respuesta esperada es `200 OK` con el mensaje de confirmación. Después actualiza **Password** en **Auth > Basic** con la nueva contraseña para continuar consultando la API.

Si usas **Auth > Bearer**, puedes enviar el cambio con el token vigente. Al completarse, ese token y los demás JWT anteriores de la cuenta quedan revocados. Envía nuevamente `POST /api/auth/login` con la contraseña nueva y reemplaza el token en Thunder Client.

Una contraseña actual incorrecta o una confirmación diferente devuelve `400 Bad Request`. Usar las credenciales anteriores después de un cambio exitoso devuelve `401 Unauthorized`.

En SQLite, la contraseña modificada sigue siendo válida después de reiniciar. `AQUAX_PERSONAL_CONTRASENA` se utiliza para crear la cuenta inicial cuando aún no existe.

## Consultar y guardar preferencias

Administrador y Vendedor pueden consultar y guardar las preferencias de su propia cuenta usando **Auth > Basic**.

1. Consulta `GET http://localhost:8080/api/personal/preferencias`. Una cuenta que aún no ha guardado preferencias devuelve `200 OK` con el idioma `es`, zoom `100`, filtro `none` y `filtroColorActivo` igual a `false`.
2. Envía `PUT http://localhost:8080/api/personal/preferencias` con **Body > JSON**.

```json
{
  "idioma": "en",
  "nivelZoom": 120,
  "filtroColor": "grayscale",
  "filtroColorActivo": true
}
```

La respuesta debe ser `200 OK` con los valores guardados y el identificador del usuario autenticado. Repite la consulta **GET** para comprobarlos.

Los cuatro campos son obligatorios. Los valores admitidos coinciden con los controladores del proyecto original.

| Campo | Valores admitidos |
| --- | --- |
| `idioma` | `es`, `en`, `pt`. |
| `nivelZoom` | `80`, `90`, `100`, `110`, `120`, `130`, `150`. |
| `filtroColor` | `none`, `deuteranopia`, `protanopia`, `tritanopia`, `grayscale`, `contrast`, `warm`, `cool`, `sepia`, `invert`. |
| `filtroColorActivo` | `true` o `false`. |

Un idioma, filtro o zoom fuera de esos valores devuelve `400 Bad Request`. Angular recupera y aplica las preferencias de la cuenta. El idioma ya cambia los textos del menú principal y de Preferencias entre español, inglés y portugués; el resto de las pantallas continúa en español.

Las preferencias guardadas en SQLite se conservan al reiniciar la aplicación.

## Gestionar empleados

La ficha de empleado guarda el DNI y la fecha de ingreso, y se vincula a una cuenta de usuario. El nombre, apellido, contacto, rol y estado activo se obtienen de esa cuenta. Las modificaciones del perfil se reflejan en la ficha laboral.

Usa las credenciales del administrador en **Auth > Basic**. Estas operaciones corresponden al rol Administrador.

### Registrar empleado y cuenta juntos

Envía `POST http://localhost:8080/api/admin/empleados/con-usuario` con **Body > JSON**.

```json
{
  "usuario": {
    "nombreUsuario": "vendedor2",
    "nombre": "Ana",
    "apellido": "Pérez",
    "correo": "ana.empleado@example.com",
    "contrasena": "PruebaAquax2026!",
    "rol": "VENDEDOR"
  },
  "telefono": "999888777",
  "dni": "12345678"
}
```

La respuesta esperada es `201 Created`, con el identificador del empleado y el de su cuenta. La fecha de ingreso omitida toma la fecha actual de Lima. El registro de la cuenta y de la ficha laboral se realiza en una misma transacción.

### Vincular una cuenta existente

Consulta `GET /api/admin/usuarios` y copia el identificador de la cuenta que quieres vincular. Envía `POST http://localhost:8080/api/admin/empleados` con este cuerpo, reemplazando `2` por el identificador de la cuenta elegida.

```json
{
  "usuarioId": 2,
  "dni": "87654321"
}
```

La respuesta esperada es `201 Created`. Cada cuenta admite una ficha laboral y el DNI debe contener exactamente 8 dígitos. El DNI y el vínculo con la cuenta son únicos.

### Consultar y modificar la ficha laboral

| Método | Ruta | Operación |
| --- | --- | --- |
| GET | `/api/admin/empleados` | Lista los empleados con sus datos personales y laborales. |
| GET | `/api/admin/empleados/{id}` | Consulta la ficha del empleado indicado. |
| DELETE | `/api/admin/empleados/{id}` | Elimina la ficha laboral y conserva la cuenta de usuario. |
| PUT | `/api/admin/empleados/{id}` | Actualiza el DNI y la fecha de ingreso. |
| PUT | `/api/admin/empleados/{id}/perfil` | Actualiza nombre, apellido, correo y teléfono de la cuenta vinculada. |
| PATCH | `/api/admin/empleados/{id}/rol` | Cambia el rol de la cuenta vinculada. |
| PATCH | `/api/admin/empleados/{id}/activo` | Activa o desactiva la cuenta vinculada al empleado. |

Para actualizar los datos laborales usa un cuerpo como el siguiente.

```json
{
  "dni": "12345678",
  "fechaIngreso": "2026-11-11"
}
```

Para cambiar el estado usa `{"activo":false}` o `{"activo":true}`. El estado se comparte con la gestión de usuarios. Desactivar al empleado impide que su cuenta se autentique, y el sistema conserva al menos un administrador activo.

Un DNI o una cuenta ya vinculados devuelven `409 Conflict`. Un empleado o usuario inexistente devuelve `404 Not Found`. Una solicitud inválida devuelve `400 Bad Request`. Un vendedor recibe `403 Forbidden` en estas rutas.

### Editar los datos personales y el rol

Como administrador, consulta `GET /api/admin/empleados` y copia el `id` del empleado que deseas editar. Envía `PUT http://localhost:8080/api/admin/empleados/1/perfil`, reemplazando `1` por ese identificador, con este cuerpo.

```json
{
  "nombre": "Ana María",
  "apellido": "Pérez",
  "correo": "ana.actualizada@example.com",
  "telefono": "987654321"
}
```

La respuesta debe ser `200 OK` con la ficha actualizada. Los mismos datos se verán en el perfil de esa cuenta y en la consulta de usuarios. El correo debe tener un formato válido y ser único entre las cuentas.

Para cambiar el rol, envía `PATCH http://localhost:8080/api/admin/empleados/1/rol` con las credenciales del administrador y **Body > JSON**.

```json
{
  "rol": "ADMINISTRADOR"
}
```

La respuesta debe ser `200 OK`. La cuenta tendrá los permisos del nuevo rol desde su siguiente solicitud autenticada. Para asignarle el rol Vendedor, envía `{"rol":"VENDEDOR"}`. La API devuelve `409 Conflict` si el cambio dejaría al sistema sin administradores activos.

Los cambios se guardan en SQLite y permanecen después de reiniciar AquaX.

### Eliminar una ficha laboral

Como administrador, consulta `GET /api/admin/empleados` para identificar la ficha que deseas eliminar. Envía `DELETE http://localhost:8080/api/admin/empleados/1`, sustituyendo `1` por su identificador y dejando el cuerpo vacío.

La respuesta esperada es `204 No Content`, con el cuerpo vacío. La ficha deja de aparecer en el listado y su consulta individual devuelve `404 Not Found`. La eliminación queda guardada en SQLite.

La cuenta de usuario vinculada conserva su acceso, perfil, preferencias y referencias en pedidos e historial, tal como ocurría con la eliminación de empleados del proyecto original. Para suspender el acceso de esa cuenta utiliza `PATCH /api/admin/usuarios/{usuarioId}/activo` con `{"activo":false}`.

Un vendedor recibe `403 Forbidden` al intentar eliminar fichas. Un identificador inexistente o una ficha ya eliminada devuelve `404 Not Found`.

## Listados paginados y búsqueda

Las siguientes rutas devuelven una página de registros con **Auth > Bearer** o **Auth > Basic**.

| Método | Ruta | Acceso |
| --- | --- | --- |
| GET | `/api/admin/usuarios/pagina` | Administrador. |
| GET | `/api/admin/empleados/pagina` | Administrador. |
| GET | `/api/personal/pedidos/pagina` | Administrador y Vendedor. |

| Parámetro | Comportamiento |
| --- | --- |
| `page` | Índice de página desde `0`; por defecto `0`. |
| `size` | Cantidad por página; por defecto `10`, máximo `50`. Un valor mayor se limita a `50`. |
| `texto` | Búsqueda opcional. Los espacios de los extremos se omiten. |
| `sort` | Campo y dirección, por ejemplo `nombre,asc`. Admite varias apariciones para combinar criterios. |
| `completados` | Solo pedidos. Por defecto `false` lista los operativos; `true` lista los entregados y pagados. |

La búsqueda por `texto` no distingue mayúsculas de minúsculas, incluidos los caracteres Unicode. Por ejemplo, `pérez` encuentra `Pérez`. Los caracteres `%`, `_` y `!` se buscan literalmente. En usuarios busca coincidencias parciales en usuario, nombre, apellido y correo; en empleados agrega el DNI. En pedidos busca por parte del nombre del cliente o teléfono y permite consultar un número de pedido exacto, como `AQX-0001`, o su identificador numérico, como `1`.

Los campos permitidos para ordenar son los siguientes.

| Listado | Campos de `sort` | Orden predeterminado |
| --- | --- | --- |
| Usuarios | `id`, `nombreUsuario`, `nombre`, `apellido`, `correo`, `rol`, `activo` | `nombre,asc` y `id,asc`. |
| Empleados | `id`, `dni`, `fechaIngreso`, `nombreUsuario`, `nombre`, `apellido`, `correo`, `rol`, `activo` | `nombre,asc` y `id,asc`. |
| Pedidos | `id`, `numeroPedido`, `nombreCliente`, `telefonoCliente`, `fechaRegistro`, `fechaEntrega`, `estadoEntrega`, `estadoPago`, `origen` | `fechaRegistro,desc` y `id,asc`. |

Se agrega `id,asc` como desempate cuando no se solicita un orden por identificador. Un campo de orden no permitido, `page` negativo, `size` menor o igual a cero o un número fraccionario devuelve `400 Bad Request`.

Ejemplos para Thunder Client con el cuerpo vacío.

```text
GET http://localhost:8080/api/admin/usuarios/pagina?page=0&size=10&texto=ana&sort=nombre,asc&sort=id,asc
GET http://localhost:8080/api/admin/empleados/pagina?page=0&size=10&texto=12345678&sort=fechaIngreso,desc
GET http://localhost:8080/api/personal/pedidos/pagina?page=0&size=10&completados=false&texto=AQX-0001&sort=fechaRegistro,desc
```

La respuesta `200 OK` contiene `content`, con los DTO del listado, y `page`, con metadatos estables. Una búsqueda sin resultados devuelve lo siguiente.

```json
{
  "content": [],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

`page.number` identifica la página actual. `page.totalElements` cuenta todos los registros que cumplen el filtro y `page.totalPages` indica la cantidad de páginas. Las rutas anteriores sin `/pagina` conservan sus respuestas como arrays. Los reportes y su exportación CSV siguen procesando todo el conjunto que cumple sus filtros de fecha, independientemente de la paginación de los listados.

## Consultar el resumen del dashboard

Usa las credenciales de Administrador o Vendedor en **Auth > Basic** y consulta `GET http://localhost:8080/api/personal/dashboard/resumen`.

La respuesta será `200 OK` con los indicadores de los pedidos operativos. Un pedido es operativo mientras tenga pendiente la entrega, el pago o ambos.

| Campo | Significado |
| --- | --- |
| `totalPedidos` | Cantidad de pedidos operativos. |
| `pendientes` | Pedidos operativos con pago pendiente. |
| `pagados` | Pedidos pagados cuya entrega sigue pendiente. |
| `deClientes` | Pedidos operativos registrados desde el formulario público. |

Si todavía no hay pedidos operativos, los cuatro valores serán `0`. Cada pedido se cuenta una vez, aunque tenga varios productos. Los indicadores corresponden al listado de `GET /api/personal/pedidos`.

Para comprobar los cambios, registra un pedido público y vuelve a consultar el resumen. Deben aumentar `totalPedidos`, `pendientes` y `deClientes`. Al marcarlo como pagado, pasa de `pendientes` a `pagados` mientras siga pendiente la entrega. Cuando esté entregado y pagado, pasa al historial de completados y deja de participar en los indicadores operativos.

## Consultar el historial de pedidos pagados

Usa las credenciales de Administrador o Vendedor en **Auth > Basic** y envía `GET http://localhost:8080/api/personal/pedidos/pagados`, con el cuerpo vacío.

La respuesta será `200 OK` con `totalPagados`, `montoTotal` y `pedidos`. Incluye los pedidos cuyo estado de pago actual es `PAGADO`, tanto los pendientes de entrega como los ya entregados. Se ordenan por fecha de registro descendente y por identificador descendente cuando comparten fecha.

El monto se calcula con los precios unitarios guardados al registrar cada pedido. La suma usa `BigDecimal` en Java para conservar los importes exactos en SQLite.

Si no existen pedidos pagados, la respuesta será la siguiente.

```json
{
  "totalPagados": 0,
  "montoTotal": 0.00,
  "pedidos": []
}
```

Para comprobar el flujo, registra un pedido y márcalo como `PAGADO` con `PATCH /api/personal/pedidos/{id}/pago`. Vuelve a consultar el historial y verifica que aumenten el contador y el monto total. Entrega el pedido y consulta otra vez para comprobar que sigue apareciendo. Si cambias su pago a `PENDIENTE`, la consulta se recalcula con los pedidos que siguen pagados.

## Reporte de pedidos completados y CSV

Administrador y Vendedor pueden consultar el reporte con **Auth > Basic** mediante `GET http://localhost:8080/api/personal/reportes/completados`.

El reporte incluye los pedidos que están entregados y pagados. Devuelve `desde`, `hasta`, `totalPedidos`, `productos` y `pedidos`. La lista `productos` contiene el identificador, nombre y cantidad de unidades de cada producto del catálogo. Los productos sin unidades en el rango aparecen con cantidad `0`.

Para filtrar por fecha de entrega, agrega `desde` y `hasta` en formato `yyyy-MM-dd`. Ambos extremos están incluidos.

```text
http://localhost:8080/api/personal/reportes/completados?desde=2026-11-01&hasta=2026-11-30
```

Puedes enviar únicamente `desde` o únicamente `hasta`. Al omitir ambos parámetros, se devuelve todo el historial de completados. El rango invertido y una fecha mal escrita devuelven `400 Bad Request`.

Para exportar el mismo conjunto de pedidos, usa **GET** con las mismas credenciales.

```text
http://localhost:8080/api/personal/reportes/completados/csv?desde=2026-11-01&hasta=2026-11-30
```

La respuesta será `200 OK` con un archivo `aquax-historial-AAAA-MM-DD.csv`, codificado en UTF-8 con BOM. Contiene las columnas originales de número de pedido, cliente, teléfono, productos, cantidad, origen, pago y fecha de entrega. Cada pedido ocupa una fila lógica. Los campos con comillas, comas o saltos de línea quedan escapados, y los textos que pueden interpretarse como fórmulas reciben un apóstrofo inicial.

Un rango sin pedidos devuelve un reporte vacío con los contadores en cero. Su exportación CSV contiene únicamente el encabezado.

Para comprobar los datos, registra un pedido, márcalo como entregado y pagado, y consulta ambas rutas. Debe aparecer en la lista y en el CSV, y sus cantidades deben reflejarse en el resumen por producto.

## Documentación Swagger y OpenAPI

Con AquaX en ejecución y el perfil predeterminado `sqlite`, abre `http://localhost:8080/swagger-ui/index.html` para consultar los módulos, cuerpos JSON, validaciones, respuestas, filtros y exportaciones. La especificación JSON está disponible en `http://localhost:8080/v3/api-docs`. Ambas rutas son públicas en desarrollo.

Para probar una ruta protegida desde Swagger UI, pulsa **Authorize** y configura `bearerAuth` con el token obtenido en `POST /api/auth/login`, o `basicAuth` con las credenciales de la cuenta. Son dos alternativas de acceso. El catálogo público, el registro público de pedidos y el inicio de sesión pueden probarse sin autorización.

El perfil `prod` deshabilita tanto Swagger UI como `/v3/api-docs` mediante `application-prod.properties`.

## Ejecutar las pruebas automatizadas

Desde la carpeta del backend que contiene `pom.xml`, ejecuta lo siguiente con JDK 21 y Maven.

```powershell
mvn test
```

En Eclipse también puedes usar **Run As > Maven test**. Las pruebas configuran sus propias cuentas iniciales, clave JWT y bases SQLite temporales dentro de `target`. Esa configuración está aislada de `aquax.db` y de las credenciales del entorno habitual.

La suite permanente incluye las regresiones de pedidos, empleados, perfiles, preferencias, notificaciones, reportes y persistencia, además de JWT, CORS y contratos de error. También hay pruebas unitarias de reglas del pedido y exportación CSV que se ejecutan sin iniciar Spring.

## Validación de solicitudes

La API valida los tipos y valores recibidos. Los identificadores, cantidades y niveles de zoom deben representar enteros. Los estados y roles usan sus nombres, como `PAGADO` y `VENDEDOR`, y los campos booleanos usan `true` o `false`.

Una solicitud con tipos incorrectos devuelve `400 Bad Request` y un mensaje en español. Cuando falla una validación de campo, la respuesta incluye una lista `errores` con su nombre y la causa. Por ejemplo, una contraseña nueva demasiado corta produce un elemento como el siguiente.

```json
{
  "campo": "nuevaContrasena",
  "mensaje": "La nueva contraseña debe tener entre 8 y 72 caracteres."
}
```

## Errores y diagnóstico de integración

Los errores de autenticación, permisos, validación y negocio comparten el formato `application/problem+json`, con `status`, `title`, `detail` e `instance`. Las validaciones por campo agregan `errores`. Los parámetros inválidos tienen mensajes en español y los fallos inesperados responden con un detalle público genérico.

El `401` identifica un problema de autenticación y el `403`, permisos insuficientes. Las respuestas conservan las cabeceras CORS del origen admitido. Swagger documenta el contrato común.

Consulta `GUIA_DIAGNOSTICO.md` para reproducir errores en Thunder Client, comprobar un preflight OPTIONS, interpretar Network y activar logs de desarrollo. El documento incluye los pasos de ejecución de las pruebas y de compilación.

## Conexión del frontend Angular

La configuración CORS permite por defecto el origen `http://localhost:4200` para las rutas `/api/**`. Las solicitudes pueden enviar `Authorization`, `Content-Type` y `Accept`, y el navegador puede leer `Content-Disposition` para obtener el nombre del archivo CSV.

Si Angular usa otro origen, configura `AQUAX_CORS_ALLOWED_ORIGINS` en el entorno de ejecución de AquaX. Puedes indicar varios orígenes separados por comas, por ejemplo `http://localhost:4200,http://127.0.0.1:4200`.

Angular podrá iniciar sesión con `POST /api/auth/login` y enviar `Authorization: Bearer <token>` en las siguientes solicitudes protegidas. Al vencer o revocarse el token deberá iniciar sesión nuevamente. HTTP Basic sigue disponible para las mismas cuentas. Las operaciones administrativas y del personal conservan sus permisos por rol y los listados `/pagina` ofrecen el contrato necesario para los controles de búsqueda y paginación.
