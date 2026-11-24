# AquaX Frontend

Aplicación Angular 22 standalone para el backend AquaX. Creada desde PowerShell con Angular CLI 22.2.3; compilación estricta de TypeScript y plantillas, SCSS, rutas diferidas, formularios reactivos y HttpClient.

## Iniciar desde PowerShell

```powershell
Set-Location 'C:\Proyecto_Soluciones Web\aquax-frontend'
npm.cmd ci
npm.cmd start
```

Abre http://localhost:4200. Se usa `npm.cmd` para evitar depender de la política de ejecución de scripts de PowerShell. También puedes ejecutar `./iniciar.ps1` si tu política permite scripts locales. El proyecto ya está creado; no repitas `ng new` sobre esta carpeta.

El comando usado para crearlo fue:

```powershell
ng.cmd new aquax-frontend --routing --style=scss --standalone --ssr=false --skip-git --package-manager=npm --defaults --skip-install
```

Requisitos: Node y npm compatibles con Angular 22. Entorno de validación: Node 26.12.0, npm 11.20.0. Consulta la [tabla oficial de compatibilidad](https://angular.dev/reference/versions) para otras versiones. `package-lock.json` fija las dependencias; usa `npm.cmd ci` en otra instalación.

## Conectar con Spring Boot

1. Inicia AquaX desde Eclipse o Maven en `C:\Proyecto_Soluciones Web\aquax-backend\aquax-backend` (JDK 21).
2. Configura `AQUAX_JWT_SECRET` en el entorno del backend como indica su README. Sin esa clave, el login JWT responde 503.
3. Si es una base nueva, configura el administrador inicial con `AQUAX_PERSONAL_USUARIO` y `AQUAX_PERSONAL_CONTRASENA`. Se usan cuentas reales existentes; no hay contraseña predeterminada en el frontend.
4. Comprueba `http://localhost:8080/api/productos` y abre el frontend.

En desarrollo, `proxy.conf.json` redirige `/api/**` al puerto 8080. `environment.ts` usa `/api`. Cambia el destino del proxy si el backend usa otro puerto y reinicia `npm.cmd start`. El proxy evita solicitudes entre orígenes desde el navegador en desarrollo. Para conectar directamente con otro origen, modifica `src/environments/environment.ts` para desarrollo o `src/environments/environment.prod.ts` para producción y configura `AQUAX_CORS_ALLOWED_ORIGINS` en Spring para el origen exacto del frontend.

## Rutas y funciones

| Ruta                    | Función                                                             | Acceso        |
| ----------------------- | ------------------------------------------------------------------- | ------------- |
| `/`                     | Catálogo, cantidades y pedido público                               | Público       |
| `/login`                | Inicio de sesión JWT                                                | Público       |
| `/panel`                | Indicadores y últimos pedidos operativos                            | Personal      |
| `/panel/pedidos`        | Búsqueda, paginación, entrega, pago, detalle e historial            | Personal      |
| `/panel/pedidos/nuevo`  | Registrar pedido con la cuenta autenticada                          | Personal      |
| `/panel/reportes`       | Completados por fecha, resumen por producto, CSV e historial pagado | Personal      |
| `/panel/notificaciones` | Lectura individual/masiva y acceso al pedido                        | Personal      |
| `/panel/perfil`         | Datos personales y cambio de contraseña                             | Personal      |
| `/panel/preferencias`   | Idioma, zoom y filtros visuales guardados por cuenta                | Personal      |
| `/panel/usuarios`       | Crear cuentas y administrar acceso                                  | Administrador |
| `/panel/empleados`      | Crear/vincular/editar ficha, perfil, rol, acceso y eliminar ficha   | Administrador |
| `/panel/empresa`        | Datos empresariales                                                 | Administrador |

Los idiomas es/en/pt se guardan como preferencia de la cuenta. El menú, Preferencias, Resumen, Pedidos, Reportes, Notificaciones, Mi perfil, Usuarios, Empleados y Empresa muestran los textos de la interfaz en español, inglés o portugués. El catálogo, el formulario de pedido y el acceso también están traducidos y ofrecen un selector local de idioma. Los filtros visuales y el zoom se aplican al panel. Las notificaciones se actualizan al entrar o pulsar Actualizar; no hay WebSocket en el backend.

## Idiomas y comprobación

Las traducciones se encuentran en `src/app/core/i18n/es.json`, `en.json` y `pt.json`. Los tres archivos usan las mismas claves. `TranslationService` selecciona el diccionario mediante la preferencia recibida del backend; los archivos se incluyen en la compilación, sin un servicio externo de traducción. Para añadir un texto, crea su clave en los tres archivos y úsala mediante `i18n.t('clave')` en el componente o su plantilla. Los mensajes con datos usan parámetros, por ejemplo `i18n.t('orders.deliveryUpdated', { order: pedido.numeroPedido })`. Los valores del contrato API, como `EN_CAMINO`, `VENDEDOR` o los campos de ordenamiento, se conservan; solo sus etiquetas cambian.

Al entrar al panel, incluso tras recargar o volver a iniciar sesión, se consultan las preferencias de la cuenta. Guardar aplica el idioma después de la respuesta exitosa de Spring Boot. Si falla, se conserva el idioma anterior y la selección para reintentar. Restablecer formulario no guarda ni cambia el idioma aplicado. Cerrar sesión restablece el español, el zoom y los filtros para evitar trasladar preferencias a otra cuenta. Los idiomas desconocidos usan español.

Para comprobar las traducciones:

1. Inicia el backend con persistencia SQLite y el frontend. Entra con una cuenta y abre Preferencias.
2. Selecciona English y pulsa Guardar preferencias. Recorre Resumen, Pedidos, Reportes, Notificaciones y Mi perfil; con una cuenta administradora comprueba también Usuarios, Empleados y Empresa. Deben traducirse títulos, botones, tablas, estados, validaciones y etiquetas de accesibilidad. Los títulos de las pestañas y los formatos numéricos también siguen el idioma. La fecha de la cabecera mantiene la zona horaria de Lima y la moneda continúa siendo PEN.
3. Recarga y comprueba que continúa en inglés. Cierra sesión, vuelve a entrar con la misma cuenta y comprueba de nuevo.
4. Repite con Português y luego Español. Confirma que el zoom y el filtro siguen funcionando.
5. Detén el backend e intenta guardar otro idioma: el cambio no debe aplicarse ni mostrar confirmación de éxito. Al volver a iniciarlo, reintenta.
6. Entra con una segunda cuenta: debe recuperar su propia preferencia, o español si todavía no guardó una.
7. En el catálogo público o en el acceso, usa el selector local de idioma. Esa elección no guarda preferencias ni requiere una solicitud nueva al backend. Al entrar al panel se aplica la preferencia registrada de la cuenta.

Los nombres, correos, direcciones, nombres de productos, observaciones y demás datos del negocio conservan su contenido original. Spring Boot y sus respuestas no se modifican. Angular muestra equivalentes locales de los errores conocidos; para respuestas desconocidas en inglés o portugués usa un mensaje del estado HTTP e indica los campos reconocidos, sin reutilizar detalles españoles. Las notificaciones conocidas se presentan con una plantilla local que conserva el cliente y el número de pedido. Los mensajes de contenido libre conservan su texto original. Los archivos CSV se descargan tal como los genera el backend, sin traducir sus bytes ni sus encabezados.

## Organización

```text
src/app/
  core/
    models.ts                 Contratos basados en los DTO Java
    api.service.ts            Cliente HTTP común
    auth/                     Sesión, restauración y guardias de acceso/rol
    interceptors/             Bearer, problemas HTTP y errores de archivos
    preferences.service.ts    Aplicación de idioma y preferencias visuales
    i18n/                     Diccionarios es/en/pt y selección reactiva del idioma
  layout/                     Navegación adaptable por rol
  shared/                     Estado URL de tablas, iconos, errores y página 404
  features/
    auth/                     Login
    catalogo/                 Catálogo y formulario reutilizable de pedidos
    dashboard/                Resumen operativo
    pedidos/                  Gestión e historial
    reportes/                 Reportes y exportación
    admin/                    Usuarios, empleados y empresa
    cuenta/                   Perfil y preferencias
    notificaciones/           Avisos propios
```

## Sesión y errores

El JWT y su expiración se guardan en `sessionStorage` (por pestaña). No se almacenan contraseñas ni credenciales Basic. Las guardias restauran y revalidan la cuenta con `/api/auth/me`; el rol guardado en el navegador no determina permisos. Las guardias mejoran la navegación y el backend sigue autorizando cada operación.

El Bearer se envía únicamente al origen y ruta de la API configurada y se omite para catálogo público, pedido público y login. Un 401 protegido elimina la sesión; un 403 conserva la sesión. La expiración local se comprueba al navegar o realizar la siguiente solicitud protegida. Cambiar contraseña cierra la sesión porque Spring revoca los tokens anteriores.

Los errores `application/problem+json` presentan `detail` y los mensajes `errores`; la descarga CSV también interpreta errores Blob. Los formularios bloquean envíos repetidos y no reintentan escrituras automáticamente. Si se pierde la conexión después de registrar un pedido, consulta el listado antes de repetirlo: puede haberse guardado en el servidor.

## Verificar

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
```

Pruebas con Vitest y HttpTestingController: aislamiento y expiración del token, restauración, 401/403, guardias, navegación protegida, pedidos y prevención de duplicados, contratos de servicios, fechas y nombres de CSV, validación administrativa y acceso de la cuenta propia. Las pruebas HTTP usan respuestas controladas y no modifican `aquax.db`.

La revisión en navegador y las pruebas unitarias no certifican por sí solas la integración con Spring. Sigue `PRUEBAS_INTEGRACION.md` con el backend iniciado y una base de pruebas.

## Compilar y servir en producción

`npm.cmd run build` genera `dist/aquax-frontend/browser`. Sirve ese directorio con un servidor HTTP que devuelva `index.html` para rutas Angular y redirija `/api/**` al backend. El proxy de Angular se utiliza exclusivamente con `ng serve`; no se incluye en la compilación. Producción sustituye `environment.ts` por `environment.prod.ts` mediante `fileReplacements` en `angular.json`. Para otro origen de API, configura el archivo del entorno correspondiente antes de compilar y CORS en Spring. Usa HTTPS en el despliegue.

## Adaptación al material del curso

El análisis de los cinco PDF y su correspondencia con los componentes y servicios está en [docs/ANALISIS_MATERIAL_ANGULAR.md](docs/ANALISIS_MATERIAL_ANGULAR.md).

Las tablas de pedidos, usuarios y empleados conservan `page`, `size`, `texto` y `sort` en query params. La búsqueda espera 300 ms, reinicia la página y cancela la petición previa con RxJS. Buscar/Enter envía de inmediato. Atrás/Adelante y F5 restauran los criterios; `size` se limita a 50 y una página fuera de rango se corrige directamente. En pedidos también se conserva `completados`.

Las vistas extensas tienen plantillas externas. `ProductoCard` recibe producto/cantidad y emite `cantidadChange`; `PedidoForm` mantiene el estado del pedido y `CatalogoService` consulta y confirma con Spring. Los restantes módulos también usan servicios específicos para datos y terminan sus suscripciones al salir.

`environment.ts` y `environment.prod.ts` contienen configuración pública de la API. No deben incluir claves JWT de firma ni contraseñas.

## Manejo y estado de validación

- [Guía de uso y ejecución desde Visual Studio Code](GUIA_USO.md): abrir carpeta, F5, tareas, terminal y recorrido por módulos.
- [Informe actual de validaciones](VALIDACIONES.md): reglas por formulario, casos comprobados y verificaciones pendientes.

Resultado actual: compilación de producción correcta y **294 pruebas aprobadas en 21 archivos, sin fallos**. Se verificaron en navegador los mensajes de campos obligatorios del login. La aceptación con Spring Boot, datos persistidos y cuentas de prueba sigue pendiente; no debe confundirse la suite HTTP controlada con esa integración.

En VS Code están disponibles las tareas `AquaX: instalar dependencias`, `AquaX: iniciar frontend`, `AquaX: ejecutar pruebas` y `AquaX: compilar produccion`. Para F5 elige `AquaX: iniciar y abrir frontend`; si ya está ejecutándose usa `AquaX: abrir frontend ya iniciado`.

## Enviar solamente el frontend

Consulta [CONEXION_BACKEND.md](CONEXION_BACKEND.md) para extraer esta entrega, instalar dependencias y conectarla a una API AquaX local o remota. El ZIP de fuentes excluye backend, base de datos, `node_modules`, `dist` y cachés; se ejecuta con `npm.cmd ci` y `npm.cmd start`.
