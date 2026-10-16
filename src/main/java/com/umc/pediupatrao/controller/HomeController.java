package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioResumo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;

    public HomeController(
            PedidoService pedidoService,
            ClienteService clienteService,
            ProdutoService produtoService,
            UsuarioService usuarioService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String listarUsuarios(Model model) {
        List<UsuarioResumo> usuarios = usuarioService.listarTodos().stream()
                .map(usuario -> new UsuarioResumo(usuario.getId(), usuario.getUsername(), usuario.getRole()))
                .toList();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    @PreAuthorize("hasRole('ADMIN')")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarUsuarioForm(@PathVariable String id, Model model, RedirectAttributes flash) {
        Optional<Usuario> encontrado = usuarioService.buscarPorId(id);
        if (encontrado.isEmpty()) {
            flash.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }
        Usuario form = new Usuario();
        form.setId(encontrado.get().getId());
        form.setUsername(encontrado.get().getUsername());
        form.setRole(encontrado.get().getRole());
        model.addAttribute("usuario", form);
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    @PreAuthorize("hasRole('ADMIN')")
    public String salvarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes flash) {
        try {
            usuarioService.criarUsuario(usuario);
            flash.addFlashAttribute("sucesso", "Usuário criado com sucesso.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String atualizarUsuario(
            @PathVariable String id,
            @ModelAttribute Usuario usuario,
            Authentication autenticacao,
            RedirectAttributes flash) {
        try {
            usuarioService.atualizarUsuario(id, usuario, autenticacao);
            flash.addFlashAttribute("sucesso", "Usuário atualizado com sucesso.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletarUsuario(@PathVariable String id, Authentication autenticacao, RedirectAttributes flash) {
        try {
            usuarioService.deletarUsuario(id, autenticacao);
            flash.addFlashAttribute("sucesso", "Usuário removido com sucesso.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public String pedidos(Model model) {
        prepararTelaPedido(model, "pedidos :: content");
        return "layout";
    }

    @GetMapping("/pedidos/novo")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public String novoPedido(Model model) {
        prepararTelaPedido(model, "pedidos :: content");
        model.addAttribute("novoPedidoAberto", true);
        return "layout";
    }

    private void prepararTelaPedido(Model model, String fragmento) {
        List<Pedido> pedidos = pedidoService.listarPedidos();
        List<Cliente> clientes = clienteService.listarClientes();
        List<Produto> produtos = produtoService.listarProdutosAtivos();
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("clientes", clientes);
        model.addAttribute("produtos", produtos);
        model.addAttribute("content", fragmento);
    }

    @GetMapping("/produtos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String produtos(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        model.addAttribute("content", "produtos/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("content", "clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String salvarCliente(@ModelAttribute Cliente cliente, Authentication autenticacao, RedirectAttributes flash) {
        try {
            clienteService.salvar(cliente, autenticacao);
            flash.addFlashAttribute("sucesso", "Cliente criado com sucesso.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String editarClienteForm(@PathVariable String id, Model model, RedirectAttributes flash) {
        Optional<Cliente> cliente = clienteService.buscarPorId(id);
        if (cliente.isEmpty()) {
            flash.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }
        model.addAttribute("cliente", cliente.get());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/editar/{id}")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String atualizarCliente(
            @PathVariable String id,
            @ModelAttribute Cliente cliente,
            Authentication autenticacao,
            RedirectAttributes flash) {
        try {
            cliente.setId(id);
            clienteService.salvar(cliente, autenticacao);
            flash.addFlashAttribute("sucesso", "Cliente atualizado com sucesso.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/clientes";
    }

    @PostMapping("/clientes/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String excluirCliente(@PathVariable String id, Authentication autenticacao, RedirectAttributes flash) {
        try {
            clienteService.excluir(id, autenticacao);
            flash.addFlashAttribute("sucesso", "Cliente removido com sucesso.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/clientes";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("content", "configuracoes :: content");
        return "layout";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin() {
        return "admin";
    }
}
