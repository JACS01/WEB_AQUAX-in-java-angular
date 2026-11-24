# Entrega del frontend y conexión con AquaX Backend

Este paquete contiene solamente el proyecto Angular: código, pruebas, configuración y guías. No contiene el backend Java, la base de datos, `node_modules`, cachés ni archivos de compilación. La persona que lo recibe debe disponer de la API de AquaX por separado.

## Abrir y ejecutar

1. Extrae el ZIP. Abre la carpeta `aquax-frontend` en Visual Studio Code; dentro deben estar `package.json` y `angular.json`.
2. Usa una versión de Node compatible con Angular 22. El proyecto se comprobó con Node 26.12.0 y npm 11.20.0.
3. Abre **Terminal > Nueva terminal** desde esa carpeta y ejecuta:

```powershell
npm.cmd ci
npm.cmd start
```

4. Abre `http://localhost:4200`. También están disponibles las tareas y la configuración F5 descritas en `GUIA_USO.md`.

No es necesario crear otro proyecto con `ng new` ni instalar Angular CLI globalmente. `npm ci` instala lo indicado por el lockfile.

## Backend en el mismo equipo

Inicia primero el backend AquaX con JDK 21 y su configuración. La configuración de desarrollo del frontend ya espera la API en `http://localhost:8080`:

```json
{
  "/api/**": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

Ese archivo es `proxy.conf.json`. En los entornos de Angular, `apiUrl` vale `/api`: el navegador llama a Angular y el servidor de desarrollo reenvía la solicitud a Spring. No se reemplaza `/api` por otra ruta porque el backend ya utiliza ese prefijo.

Comprueba primero `http://localhost:8080/api/productos`; debe devolver el catálogo en JSON. Después comprueba el catálogo desde Angular. Para iniciar sesión, el backend necesita `AQUAX_JWT_SECRET` correctamente configurado y una cuenta activa real. No se incluyen contraseñas de acceso ni claves de firma en el frontend.

## Backend en otro equipo o puerto

Cambia únicamente `target` en `proxy.conf.json` por la dirección accesible de la API, manteniendo `apiUrl: '/api'`. Por ejemplo, para una API de desarrollo en el puerto 9090 del mismo equipo:

```json
{
  "/api/**": {
    "target": "http://localhost:9090",
    "secure": false,
    "changeOrigin": true
  }
}
```

Si Spring está en otro equipo, utiliza su host/IP accesible en `target`. `localhost` siempre representa el equipo donde se ejecuta Angular CLI, no el equipo de quien envió el ZIP. Reinicia `npm.cmd start` después de cambiar el proxy.

La API debe exponer los mismos endpoints y DTO del backend AquaX; cambiar solo la dirección no adapta el frontend a una API diferente.

## Producción o conexión directa sin proxy de desarrollo

```powershell
npm.cmd run build
```

El resultado se genera en `dist/aquax-frontend/browser`. Configura el servidor que lo publique para servir `index.html` en las rutas Angular y reenviar `/api/**` a Spring. El proxy de `ng serve` no se incluye en los archivos de producción.

Si frontend y API se publican en orígenes diferentes, configura la URL absoluta de la API en `src/environments/environment.prod.ts` antes de compilar. En Spring configura `AQUAX_CORS_ALLOWED_ORIGINS` con el origen exacto del frontend (protocolo, host y puerto); la variable admite varios orígenes separados por comas. Utiliza HTTPS para el despliegue. Nunca coloques secretos JWT ni contraseñas en los archivos de entorno de Angular: se distribuyen al navegador.

## Verificación conjunta pendiente

Las últimas comprobaciones locales del frontend fueron: compilación de producción correcta y 294 pruebas aprobadas en 21 archivos. Las pruebas HTTP usan respuestas controladas; no certifican el backend.

Antes de cerrar la entrega, ambos responsables deben ejecutar `PRUEBAS_INTEGRACION.md`: login por rol, registro y persistencia de pedidos, cambios de entrega/pago, conflictos de duplicados, protección del último administrador, CSV y aislamiento entre cuentas.

Para conocer los módulos y límites implementados consulta `GUIA_USO.md` y `VALIDACIONES.md`. El backend fuente no fue modificado para preparar este paquete.
