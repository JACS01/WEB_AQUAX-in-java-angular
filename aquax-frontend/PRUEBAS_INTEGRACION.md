# Recorrido de integración AquaX

Realiza estas comprobaciones con Spring Boot en el puerto 8080, el frontend en el puerto 4200 y una base de pruebas. No uses pedidos reales para probar cambios de estado. Necesitas un administrador, un vendedor y un JWT configurado. No hay credenciales incrustadas en Angular.

## Acceso y navegación

- Abrir `/panel/usuarios` sin sesión: redirige a `/login` y conserva el destino interno.
- Credenciales incorrectas: termina el estado de carga y muestra 401, sin guardar contraseña.
- Administrador válido: abre el panel y permite usuarios/empleados/empresa.
- Vendedor válido: permite pedidos/reportes y oculta administración; navegar directamente a una ruta administrativa vuelve al panel.
- Cambiar el rol o estado desde otra sesión: la siguiente navegación y petición respetan los permisos vigentes.
- Recargar una ruta protegida: restaura la sesión consultando `/api/auth/me`.
- Revocar o vencer JWT: la siguiente navegación o solicitud protegida lleva al login.
- Respuesta 403: conserva la sesión. Salir elimina el JWT de la pestaña.

## Pedido completo

- Ver catálogo y precios obtenidos desde `/api/productos`.
- Elegir productos, cambiar cantidades enteras y completar cliente, teléfono y dirección.
- Confirmar una sola vez: recibe 201, muestra número/total reales y deshabilita un segundo envío.
- Verlo en pedidos operativos y dashboard; el pedido público genera avisos del backend.
- Crear otro desde el panel: verifica origen TRABAJADOR y usuario responsable.
- Cambiar entrega a EN_CAMINO/ENTREGADO y pago a PAGADO; consultar auditoría.
- Entregado+pagado desaparece de operativos y aparece en completados, reporte e historial pagado.
- Repetir carga de listados, buscar AQX/cadena Unicode y paginar. Probar búsqueda sin resultados.

## Reportes y notificaciones

- Filtrar completados con ambas fechas, una sola fecha y sin fechas; extremos incluidos.
- Rango invertido: mensaje local sin enviar consulta. Rango vacío: reporte vacío.
- Descargar CSV: nombre de Content-Disposition, UTF-8 con BOM y mismo conjunto que el reporte aplicado.
- Marcar un aviso como leído y luego todos; comprobar que otra cuenta mantiene sus avisos.
- Abrir un aviso de pedido operativo y otro ya completado; abrir detalle/historial correctos.

## Administración y cuenta

- Crear usuario, probar usuario/correo duplicados y activar/desactivar.
- Crear empleado con cuenta y vincular otra existente; DNI/vínculo duplicados devuelven conflicto.
- Editar datos laborales, perfil y rol; verificar cambios en la cuenta vinculada.
- Eliminar una ficha tras confirmar: conservar la cuenta, como define el backend.
- Intentar desactivar/degradar al último administrador: mostrar conflicto y conservar acceso.
- Actualizar empresa; recargar para comprobar persistencia.
- Guardar perfil; cambiar contraseña y volver a entrar con la nueva.
- Guardar zoom/filtro; recargar. La preferencia de idioma se guarda, la interfaz sigue en español.
- Reiniciar Spring para comprobar que pedidos/cuentas/preferencias permanecen en SQLite.

## Red y presentación

- Detener Spring: mostrar error y salir del estado de carga; no reintentar POST automáticamente.
- Simular validaciones 400, conflicto409 y fallo500; mostrar mensaje público sin datos internos.
- Para conexión directa sin proxy, verificar OPTIONS y CORS en éxito/error; consultar GUIA_DIAGNOSTICO.md.
- Probar teclado, foco visible y formularios vacíos; verificar anchos de escritorio y móvil.
- En producción, abrir una ruta profunda directamente para comprobar el fallback a index.html.

## Evidencia actual

La compilación de producción es correcta y la suite completa registra **294 pruebas aprobadas en 21 archivos, sin fallos**. Las pruebas HTTP usan HttpTestingController o respuestas controladas. No modifican la base habitual ni ejecutan las reglas de negocio de Java.

En esta revisión se observaron en navegador los mensajes de campos obligatorios del login. Evidencia: `docs/validacion-login.jpg`. Se configuraron tareas y depuración de VS Code; el comando de arranque del frontend fue ejecutado correctamente. La interacción F5 completa queda como verificación manual.

La comprobación actual del puerto8080 no encontró Spring Boot ejecutándose. El siguiente recorrido sigue pendiente de ejecución contra una base de pruebas. Consulta `VALIDACIONES.md` para las reglas y `GUIA_USO.md` para iniciar ambos procesos.

## Comprobaciones añadidas por el material de la semana 11

- Abrir `/panel/usuarios?page=2&size=25&texto=P%C3%A9rez&sort=nombre,asc`: restaurar controles y enviar esos criterios a Spring.
- Escribir varias letras con menos de 300 ms entre ellas: enviar solamente el último filtro tras la pausa. Enter debe enviar inmediatamente.
- Escribir un filtro y pulsar Atrás antes de terminar la espera: conservar la vista restaurada, sin que el texto pendiente sobrescriba la URL.
- Cambiar rápidamente de búsqueda con red lenta: la consulta anterior queda cancelada y no sobrescribe resultados nuevos.
- Probar los mismos casos en empleados y pedidos, incluyendo `completados=true`.
- Abrir `page=99` con pocas páginas: corregir a la última en una navegación, sin repetir 99 consultas.
- Revisar `size=0`, valores fraccionarios y orden no permitido en la URL: enviar criterios seguros a la API.
- Interrumpir una lectura, mostrar error y reintentar: el flujo continúa; no queda cargando permanentemente.
- Comprobar el pedido desde notificaciones cuando ya está completado: abrir su detalle y auditoría.
- Rechazar una escritura desde Spring: conservar formulario e identidad; aplicar cambios únicamente después del éxito.
