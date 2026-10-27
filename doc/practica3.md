# Documentación técnica de la práctica 3

## Introducción y modelo de datos

En esta práctica se completa la funcionalidad de equipos en la aplicación implementando las historias 009 (*Pertenencia al equipo*) y 010 (*Gestión de equipos por administradores*). Para modelar el dominio se ha definido una relación bidireccional muchos a muchos (`@ManyToMany`) entre `Usuario` y `Equipo`.

En el esquema relacional de PostgreSQL, el modelo comprende:
- La tabla `equipos`: contiene la clave primaria `id` y la columna `nombre`.
- La tabla intermedia `equipo_usuario`: gestiona la relación muchos a muchos mediante las claves foráneas `fk_equipo` (referencia a `equipos.id`) y `fk_usuario` (referencia a `usuarios.id`), configurando ambas columnas la clave primaria compuesta.

```
       +---------------+              +--------------------+              +----------------+
       |    equipos    | 1          * |   equipo_usuario   | *          1 |    usuarios    |
       +---------------+--------------+--------------------+--------------+----------------+
       | id (PK)       |              | fk_equipo (PK, FK) |              | id (PK)        |
       | nombre        |              | fk_usuario (PK, FK)|              | email, nombre  |
       +---------------+              +--------------------+              +----------------+
```

*(Estructura visible en pgAdmin sobre las tablas y restricciones del esquema público).*

## Endpoints definidos

La capa web expone los siguientes endpoints gestionados por `EquipoController`:

| Método | URL | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/equipos` | Registrado | Lista los equipos y acciones disponibles según rol. |
| `GET` | `/equipos/nuevo` | Registrado | Formulario para crear un equipo. |
| `POST` | `/equipos/nuevo` | Registrado | Procesa la creación validando el nombre. |
| `GET` | `/equipos/{id}` | Registrado | Detalle del equipo y lista de miembros. |
| `POST` | `/equipos/{id}/unirse` | Registrado | Añade al usuario en sesión como miembro. |
| `POST` | `/equipos/{id}/salir` | Registrado | Elimina al usuario en sesión del equipo. |
| `GET` | `/equipos/{id}/editar` | Administrador | Formulario de edición del nombre del equipo. |
| `POST` | `/equipos/{id}/editar` | Administrador | Actualiza el nombre validando que no esté vacío. |
| `POST` / `DELETE` | `/equipos/{id}/eliminar` o `/equipos/{id}` | Administrador | Elimina el equipo y desvincula a sus miembros. |

## Clases y métodos implementados

- **`Equipo`**: Entidad JPA con colección `Set<Usuario>`. Incluye los métodos helper `addUsuario` y `removeUsuario` para mantener la coherencia bidireccional en memoria.
- **`EquipoData`**: DTO desacoplado para transportar datos hacia las vistas con validación `@NotEmpty` en el nombre.
- **`EquipoService`**: Concentra la lógica de negocio bajo transacciones `@Transactional`:
  - `crearEquipo(String)`: Valida nombre no nulo ni vacío antes de persistir.
  - `eliminarUsuarioEquipo(Long, Long)`: Comprueba pertenencia previa y desvincula al usuario.
  - `modificarEquipo(Long, String)`: Verifica existencia del equipo y nombre no vacío antes de actualizar.
  - `eliminarEquipo(Long)`: Desvincula las referencias de los usuarios miembros para mantener la integridad referencial y borra la entidad.
- **`EquipoController`**: Atiende las peticiones, comprueba permisos con `comprobarAdministrador()` y `ManagerUserSession`, y delega en el servicio.

## Plantillas Thymeleaf

- **`listaEquipos.html`**: Muestra la tabla de equipos con botones para unirse o salir y, si el usuario es administrador (`usuarioMenu.administrador`), botones para editar y eliminar.
- **`detalleEquipo.html`**: Presenta los datos del equipo, lista alfabética de integrantes y botones de acción contextuales.
- **`formNuevoEquipo.html`**: Formulario de alta con validación visual mediante `th:errors`.
- **`formEditarEquipo.html`**: Formulario de edición con precarga del nombre actual y control de errores.

## Pruebas automatizadas

El desarrollo del servicio se abordó mediante ciclos TDD (Red-Green-Refactor):
- **`EquipoTest`**: Comprueba el mapeo Many-to-Many y los métodos helper de sincronización.
- **`EquipoServiceTest`**: Verifica creación, validaciones de nombre vacío, adición/eliminación de miembros, control de duplicados, modificación y borrado con desvinculación.
- **`EquipoWebTest`**: Evalúa con `MockMvc` la seguridad de acceso (respuestas 401 para no autorizados), renderizado de vistas, validaciones y redirecciones.

## Aspectos destacados del código

Destaca la eliminación segura de equipos en relaciones Many-to-Many en JPA, donde es preciso desvincular bidireccionalmente los usuarios asociados antes de borrar:

```java
@Transactional
public void eliminarEquipo(Long idEquipo) {
    Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
    if (equipo == null) {
        throw new EquipoServiceException("El equipo no existe");
    }
    for (Usuario usuario : equipo.getUsuarios()) {
        usuario.getEquipos().remove(equipo);
    }
    equipo.getUsuarios().clear();
    equipoRepository.delete(equipo);
}
```

Asimismo, el control de seguridad centralizado en el controlador restringe las acciones de administración:

```java
private void comprobarAdministrador() {
    Long idUsuario = managerUserSession.usuarioLogeado();
    UsuarioData usuario = idUsuario == null ? null : usuarioService.findById(idUsuario);
    if (usuario == null || !usuario.isAdministrador()) {
        throw new UsuarioNoAutorizadoException();
    }
}
```
