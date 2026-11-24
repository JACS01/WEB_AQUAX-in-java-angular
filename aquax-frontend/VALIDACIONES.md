# Informe de validaciones de AquaX Frontend

## Resultado comprobado

- Compilación de producción: **correcta**, mediante `npm.cmd run build`.
- Suite completa: **294 pruebas aprobadas en 21 archivos, 0 fallos**, mediante `npm.cmd test -- --watch=false`.
- Login revisado en navegador: al enviar vacío aparecen mensajes de campos obligatorios.
- Configuración VS Code: tareas de instalar, iniciar, probar y compilar; F5 configurado para iniciar y abrir Chrome; se eliminó la configuración obsoleta de Karma.
- Comprobación de conexión en esta revisión: no había una API escuchando en `http://localhost:8080/api/productos`. Por eso no se certifica todavía el recorrido integrado contra Spring.

El número de pruebas incluye casos parametrizados de límites. No es un porcentaje de cobertura y no demuestra ausencia de cualquier error posible. La matriz siguiente describe las reglas concretas verificadas y quién las decide.

## Validaciones de formularios

| Área                      | Regla comprobada en Angular                                                                                                                                                                                                                       | Comprobación que corresponde a Spring                                                                         |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Login                     | Usuario obligatorio/no blanco, máximo50; contraseña obligatoria/no blanca, máximo72 caracteres; envío único mientras espera                                                                                                                       | Cuenta existente/activa, contraseña real, configuración JWT y emisión de token                                |
| Pedido público y personal | Cliente obligatorio máximo150; teléfono obligatorio máximo15; dirección obligatoria máximo300; observaciones opcionales máximo500                                                                                                                 | Precios actuales, total, origen, creador y persistencia transaccional                                         |
| Productos y cantidades    | Carrito no vacío; IDs presentes en el catálogo cargado; cantidades enteras positivas; máximo9999 por producto en esta UI; no enviar durante carga/error; no reenviar tras éxito                                                                   | Producto existente al momento de guardar; cantidad positiva dentro del tipo Java; total calculado en servidor |
| Crear usuario             | Usuario de3–50 letras ASCII/números/punto/guion/guion bajo; nombre/apellido no blancos máximo100; correo válido máximo150; contraseña de8–72 caracteres y hasta72 bytes UTF-8; rol Administrador/Vendedor                                         | Unicidad de usuario/correo; codificación BCrypt; autorización del administrador                               |
| Vincular empleado         | Identificador de cuenta numérico, entero seguro y positivo; cuentas cargadas correctamente; DNI de8 dígitos; fecha opcional real                                                                                                                  | Cuenta existente, DNI y vínculo únicos; fecha de Lima por defecto si se omite                                 |
| Crear empleado con cuenta | Reglas de crear usuario más teléfono no blanco máximo15, DNI de8 dígitos y fecha opcional real                                                                                                                                                    | Creación atómica de cuenta/ficha y duplicados                                                                 |
| Editar ficha laboral      | DNI de8 dígitos y fecha obligatoria real en formato yyyy-MM-dd                                                                                                                                                                                    | Existencia de empleado y conflictos con otra ficha                                                            |
| Perfil de empleado        | Nombre/apellido no blancos máximo100; correo válido máximo150; teléfono no blanco máximo15                                                                                                                                                        | Unicidad de correo y cuenta vinculada                                                                         |
| Rol/acceso de empleado    | Roles permitidos; bloquear envíos repetidos; validar operación antes de enviar                                                                                                                                                                    | Mantener un administrador activo; aplicación de permisos actuales                                             |
| Empresa                   | Razón social no blanca máximo150, RUC de11 dígitos, dirección no blanca máximo300; teléfonos opcionales máximo15; correo opcional válido máximo200; registro sanitario máximo500; eslogan máximo200; bloquear guardado antes de cargar/tras fallo | Existencia del registro empresarial y persistencia                                                            |
| Mi perfil                 | Nombre/apellido no blancos máximo100, correo válido máximo150, teléfono no blanco máximo15; bloquear escrituras antes de carga exitosa o durante recarga                                                                                          | Cuenta autenticada y correo único                                                                             |
| Contraseña                | Actual no blanca hasta72 caracteres/bytes; nueva no blanca de8–72 caracteres/hasta72 bytes; confirmación coincidente; bloquear doble envío                                                                                                        | Verificar contraseña actual, actualizar BCrypt y revocar JWT anteriores                                       |
| Preferencias              | Idiomas es/en/pt; zoom entero80/90/100/110/120/130/150; filtro de lista permitida; estado estrictamente booleano; no guardar formulario inválido, sin datos o durante carga                                                                       | Guardar únicamente las preferencias del usuario autenticado                                                   |

Los teléfonos siguen el contrato Java: obligatorios u opcionales según la operación y máximo15 caracteres. No se impuso una regla ficticia de solo9 dígitos. El RUC y DNI se comprueban por longitud/formato; no hay consulta a RENIEC/SUNAT ni se afirma validación registral.

La fecha de alta al crear/vincular es opcional: si se vacía, se omite en el JSON y Spring aplica su valor predeterminado. La fecha al editar ficha es obligatoria. No se impuso una prohibición de fechas futuras porque el DTO no la establece.

Filtros admitidos: `none`, `deuteranopia`, `protanopia`, `tritanopia`, `grayscale`, `contrast`, `warm`, `cool`, `sepia`, `invert`.

## Navegación, tablas y archivos

| Área                  | Casos verificados                                                                                                                                       |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Página/tamaño         | Índice desde0; rechazo o normalización de negativos, fracciones, valores no numéricos/no seguros; tamaño1–50; corrección directa a última página válida |
| Orden                 | Campo de la lista permitida para cada tabla y dirección asc/desc; un campo interno/inválido no se propaga a la API                                      |
| Búsqueda              | Debounce300ms; Enter inmediato; reinicio de página; restauración por URL/F5/Atrás; búsqueda pendiente no sobrescribe una URL restaurada                 |
| Lecturas concurrentes | `switchMap` cancela consultas anteriores; respuesta vieja no reemplaza datos nuevos; error no destruye permanentemente el flujo y se permite reintentar |
| Estados de pedido     | Solo NUEVO/EN_CAMINO/ENTREGADO y PENDIENTE/PAGADO antes de PATCH; bloqueo por fila; ante fallo se conserva el estado anterior                           |
| Pedido desde aviso    | Busca operativos y completados; abre detalle/auditoría; corrige página antes de cambiar de pestaña                                                      |
| Reportes              | Fechas ISO reales, incluidos años bisiestos; fechas opcionales; rango inicial no posterior al final; no consultar rango inválido                        |
| CSV                   | Usa filtros aplicados; respuesta Blob; nombre seguro y fallback; errores ProblemDetail incluso en Blob; bloqueo de descargas duplicadas                 |

## Autenticación y errores

Se verificaron restauración de sesión, sesión malformada/vencida, cuenta inactiva, consulta actual de roles, guardias y rechazo de acceso administrativo. El token solo se adjunta al origen/ruta de API y se excluye de las operaciones públicas.

Un401 protegido de la sesión actual elimina esa sesión. Un401 tardío de una solicitud perteneciente a una sesión anterior ya no elimina una sesión nueva. Un403 conserva la sesión. Las solicitudes a otro origen no provocan cierre de la sesión de AquaX.

Se verificaron mensajes ProblemDetail, errores de campos, fallo de conexión, fin del estado de carga y conservación de formularios ante escrituras rechazadas. Las preferencias y la identidad se actualizan después de una respuesta exitosa, no antes.

La expiración local se comprueba al navegar o realizar la siguiente solicitud protegida. No existe un temporizador de cierre automático mientras la pantalla permanece inactiva.

## Fallos corregidos durante esta revisión

1. Login y campos obligatorios aceptaban algunos valores compuestos solo de espacios; los validadores ahora también rechazan nulos/no textuales cuando corresponde.
2. Roles y preferencias podían contener valores ajenos a sus listas si se modificaba un control programáticamente.
3. Fechas laborales no comprobaban el calendario real.
4. Empresa, perfil, preferencias o vinculación podían intentar guardar antes de una carga exitosa o durante una recarga.
5. El pedido podía enviarse con un ID ajeno al catálogo o mientras el catálogo no estaba disponible.
6. Estados de entrega/pago desconocidos podían llegar al PATCH.
7. Un401 antiguo podía cerrar una sesión recién reemplazada.
8. VS Code incluía un lanzamiento de pruebas en el puerto9876 de Karma, que no corresponde al ejecutor Vitest de este proyecto.

## Archivos de pruebas para consultar

- `src/app/features/cuenta/form-validation.spec.ts`: reglas de acceso, cuenta, preferencias y pedido.
- `src/app/features/admin/admin-validation.spec.ts`: límites de formularios administrativos y bloqueo de escrituras inválidas.
- `src/app/shared/validators.spec.ts`: calendario, no blanco, enteros, enumeraciones y bytes UTF-8.
- `src/app/core/auth/*.spec.ts` y `src/app/core/interceptors/*.spec.ts`: sesión y seguridad HTTP.
- `src/app/shared/table-query-state.spec.ts` y pruebas de páginas: URL, debounce y cancelación.
- `src/app/features/reportes/*.spec.ts`: fechas, exportación y contratos.

## Qué falta para cerrar la entrega

El frontend tiene implementados los módulos del backend actual. Aún falta la aceptación integrada con Spring: login de cuentas reales de prueba, pedidos que persistan, cambios de estado, conflictos de duplicados, protección del último administrador, descarga real, aislamiento entre cuentas y persistencia tras reiniciar.

Tampoco se ejecutó en esta revisión la nueva configuración F5 dentro de VS Code: se configuraron sus archivos y se comprobó que el comando de arranque de Angular funciona, pero la interacción F5 es una verificación manual pendiente. Sigue `GUIA_USO.md` para probarla.

Para producción todavía se debe preparar y comprobar el alojamiento, HTTPS, fallback de rutas y conexión con la API. El idioma de la interfaz actual es español; es/en/pt se guarda solo como preferencia. Las notificaciones requieren entrar/actualizar y el pago se registra como estado manual, conforme a la API disponible.

Ejecuta el recorrido de `PRUEBAS_INTEGRACION.md` con una base de pruebas antes de declarar la entrega completa. Las pruebas con HttpTestingController comprueban al consumidor HTTP; no ejecutan la lógica Java de unicidad, permisos o rollback.
