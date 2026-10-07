package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RegistradosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void listaUsuariosRegistradosMuestraIdentificadoresYCorreosSinContrasenas() throws Exception {
        UsuarioData ana = new UsuarioData();
        ana.setId(1L);
        ana.setEmail("ana@ua");
        ana.setPassword("secreto-ana");
        UsuarioData luis = new UsuarioData();
        luis.setId(2L);
        luis.setEmail("luis@ua");
        luis.setPassword("secreto-luis");
        when(usuarioService.listarUsuarios()).thenReturn(Arrays.asList(ana, luis));

        mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("1"),
                        containsString("ana@ua"),
                        containsString("2"),
                        containsString("luis@ua"),
                        containsString("href=\"/registrados/1\""),
                        not(containsString("secreto-ana")),
                        not(containsString("secreto-luis"))
                )));
    }

    @Test
    public void listaUsuariosRegistradosSinUsuariosMuestraTablaVacia() throws Exception {
        when(usuarioService.listarUsuarios()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuarios registrados")));
    }

    @Test
    public void descripcionUsuarioMuestraSusDatosSinContrasena() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(7L);
        usuario.setNombre("Ana García");
        usuario.setEmail("ana@ua");
        usuario.setFechaNacimiento(new Date(0));
        usuario.setPassword("no-mostrar");
        when(usuarioService.findById(7L)).thenReturn(usuario);

        mockMvc.perform(get("/registrados/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("7"),
                        containsString("Ana García"),
                        containsString("ana@ua"),
                        containsString("01/01/1970"),
                        not(containsString("no-mostrar"))
                )));
    }

    @Test
    public void descripcionUsuarioInexistenteDevuelve404() throws Exception {
        when(usuarioService.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/registrados/999"))
                .andExpect(status().isNotFound());
    }
}