# Guía de manejo de AquaX

## Estado actual

Las pantallas y los flujos principales del frontend están implementados. Para considerar terminada la entrega hace falta ejecutar el recorrido de integración con Spring Boot, una base de pruebas y cuentas de administrador/vendedor. Las pruebas de frontend con respuestas controladas no sustituyen esa comprobación.

La aplicación permite consultar el catálogo, crear pedidos, iniciar sesión, gestionar pedidos y estados, consultar reportes/CSV, administrar usuarios y empleados, actualizar la empresa, editar perfil/contraseña y guardar preferencias.

## Abrir el proyecto en Visual Studio Code

1. Abre Visual Studio Code.
2. Selecciona **Archivo > Abrir carpeta…**.
3. Elige `C:\Proyecto_Soluciones Web\aquax-frontend`, la carpeta donde están `package.json` y `angular.json`.
4. En el explorador izquierdo, despliega `src/app`.

También puedes abrirlo desde PowerShell:

```powershell
code 'C:\Proyecto_Soluciones Web\aquax-frontend'
```

Para leer esta guía en VS Code, abre `GUIA_USO.md` y pulsa **Ctrl+Shift+V** para la vista previa de Markdown.

## Iniciar el frontend desde VS Code

### Con F5

1. Si todavía no existe `node_modules`, usa **Terminal > Ejecutar tarea… > AquaX: instalar dependencias**. Espera a que termine.
2. Abre **Ejecutar y depurar** con **Ctrl+Shift+D**.
3. Selecciona **AquaX: iniciar y abrir frontend**.
4. Pulsa **F5**. VS Code inicia Angular y, cuando el servidor anuncia su URL, abre Chrome en `http://localhost:4200/`.
5. Modifica y guarda un archivo para ver la actualización de Angular.

Si el servidor Angular ya está ejecutándose en el puerto 4200, selecciona **AquaX: abrir frontend ya iniciado** para abrir el navegador sin iniciar un segundo servidor.

F5 también permite poner puntos de interrupción en archivos TypeScript. El frontend se visualiza en un navegador; VS Code se usa para editar, ejecutar y depurar.

### Con la terminal integrada

Abre **Terminal > Nueva terminal** y ejecuta:

```powershell
Set-Location 'C:\Proyecto_Soluciones Web\aquax-frontend'
# Solo en una instalación nueva o cuando se necesite reinstalar desde el lockfile:
npm.cmd ci
# Para el uso normal, basta con este comando:
npm.cmd start
```

Espera al mensaje `Local: http://localhost:4200/` y abre esa dirección. Usa `npm.cmd` para evitar bloqueos de `npm.ps1` en PowerShell. No necesitas instalar Angular CLI globalmente: los scripts usan la versión del proyecto.

También puedes usar **Terminal > Ejecutar tarea… > AquaX: iniciar frontend**. Para detenerlo, enfoca su terminal y pulsa **Ctrl+C**; al terminar una sesión de depuración, comprueba si la tarea del servidor continúa abierta y detenla si ya no la necesitas.

No abras `index.html` directamente como archivo; las rutas, TypeScript y plantillas se sirven mediante Angular CLI.

## Iniciar la API y conseguir acceso

El frontend y el backend son procesos distintos:

| Proceso     | Dirección esperada      | Propósito                                  |
| ----------- | ----------------------- | ------------------------------------------ |
| Angular     | `http://localhost:4200` | Pantallas                                  |
| Spring Boot | `http://localhost:8080` | Productos, cuentas, pedidos y persistencia |

Abre el backend en Eclipse y ejecuta `AquaxApplication.java`, o utiliza JDK21 y Maven desde `C:\Proyecto_Soluciones Web\aquax-backend\aquax-backend` con `mvn spring-boot:run`.

Comprueba `http://localhost:8080/api/productos`. Debe devolver JSON del catálogo. Para el inicio de sesión JWT, configura `AQUAX_JWT_SECRET` según el README del backend. Si la base es nueva, configura también el administrador inicial con `AQUAX_PERSONAL_USUARIO` y `AQUAX_PERSONAL_CONTRASENA`.

No existe un usuario/contraseña del frontend: debes usar una cuenta activa que exista en la base de AquaX. Las credenciales usadas por pruebas automatizadas no son cuentas de uso habitual.

`proxy.conf.json` envía `/api/**` al puerto8080. Si la API utiliza otro puerto, cambia ese destino y reinicia Angular.

## Recorrido de uso

### Cliente: consultar y pedir

1. Abre `/` y espera a que carguen los productos reales del servidor.
2. Agrega unidades con +/− o escribe una cantidad entera.
3. Completa cliente, teléfono y dirección; las observaciones son opcionales.
4. Revisa el total estimado y pulsa **Confirmar pedido** una sola vez.
5. Espera la confirmación con el número `AQX-…` y el total calculado por Spring.

Si se pierde la conexión al confirmar, el servidor podría haber guardado el pedido. Consulta el listado con el personal antes de repetirlo. El frontend no reintenta una creación automáticamente.

### Personal: acceso y pedidos

1. Entra por **Acceso del equipo** e inicia sesión.
2. En **Resumen**, consulta los pedidos operativos y pagos pendientes.
3. En **Pedidos**, busca por cliente, teléfono o número; cambia tamaño, orden y página según necesites.
4. Los filtros se conservan en la URL: puedes recargar o volver con Atrás/Adelante. La búsqueda automática espera300ms; Buscar/Enter la envía de inmediato.
5. Usa **Nuevo pedido** para registrar una venta con tu cuenta como responsable.
6. Cambia **Entrega** a Nuevo/En camino/Entregado y **Pago** a Pendiente/Pagado según lo que realmente ocurrió.
7. Abre **Detalle** para ver productos, dirección, creador y auditoría.

Un pedido entregado y pagado pasa de **En operación** a **Completados**. El historial conserva quién hizo los cambios.

### Reportes y notificaciones

En **Reportes**, consulta completados, aplica fechas de entrega incluidas y descarga CSV. El archivo usa los filtros aplicados; cambiar las fechas sin pulsar Aplicar no cambia el conjunto descargado. La otra pestaña muestra todos los pedidos pagados, incluso si falta entregarlos.

En **Notificaciones**, filtra sin leer, marca una o todas y abre el pedido relacionado. Los avisos corresponden a tu cuenta. Se actualizan al entrar o pulsar Actualizar.

### Administrador

| Pantalla  | Operaciones                                                                           |
| --------- | ------------------------------------------------------------------------------------- |
| Usuarios  | Crear cuenta y activar/desactivar acceso                                              |
| Empleados | Crear con cuenta, vincular existente, editar ficha/perfil/rol/acceso y eliminar ficha |
| Empresa   | Consultar y guardar datos empresariales                                               |

Antes de eliminar un empleado se pide confirmar. Eliminar la ficha laboral conserva su cuenta: utiliza activar/desactivar para suspender el acceso. El backend impide dejar al sistema sin un administrador activo y rechaza nombres, correos, DNI o vínculos duplicados.

Un vendedor no ve estas opciones y las rutas administrativas comprueban su rol. La autorización definitiva se aplica en Spring.

### Mi cuenta

En **Mi perfil**, guarda tus datos personales. Para cambiar contraseña debes escribir la actual, la nueva y la confirmación. Tras un cambio exitoso se cierra la sesión y debes ingresar con la nueva contraseña.

En **Preferencias**, guarda zoom y filtro visual. El idioma preferido se guarda, pero la interfaz actual está traducida únicamente al español. Al cerrar sesión se restablece la presentación para no trasladar tus preferencias a otra cuenta.

## Problemas frecuentes

| Síntoma                            | Comprobación                                                                                        |
| ---------------------------------- | --------------------------------------------------------------------------------------------------- |
| No abre localhost:4200             | Angular debe estar ejecutándose; revisa su terminal                                                 |
| Puerto4200 ocupado                 | Usa el servidor ya iniciado o detén la tarea anterior; evita aceptar otro puerto si F5 apunta a4200 |
| Catálogo muestra error             | Abre primero `/api/productos` en el puerto8080; revisa la consola de Spring y el destino del proxy  |
| Login503                           | Configura una clave JWT válida en el backend y reinícialo                                           |
| Login401                           | Comprueba cuenta activa, nombre y contraseña actuales                                               |
| Respuesta403                       | La cuenta no tiene el rol requerido; no equivale a contraseña incorrecta                            |
| Conflicto409                       | Corrige duplicados o la operación que dejaría sin administrador activo                              |
| Módulo Angular no encontrado       | Ejecuta la tarea de instalar dependencias o `npm.cmd ci`                                            |
| F5 abre pero los datos no aparecen | Iniciar Angular no inicia Spring; comprueba ambos procesos                                          |

Un mensaje genérico por sí solo no identifica la causa exacta. Revisa la solicitud en **DevTools > Network** y el registro del backend.

## Validar y compilar desde VS Code

- **Terminal > Ejecutar tarea… > AquaX: ejecutar pruebas** ejecuta Vitest una sola vez.
- **Ctrl+Shift+B** ejecuta la tarea predeterminada **AquaX: compilar produccion**.
- Los detalles de cobertura por regla y los límites de la comprobación están en `VALIDACIONES.md`.
- El recorrido real entre frontend y backend está en `PRUEBAS_INTEGRACION.md`.

Comandos equivalentes:

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
```

La compilación se genera en `dist/aquax-frontend/browser`. `ng serve` se utiliza para desarrollo; un despliegue necesita un servidor HTTP con fallback de rutas SPA y proxy de `/api`.

Referencias oficiales: [Angular en VS Code](https://code.visualstudio.com/docs/nodejs/angular-tutorial), [tareas de VS Code](https://code.visualstudio.com/docs/debugtest/tasks), [servidor de Angular](https://angular.dev/tools/cli/serve).
