package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.UsuarioNoAutorizadoException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class EquipoController {

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    private Long comprobarUsuarioLogeado() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null) {
            throw new UsuarioNoLogeadoException();
        }
        return idUsuarioLogeado;
    }

    private void comprobarAdministrador() {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario == null ? null : usuarioService.findById(idUsuario);
        if (usuario == null || !usuario.isAdministrador()) {
            throw new UsuarioNoAutorizadoException();
        }
    }

    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();
        model.addAttribute("equipos", equipos);
        return "listaEquipos";
    }

    @GetMapping("/equipos/nuevo")
    public String formNuevoEquipo(@ModelAttribute EquipoData equipoData, Model model) {
        comprobarUsuarioLogeado();
        return "formNuevoEquipo";
    }

    @PostMapping("/equipos/nuevo")
    public String nuevoEquipo(@ModelAttribute EquipoData equipoData, Model model, RedirectAttributes flash) {
        comprobarUsuarioLogeado();
        try {
            equipoService.crearEquipo(equipoData.getNombre());
            flash.addFlashAttribute("mensaje", "Equipo creado correctamente");
            return "redirect:/equipos";
        } catch (EquipoServiceException e) {
            model.addAttribute("error", e.getMessage());
            return "formNuevoEquipo";
        }
    }

    @GetMapping("/equipos/{id}")
    public String detalleEquipo(@PathVariable("id") Long id, Model model) {
        try {
            EquipoData equipo = equipoService.recuperarEquipo(id);
            List<UsuarioData> usuarios = equipoService.usuariosEquipo(id);
            Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
            boolean esMiembro = false;
            if (idUsuarioLogeado != null) {
                esMiembro = usuarios.stream().anyMatch(u -> u.getId().equals(idUsuarioLogeado));
            }
            model.addAttribute("equipo", equipo);
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("esMiembro", esMiembro);
            return "detalleEquipo";
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @PostMapping("/equipos/{id}/unirse")
    public String unirseEquipo(@PathVariable("id") Long id, RedirectAttributes flash) {
        Long idUsuarioLogeado = comprobarUsuarioLogeado();
        try {
            equipoService.añadirUsuarioAEquipo(id, idUsuarioLogeado);
            flash.addFlashAttribute("mensaje", "Te has unido al equipo correctamente");
            return "redirect:/equipos/" + id;
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @PostMapping("/equipos/{id}/usuarios")
    public String anadirUsuarioAEquipo(@PathVariable("id") Long id, RedirectAttributes flash) {
        return unirseEquipo(id, flash);
    }

    @PostMapping("/equipos/{id}/salir")
    public String salirEquipo(@PathVariable("id") Long id, RedirectAttributes flash) {
        Long idUsuarioLogeado = comprobarUsuarioLogeado();
        try {
            equipoService.eliminarUsuarioEquipo(id, idUsuarioLogeado);
            flash.addFlashAttribute("mensaje", "Has salido del equipo correctamente");
            return "redirect:/equipos/" + id;
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @DeleteMapping("/equipos/{id}/usuarios/{idUsuario}")
    public String eliminarUsuarioRest(@PathVariable("id") Long id, @PathVariable("idUsuario") Long idUsuario) {
        comprobarUsuarioLogeado();
        try {
            equipoService.eliminarUsuarioEquipo(id, idUsuario);
            return "redirect:/equipos/" + id;
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @GetMapping("/equipos/{id}/editar")
    public String formEditarEquipo(@PathVariable("id") Long id, Model model) {
        comprobarAdministrador();
        try {
            EquipoData equipoData = equipoService.recuperarEquipo(id);
            model.addAttribute("equipoData", equipoData);
            return "formEditarEquipo";
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @PostMapping("/equipos/{id}/editar")
    public String modificarEquipo(@PathVariable("id") Long id, @ModelAttribute EquipoData equipoData, Model model, RedirectAttributes flash) {
        comprobarAdministrador();
        try {
            equipoService.modificarEquipo(id, equipoData.getNombre());
            flash.addFlashAttribute("mensaje", "Equipo modificado correctamente");
            return "redirect:/equipos";
        } catch (EquipoServiceException e) {
            if (e.getMessage().contains("no existe")) {
                throw new EquipoNotFoundException();
            }
            equipoData.setId(id);
            model.addAttribute("error", e.getMessage());
            return "formEditarEquipo";
        }
    }

    @PostMapping("/equipos/{id}/eliminar")
    public String eliminarEquipo(@PathVariable("id") Long id, RedirectAttributes flash) {
        comprobarAdministrador();
        try {
            equipoService.eliminarEquipo(id);
            flash.addFlashAttribute("mensaje", "Equipo eliminado correctamente");
            return "redirect:/equipos";
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @DeleteMapping("/equipos/{id}")
    public String eliminarEquipoDelete(@PathVariable("id") Long id, RedirectAttributes flash) {
        return eliminarEquipo(id, flash);
    }
}
