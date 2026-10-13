package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.controller.exception.UsuarioNotFoundException;
import madstodolist.controller.exception.UsuarioNoAutorizadoException;
import madstodolist.authentication.ManagerUserSession;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    private void comprobarAdministrador() {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario == null ? null : usuarioService.findById(idUsuario);
        if (usuario == null || !usuario.isAdministrador()) {
            throw new UsuarioNoAutorizadoException();
        }
    }

    @GetMapping("/registrados")
    public String listaUsuarios(Model model) {
        comprobarAdministrador();
        List<UsuarioData> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable("id") Long idUsuario, Model model) {
        comprobarAdministrador();
        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNotFoundException();
        }
        model.addAttribute("usuario", usuario);
        return "descripcionUsuario";
    }
}