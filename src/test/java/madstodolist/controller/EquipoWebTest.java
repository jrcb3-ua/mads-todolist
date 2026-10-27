package madstodolist.controller;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    public void menuMuestraOpcionEquiposConSesion() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        this.mockMvc.perform(get("/about").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/equipos\"")))
                .andExpect(content().string(containsString("Equipos")));
    }

    @Test
    public void listadoEquiposMuestraEquiposOrdenadosAlfabeticamente() throws Exception {
        // GIVEN
        equipoService.crearEquipo("Proyecto BBB");
        equipoService.crearEquipo("Proyecto AAA");

        // WHEN / THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Listado de equipos")))
                .andExpect(content().string(containsString("Proyecto AAA")))
                .andExpect(content().string(containsString("Proyecto BBB")));
    }

    @Test
    public void detalleEquipoMuestraInformacionYParticipantes() throws Exception {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario.setNombre("Juan Pérez");
        usuario = usuarioService.registrar(usuario);

        EquipoData equipo = equipoService.crearEquipo("Proyecto Alfa");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // WHEN / THEN
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Equipo: Proyecto Alfa")))
                .andExpect(content().string(containsString("user@ua")))
                .andExpect(content().string(containsString("Juan Pérez")));
    }

    @Test
    public void detalleEquipoNoExisteDevuelveNotFound() throws Exception {
        this.mockMvc.perform(get("/equipos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void nuevoEquipoSinLoginDevuelveUnauthorized() throws Exception {
        this.mockMvc.perform(get("/equipos/nuevo"))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(post("/equipos/nuevo").param("nombre", "Proyecto X"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void formNuevoEquipoConLogin() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        this.mockMvc.perform(get("/equipos/nuevo").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo equipo")))
                .andExpect(content().string(containsString("Nombre del equipo:")));
    }

    @Test
    public void crearNuevoEquipoCorrectamente() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        this.mockMvc.perform(post("/equipos/nuevo")
                        .session(session)
                        .param("nombre", "Nuevo Equipo Proyecto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));

        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();
        assertThat(equipos).extracting(EquipoData::getNombre).contains("Nuevo Equipo Proyecto");
    }

    @Test
    public void crearNuevoEquipoNombreVacioMuestraError() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        this.mockMvc.perform(post("/equipos/nuevo")
                        .session(session)
                        .param("nombre", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El nombre del equipo no puede ser vacío")));
    }

    @Test
    public void unirseYSalirDeEquipoCorrectamente() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        EquipoData equipo = equipoService.crearEquipo("Equipo Alpha");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        // Comprobamos que al inicio no es miembro y aparece el botón Unirse al equipo
        this.mockMvc.perform(get("/equipos/" + equipo.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Unirse al equipo")));

        // Nos unimos al equipo
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirse").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipo.getId()));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).hasSize(1);

        // Ahora comprobamos que aparece el botón Dejar el equipo
        this.mockMvc.perform(get("/equipos/" + equipo.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dejar el equipo")));

        // Dejamos el equipo
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipo.getId()));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
    }
}
