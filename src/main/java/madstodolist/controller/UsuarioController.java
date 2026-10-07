package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.controller.exception.UsuarioNotFoundException;
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

    @GetMapping("/registrados")
    public String listaUsuarios(Model model) {
        List<UsuarioData> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable("id") Long idUsuario, Model model) {
        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNotFoundException();
        }
        model.addAttribute("usuario", usuario);
        return "descripcionUsuario";
    }
}