package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class AcercaDeWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void getAboutDevuelveNombreAplicacion() throws Exception {
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("ToDoList")));
    }

    @Test
    public void menuDeAboutMuestraLoginYRegistroSinSesion() throws Exception {
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(containsString("href=\"/registro\"")));
    }

    @Test
    public void menuDeAboutMuestraTareasYCuentaConSesion() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(7L);
        usuario.setNombre("Ana García");
        when(usuarioService.findById(7L)).thenReturn(usuario);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", 7L);

        this.mockMvc.perform(get("/about").session(session))
                .andExpect(content().string(containsString("href=\"/usuarios/7/tareas\"")))
                .andExpect(content().string(containsString("Ana García")))
                .andExpect(content().string(containsString("Cuenta")))
                .andExpect(content().string(containsString("href=\"/logout\"")));
    }
}