# iPhone: equipo Oak & Ember

Panel: https://oak-79f.pages.dev/operator

## Instalar y activar

1. Abrir el panel en Safari en un iPhone con iOS 16.4 o posterior.
2. Compartir → Agregar a pantalla de inicio. Abrir el nuevo ícono.
3. Iniciar sesión con una cuenta de Oak & Ember autorizada.
4. App y alertas → Activar alertas. Aceptar el permiso de iOS.
5. Probar alerta y comprobar que aparece el aviso en el dispositivo.

Es una PWA a pantalla completa, sin App Store ni cuota de Apple Developer. No es una IPA. Requiere conexión; no se guardan consultas ni fotos para uso offline. La APK Android 1.1 integra el mismo panel y Web Push mediante Chrome; ver docs/android-app.md. La versión Android 1.0 anterior no tiene esta integración.

## Privacidad y entrega

El push muestra únicamente “Nueva consulta”, sin nombre, contacto, dirección ni fotos. Abrirlo conduce al panel privado. Cada dispositivo debe activar sus propios avisos. Cerrar sesión intenta eliminar la asociación del servidor y cancela la suscripción local. Desactivar no cierra la sesión.

El ingreso real guarda la consulta primero y encola un evento de forma transaccional cuando está lista. La respuesta al cliente no depende del proveedor push. La función de ingreso envía los eventos en segundo plano, con reintentos a los 5 y 20 segundos. También se revisa la cola al abrir/actualizar el panel. Los eventos se reclaman con bloqueo, caducidad de 3 minutos y máximo 5 intentos; se recuerdan dispositivos ya entregados. Una caída prolongada puede dejar un aviso pendiente o agotar intentos; la consulta permanece disponible en la bandeja. Push no garantiza entrega ni sustituye revisar el panel. iOS puede silenciar avisos por ajustes o Concentración.

Suscripciones y cola tienen RLS sin acceso de anon/authenticated; se gestionan mediante la función autenticada oak-push y membresía explícita oak_operators. Claves VAPID privadas en oak_internal, sin permisos públicos; RPC invocador solo service_role. El cliente recibe exclusivamente la clave pública. Hosts de entrega HTTPS restringidos a Apple, Google y Mozilla; claves P-256 verificadas al registrar. Suscripciones vencidas se eliminan ante 404/410. Pruebas limitadas a una cada 30 segundos por dispositivo; máximo diez dispositivos por operador. Al eliminar la membresía se eliminan sus suscripciones por FK.

No se cachean datos privados, respuestas del backend ni fotografías. El service worker únicamente recibe notificaciones y abre la bandeja.

## Validación realizada

- Lint y build, pruebas de formulario/presupuestos y políticas de push.
- Chromium: login autorizado, bandeja, ajustes, logout, manifest y service worker; capturas y ausencia de desbordamiento a 320, 390 y 1440 px.
- Chromium: evento push sintético → notificación privada creada; contenido y enlace no confiables ignorados. No representa entrega física de Apple.
- Backend real: rechazo de suscripción inválida/dispositivo no registrado; RPC de claves privadas denegado a operador; evento temporal encolado y procesado sin suscripciones, eliminado después.
- Pendiente: instalación y recepción física de un aviso con iOS. Emular un viewport no verifica Apple Web Push ni su pantalla bloqueada. El botón Probar alerta permite esa comprobación final.

El esquema nuevo reproducible está en backend/push-schema.sql. La clave privada se genera y carga por un canal administrativo fuera de Git. Nunca copiarla al cliente ni publicarla en registros.
