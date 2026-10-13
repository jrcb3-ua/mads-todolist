# Documentación técnica de la práctica 2

## Evolución y arquitectura

La aplicación conserva la arquitectura por capas del proyecto inicial: los controllers adaptan las peticiones HTTP, los servicios concentran las reglas de negocio, los repositories consultan y actualizan JPA, y las vistas Thymeleaf representan los datos recibidos. Las entidades no se exponen directamente a las plantillas; los servicios las convierten a DTO mediante `ModelMapper`. Las operaciones que acceden a persistencia se ejecutan dentro de transacciones del servicio.

La primera ampliación fue la página «Acerca de», atendida por `HomeController` en `GET /about`. La navegación compartida se centralizó en el fragmento `navbar` de `fragments.html`, incluido en las vistas de la aplicación salvo login y registro. `MenuModelAdvice` publica el usuario autenticado como atributo de modelo `usuarioMenu`, consultando el identificador guardado por `ManagerUserSession`. Así el fragmento presenta navegación distinta según haya sesión y puede construir el enlace a las tareas del usuario.

## Gestión de usuarios

`UsuarioController` implementa `GET /registrados`, que obtiene la colección mediante `UsuarioService.listarUsuarios()`, y `GET /registrados/{id}`, que consulta `UsuarioService.findById(Long)`. El servicio usa `UsuarioRepository.findAll()` o `findById()` y convierte los resultados a `UsuarioData`. La plantilla `listaUsuarios.html` presenta identificador y correo y enlaza cada registro con su descripción. `descripcionUsuario.html` representa identificador, nombre, correo, fecha de nacimiento y rol, omitiendo la contraseña. Si el identificador no existe se lanza `UsuarioNotFoundException`, asociada a HTTP 404.

La entidad `Usuario` incorpora los booleanos persistentes `administrador` y `bloqueado`; `UsuarioData` transporta ambos estados entre capas. `RegistroData` también incluye el rol enviado por el formulario. `LoginController` consulta si ya existe un administrador para decidir si muestra la casilla en `formRegistro.html`; el servicio vuelve a comprobarlo al registrar, y `UsuarioRepository.existsByAdministradorTrue()` encapsula la consulta derivada. El login envía a los administradores a `/registrados` y a los usuarios normales a su lista de tareas.

El acceso al listado, a las descripciones y a la acción de bloqueo se valida en `UsuarioController.comprobarAdministrador()`. La comprobación recupera el usuario asociado al ID de sesión y rechaza cuentas ausentes o sin rol de administrador con `UsuarioNoAutorizadoException` (HTTP 401). La protección se aplica antes de cargar los datos solicitados. Para la acción de bloqueo, `POST /registrados/{id}/bloqueo`, el controller exige el mismo rol y delega el cambio en `UsuarioService.cambiarBloqueoUsuario(Long)`, que invierte y persiste el estado. La plantilla ofrece «Bloquear» o «Habilitar» según el estado. No se permite bloquear la propia cuenta administradora para evitar perder la capacidad de administración.

## Autenticación y validación

`UsuarioService.login` distingue credenciales incorrectas, usuario inexistente, acceso correcto y cuenta bloqueada mediante `LoginStatus`. El estado bloqueado se comprueba después de validar la contraseña, y `LoginController` devuelve `formLogin` con un mensaje específico sin crear sesión. El valor por defecto de `bloqueado` es `false`, por lo que las cuentas existentes y nuevas permanecen habilitadas mientras no se cambie su estado.

Un fragmento representativo de la autorización es:

```java
private void comprobarAdministrador() {
    Long idUsuario = managerUserSession.usuarioLogeado();
    UsuarioData usuario = idUsuario == null ? null : usuarioService.findById(idUsuario);
    if (usuario == null || !usuario.isAdministrador()) {
        throw new UsuarioNoAutorizadoException();
    }
}
```

Esta comprobación concentra la política en el controller y evita duplicar comparaciones de rol en cada ruta protegida. Las excepciones anotadas con `@ResponseStatus` permiten traducir fallos de autorización y de recurso inexistente a respuestas HTTP sin introducir lógica de persistencia en las vistas.

Los tests de `UsuarioServiceTest` verifican registro del administrador único, alternancia de bloqueo y resultado del login con una cuenta bloqueada. `UsuarioWebTest` cubre la casilla condicional de registro, la redirección del administrador y el mensaje de bloqueo. `RegistradosWebTest` prueba el contenido del listado y las descripciones, la ausencia de contraseñas, las respuestas 404/401 y la protección de la acción; también verifica el caso de autoprotección. `AcercaDeWebTest` comprueba la navegación con y sin sesión. Los tests web usan `MockMvc` y mocks para aislar dependencias; los de servicio ejercitan las operaciones contra la base de datos H2 configurada para pruebas.
