# Comprobación manual de AquaX desde Angular

Este recorrido comprueba la conexión entre el navegador, Angular, Spring Boot y SQLite. Mantén Spring en el puerto 8080 y Angular en el 4200. Usa registros ficticios identificados como pruebas y conserva tu cuenta de administrador habitual.

Anota el número completo de cada pedido creado; en los pasos siguientes se llama **pedido A**, **pedido B** o **pedido C**. Los números reales los asigna el servidor: no tienen que empezar en AQX-0001.

Marca cada bloque únicamente después de observar el resultado esperado. Las pruebas manuales de este documento todavía deben ser realizadas por quien las sigue.

## 1. Catálogo y primer pedido público — pedido A

1. Abre `http://localhost:4200` y pulsa **Hacer un pedido**.
2. Comprueba que aparezcan los productos y sus precios; no debe quedar «Cargando catálogo» indefinidamente.
3. Añade dos unidades de **Bidón 20L** y una de **Paquete 15 x 500 ml**.
4. Comprueba que el contador indique tres unidades. Con precios 10 y 12, el total debe ser S/ 32,00. Si el catálogo tiene otros precios, calcula con los que muestra.
5. Deja los datos vacíos y pulsa **Confirmar pedido**: deben aparecer mensajes de campos obligatorios y no debe crearse el pedido.
6. Introduce estos datos ficticios:

| Campo | Valor de prueba |
| --- | --- |
| Nombre del cliente | Cliente Prueba Web A |
| Teléfono | 999888777 |
| Dirección | Dirección ficticia de prueba 123 |
| Observaciones | Prueba de integración desde Angular |

7. Confirma una sola vez. Debe aparecer «PEDIDO REGISTRADO», el número del pedido, las tres unidades, el total y la dirección.
8. Anota el número completo como **pedido A**. La confirmación no debe quedar disponible para enviar otra vez el mismo pedido; **Registrar otro pedido** inicia uno nuevo.

**Resultado esperado:** pedido público creado y total confirmado por Spring. Posteriormente se verá como Cliente, con entrega Nuevo y pago Pendiente.

## 2. Inicio de sesión y restauración

1. Pulsa **Acceso del equipo**.
2. Envía primero el formulario vacío y comprueba los mensajes locales.
3. Prueba una contraseña incorrecta para una cuenta conocida: debe mostrarse el error y terminar «Ingresando…».
4. Entra con el administrador que ya utilizabas en Thunder Client y su contraseña actual guardada en SQLite.
5. Debe abrirse el panel y mostrarse **Usuarios**, **Empleados** y **Empresa**.
6. Pulsa F5 estando en el panel: la sesión debe restaurarse y seguir mostrando la cuenta correcta.

Si el acceso devuelve 503, revisa `AQUAX_JWT_SECRET` en la configuración de ejecución de Spring y reinicia el backend conservando la misma clave. El frontend no utiliza las credenciales Basic de Thunder Client.

## 3. Resumen y consulta del pedido A

1. Entra en **Resumen** y revisa los cuatro indicadores. Anota las cifras para comparar sus cambios; pueden existir pedidos anteriores.
2. Entra en **Pedidos** y busca el código completo del pedido A.
3. Debe aparecer en **En operación**, con tres unidades en el detalle, total correcto, entrega Nuevo y pago Pendiente.
4. Pulsa **Detalle**. Comprueba cliente, dirección, teléfono, productos, cantidades y precios unitarios.
5. Un pedido público recién registrado puede tener el historial vacío: la auditoría de sus cambios se generará en el bloque siguiente.

## 4. Entrega, pago e historial

Realiza estos cambios únicamente sobre el pedido A de prueba:

| Acción | Resultado esperado |
| --- | --- |
| Cambiar entrega a **En camino** | Sigue operativo y conserva el pago Pendiente. |
| Consultar **Detalle → Historial de modificaciones** | Aparece el cambio de entrega, con usuario, fecha y valores anterior/nuevo. |
| Cambiar pago a **Pagado** | Conserva entrega En camino y sigue operativo. Debe aparecer en Historial de pagados. |
| Revisar **Resumen** | Respecto del estado anterior, baja Pago pendiente y sube Pagados por entregar en una unidad. |
| Cambiar entrega a **Entregado** | Registra fecha de entrega y, al estar pagado, deja de aparecer en En operación. |
| Abrir **Completados** y buscar su código | Aparece el pedido A entregado y pagado, con los cambios en su historial. |
| Revisar otra vez **Resumen** | El pedido deja de contarse como operativo. |

Las cifras del dashboard corresponden al conjunto operativo, no a todos los pedidos históricos.

## 5. Pedido creado por personal — pedido B

1. Dentro del panel pulsa **Nuevo pedido** o **+ Nuevo pedido**.
2. Usa el cliente **Cliente Prueba Web B**, los datos ficticios anteriores y un producto cualquiera del catálogo.
3. Confirma y anota el código como **pedido B**.
4. Abre su detalle desde Pedidos.

**Resultado esperado:** identifica al usuario que registró el pedido, aparece como Personal y el historial incluye el evento de creación. Los estados iniciales son Nuevo/Pendiente. Este registro interno no genera el aviso de nuevo pedido de cliente.

## 6. Búsqueda, orden y paginación

Comprueba las siguientes acciones en **Pedidos**, y repítelas después en **Usuarios** y **Empleados**:

- Buscar parte de un nombre o teléfono existente.
- Buscar un código completo, por ejemplo el del pedido B.
- Buscar una cadena inexistente: debe mostrarse un estado vacío, no un error del servidor.
- Cambiar el orden y comprobar que las filas se reordenen.
- Cambiar el tamaño de página entre los valores disponibles.
- Utilizar Siguiente/Anterior si existen varias páginas.
- Recargar y utilizar Atrás/Adelante del navegador: deben recuperarse los criterios de la URL.

Si hay diez registros o menos con tamaño 10, es correcto que Siguiente esté deshabilitado. Para probar varias páginas sin crear once registros, puedes abrir una lista con `page=0&size=2`, por ejemplo `http://localhost:4200/panel/pedidos?page=0&size=2`; las tablas admiten ese tamaño por URL. Las páginas se numeran desde 0.

## 7. Reportes y archivo CSV

1. En **Reportes → Pedidos completados**, pulsa **Limpiar** para consultar todo el historial.
2. El pedido A debe aparecer y sus unidades deben contribuir al resumen por producto.
3. Aplica la fecha de entrega del pedido A tanto en **Desde** como en **Hasta**. Debe seguir apareciendo: ambos extremos están incluidos.
4. Prueba solo Desde, solo Hasta y un rango sin resultados.
5. Prueba un rango invertido: debe mostrar un mensaje y permitir corregirlo.
6. Con un filtro válido, pulsa **Descargar CSV**. Revisa la descarga del navegador, normalmente en Descargas o en la carpeta que tengas configurada.
7. Abre el archivo y comprueba número de pedido, cliente, productos, cantidades, origen, pago y fecha. Debe corresponder al mismo filtro aplicado al reporte.
8. Un rango vacío puede descargarse correctamente con solo los encabezados.
9. Abre **Historial de pagados**: el pedido A debe estar incluido y contribuir al monto total.

El historial pagado incluye los pedidos pagados aunque todavía estén pendientes de entrega; el reporte de completados requiere entrega y pago completos.

## 8. Usuarios y cuenta de vendedor de prueba

Desde el administrador, abre **Usuarios → + Nueva cuenta** y crea una cuenta ficticia:

| Campo | Valor |
| --- | --- |
| Usuario | vendedor_web_01 |
| Rol | Vendedor |
| Nombre | Ana |
| Apellido | Pérez Prueba |
| Correo | vendedor.web.01@example.com |
| Contraseña inicial | AquaXPrueba2026! |

Si el usuario o correo ya existen, utiliza otro sufijo y anótalo. Debe aparecer la cuenta activa en la lista.

Comprueba formulario vacío, correo inválido y contraseña corta. Intenta crear otra cuenta repitiendo el usuario o correo de la cuenta anterior: debe mostrar el conflicto y conservar una sola cuenta.

Abre otra ventana o una ventana de incógnito e inicia sesión con este vendedor. Mantén al administrador en la primera ventana. En adelante:

- **Ventana A:** administrador habitual.
- **Ventana B:** vendedor de prueba.

En B deben aparecer los módulos de personal y de cuenta. Usuarios/Empleados/Empresa no deben estar disponibles. Abrir `/panel/usuarios` en B debe volver al resumen; el guard puede evitar la petición administrativa, de modo que no tiene que mostrarse un 403 en pantalla.

## 9. Empleados: vincular, crear y editar

En A abre **Empleados**:

1. Pulsa **Vincular cuenta**, elige vendedor_web_01 y usa un DNI ficticio de ocho dígitos que aún no exista, por ejemplo 11112222.
2. Omite Fecha de ingreso y pulsa **Crear ficha laboral**. Debe asignarse la fecha actual.
3. Comprueba que no se pueda vincular por segunda vez la misma cuenta ni repetir el DNI.
4. Pulsa **+ Nuevo empleado** para comprobar también la creación conjunta. Usa otro usuario y correo, rol Vendedor, teléfono, contraseña de prueba y un DNI distinto. Debe crear tanto cuenta como ficha.
5. En la ficha del primer vendedor pulsa **Editar**. Cambia DNI/fecha y pulsa **Guardar datos laborales**.
6. Modifica un dato personal y pulsa **Guardar perfil**. En B, **Mi perfil** debe mostrar ese cambio al volver a cargarlo.

No se confunden los identificadores: cada ficha tiene su ID de empleado y referencia una cuenta mediante usuarioId.

## 10. Roles y activación de cuentas

Usa únicamente al vendedor de prueba; conserva tu administrador habitual:

1. En A, edita la ficha del vendedor, cambia su rol a Administrador y pulsa **Guardar rol**.
2. En B navega a otra opción o recarga. Deben aparecer los módulos administrativos.
3. En A restaura el rol Vendedor. En B vuelve a navegar o recargar: desaparece el acceso administrativo.
4. En A desactiva esa cuenta desde Usuarios o Empleados.
5. En B intenta actualizar Pedidos: debe terminar la sesión al recibir 401.
6. En A reactiva la cuenta. En B vuelve a entrar con su contraseña vigente.

La protección del último administrador está cubierta por las pruebas del backend. No uses tu propia cuenta habitual para experimentar con desactivaciones o cambios de rol.

## 11. Notificaciones por usuario — pedido C

Con A y B ya autenticadas y activas:

1. Abre el catálogo público en otra pestaña y registra **Cliente Prueba Web C**. Anota su número.
2. En A y B entra en **Notificaciones** y pulsa **Actualizar**.
3. Ambas cuentas deben recibir su propio aviso del nuevo pedido C. El vendedor no tiene por qué recibir avisos de pedidos creados antes de existir su cuenta.
4. En A marca solo el aviso C como leído y vuelve a cargar: debe desaparecer del filtro Solo sin leer.
5. En B actualiza: el aviso de esa cuenta debe seguir sin leer.
6. Desactiva el filtro Solo sin leer en A para volver a consultar sus avisos leídos.
7. Prueba **Marcar todas como leídas** en una sola cuenta y comprueba de nuevo el aislamiento.

**Pendiente conocido:** el enlace **Ver pedido** puede fallar cuando usa un ID numérico que coincide con otros teléfonos o nombres. Si sucede, anota el caso; no significa que se haya perdido el pedido. Compruébalo en Pedidos buscando su código completo. La adaptación recomendada del enlace aún debe aplicarse.

## 12. Perfil y preferencias de la cuenta de prueba

En B:

1. En **Mi perfil**, cambia nombre, apellido, correo o teléfono ficticios y pulsa **Guardar perfil**.
2. Recarga para verificar que se conservaron. Prueba un campo requerido vacío y un correo inválido.
3. En **Preferencias**, selecciona zoom 120, filtro Escala de grises y **Aplicar el filtro de color**. Pulsa **Guardar preferencias**.
4. Deben verse el cambio de tamaño y el filtro; al recargar o volver a entrar deben restaurarse.
5. **Restablecer formulario** cambia los controles, pero necesitas guardar para persistir esos valores. Puedes restaurar zoom 100 y filtro desactivado al terminar.
6. English y Português se guardan como preferencia, pero la interfaz actual sigue en español: el formulario lo indica.

## 13. Empresa

En A abre **Empresa**. Anota el eslogan original, añade temporalmente una marca de prueba y pulsa **Guardar información**. Recarga para verificar persistencia y después restaura el eslogan original.

Comprueba también que RUC o correo con formato inválido muestren validación antes de guardar. Conserva los datos empresariales reales; para esta comprobación basta una edición reversible del eslogan.

## 14. Contraseña — dejar para el final

En B abre **Mi perfil → Cambia tu contraseña**:

1. Comprueba que una nueva contraseña corta o una confirmación distinta sean rechazadas.
2. Con una contraseña nueva válida, prueba primero una contraseña actual incorrecta: debe mostrar el error y conservar la sesión.
3. Introduce la contraseña actual correcta y usa como nueva, por ejemplo, AquaXPrueba2026Nueva!, con la misma confirmación.
4. Pulsa **Actualizar contraseña**. Debe cerrar la sesión porque los JWT anteriores quedan revocados.
5. La contraseña anterior debe ser rechazada al entrar; la nueva debe funcionar.

Anota la nueva contraseña de esta cuenta ficticia. No cambies aquí la contraseña del administrador habitual.

## 15. Eliminar una ficha y conservar su cuenta

Desde A elimina únicamente una ficha creada para este recorrido y confirma la eliminación. Debe desaparecer de Empleados, pero su cuenta debe seguir en Usuarios y poder iniciar sesión si continúa activa.

Después de comprobarlo puedes desactivar las cuentas ficticias para dar por terminado el recorrido. Eliminar una ficha laboral y desactivar una cuenta son operaciones diferentes.

## 16. Persistencia, desconexión y presentación

1. Recarga el navegador: los datos guardados deben continuar.
2. Detén Spring desde Eclipse dejando Angular abierto. Pulsa **Actualizar** en Pedidos: debe verse un error y terminar el estado de carga.
3. Inicia Spring con la misma configuración y directorio de ejecución. Reintenta la consulta: deben continuar los pedidos, cuentas y preferencias guardados en SQLite.
4. Pulsa **Cerrar sesión** y abre una ruta del panel directamente: debe llevar al login.
5. Reduce el ancho de la ventana y comprueba menú, tablas y formularios. Usa Tab para recorrer campos y controles.

No repitas automáticamente un registro si se perdió la respuesta de red: primero busca el pedido por cliente o consulta el listado, porque pudo haberse guardado antes de la desconexión.

## Registro de resultados

| Bloque | Resultado observado / pendiente |
| --- | --- |
| Catálogo y pedido público A | |
| Login y sesión | |
| Resumen y detalle | |
| Entrega, pago e historial | |
| Pedido interno B | |
| Búsqueda, orden y paginación | |
| Reportes y CSV | |
| Usuarios | |
| Empleados | |
| Roles y activación | |
| Notificaciones y pedido C | |
| Perfil y preferencias | |
| Empresa | |
| Cambio de contraseña | |
| Eliminación de ficha | |
| Persistencia, red y presentación | |

Cuando falle un bloque, anota la pantalla, la acción, el resultado observado y el código del registro de prueba. Evita adjuntar contraseñas o tokens en las capturas.
