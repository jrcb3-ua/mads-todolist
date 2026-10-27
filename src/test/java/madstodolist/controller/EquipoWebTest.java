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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
}
