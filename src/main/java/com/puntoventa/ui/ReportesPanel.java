package com.puntoventa.ui;

import com.puntoventa.model.Agrupacion;
import com.puntoventa.model.TablaDatos;
import com.puntoventa.model.Usuario;
import com.puntoventa.service.CsvExporter;
import com.puntoventa.service.ReporteService;
import com.puntoventa.service.UsuarioService;
import com.puntoventa.util.Formato;
import com.puntoventa.util.NegocioException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** CU12 y CU13: reportes con periodo rápido, selectores de fecha y exportación a CSV. */
public class ReportesPanel extends JPanel implements Refrescable {

    static {
        Tema.instalar();
    }

    private static final String[] TIPOS = {
            "Ventas por día", "Ventas por semana", "Ventas por mes", "Productos más vendidos",
            "Ingresos por periodo", "Existencias actuales", "Historial de operaciones por usuario"};

    private final Usuario usuario;
    private final ReporteService reportes = new ReporteService();
    private final UsuarioService usuarios = new UsuarioService();
    private final JComboBox<String> cmbTipo = new JComboBox<>(TIPOS);
    private final JComboBox<String> cmbUsuario = new JComboBox<>();
    private final JSpinner spDesde = Ui.selectorFecha("spDesde", LocalDate.now().withDayOfMonth(1));
    private final JSpinner spHasta = Ui.selectorFecha("spHasta", LocalDate.now());
    private final JButton btnGenerar = Ui.boton("Generar reporte", Ui.Tipo.PRIMARIO);
    private final JButton btnExportar = Ui.boton("Exportar CSV", Ui.Tipo.SECUNDARIO);
    private final JPanel panelPeriodo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final DefaultTableModel modelo = Ui.modeloSoloLectura();
    private final javax.swing.JTable tabla = Ui.tabla(modelo);
    private final JLabel lblMensaje = new JLabel(" ");
    private final JLabel lblResumen = Ui.suave(" ");
    private final List<Usuario> listaUsuarios = new ArrayList<>();
    private TablaDatos ultimo;

    public ReportesPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        Ui.Tarjeta filtros = new Ui.Tarjeta();
        filtros.setLayout(new BorderLayout(0, 4));

        cmbTipo.setName("cmbTipo");
        cmbUsuario.setName("cmbUsuario");
        btnGenerar.setName("btnGenerar");
        btnExportar.setName("btnExportar");
        lblMensaje.setName("lblMensajeReportes");
        lblResumen.setName("lblResumenReportes");
        cmbTipo.setPreferredSize(new java.awt.Dimension(300, 34));
        cmbUsuario.setPreferredSize(new java.awt.Dimension(200, 34));

        JPanel campos = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        campos.setOpaque(false);
        campos.add(Ui.filaCampo("Reporte", cmbTipo));
        campos.add(Ui.filaCampo("Desde", spDesde));
        campos.add(Ui.filaCampo("Hasta", spHasta));
        campos.add(Ui.filaCampo("Usuario (para el historial)", cmbUsuario));
        filtros.add(campos, BorderLayout.NORTH);

        panelPeriodo.setOpaque(false);
        panelPeriodo.add(Ui.suave("Periodo rápido:"));
        for (String[] atajo : new String[][]{{"Hoy", "hoy"}, {"Esta semana", "semana"}, {"Este mes", "mes"}, {"Últimos 30 días", "30"}}) {
            JButton b2 = Ui.boton(atajo[0], Ui.Tipo.SECUNDARIO);
            b2.putClientProperty("FlatLaf.style", "arc:999; margin:3,14,3,14");
            b2.setName("btnPeriodo_" + atajo[1]);
            b2.addActionListener(e -> periodo(atajo[1]));
            panelPeriodo.add(b2);
        }
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(panelPeriodo, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnExportar);
        botones.add(btnGenerar);
        pie.add(botones, BorderLayout.EAST);
        filtros.add(pie, BorderLayout.SOUTH);

        JPanel norte = Ui.columna();
        norte.add(filtros);
        lblMensaje.setBorder(new EmptyBorder(6, 2, 0, 0));
        JPanel filaMensaje = new JPanel(new BorderLayout());
        filaMensaje.setOpaque(false);
        filaMensaje.add(lblMensaje, BorderLayout.CENTER);
        norte.add(filaMensaje);
        add(norte, BorderLayout.NORTH);

        tabla.setName("tablaReportes");
        add(Ui.desplazable(tabla), BorderLayout.CENTER);
        lblResumen.setBorder(new EmptyBorder(2, 2, 0, 0));
        add(lblResumen, BorderLayout.SOUTH);

        cmbTipo.addActionListener(e -> ajustarFiltros());
        btnGenerar.addActionListener(e -> generar());
        btnExportar.addActionListener(e -> exportar());
        ajustarFiltros();
    }

    @Override
    public void refrescar() {
        Ui.ejecutarSilencioso(this, () -> {
            listaUsuarios.clear();
            listaUsuarios.addAll(usuarios.listar(usuario));
            cmbUsuario.removeAllItems();
            cmbUsuario.addItem("Todos los usuarios");
            listaUsuarios.forEach(u -> cmbUsuario.addItem(u.username()));
        }, m -> mensaje(m, true));
        if (ultimo == null) {
            generar();
        }
    }

    private void ajustarFiltros() {
        int tipo = cmbTipo.getSelectedIndex();
        boolean fechas = tipo != 5;
        spDesde.setEnabled(fechas);
        spHasta.setEnabled(fechas);
        for (java.awt.Component c : panelPeriodo.getComponents()) {
            c.setEnabled(fechas);
        }
        cmbUsuario.setEnabled(tipo == 6);
    }

    private void periodo(String clave) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = switch (clave) {
            case "hoy" -> hoy;
            case "semana" -> hoy.with(DayOfWeek.MONDAY);
            case "30" -> hoy.minusDays(29);
            default -> hoy.withDayOfMonth(1);
        };
        spDesde.setValue(Ui.aDate(desde));
        spHasta.setValue(Ui.aDate(hoy));
        generar();
    }

    private void mensaje(String texto, boolean error) {
        lblMensaje.setText(texto);
        lblMensaje.setForeground(error ? Tema.PELIGRO : Tema.SUAVE);
    }

    private void generar() {
        try {
            int tipo = cmbTipo.getSelectedIndex();
            LocalDate hoy = LocalDate.now();
            LocalDate desde = tipo == 5 ? hoy : Ui.fechaDe(spDesde, "La fecha inicial");
            LocalDate hasta = tipo == 5 ? hoy : Ui.fechaDe(spHasta, "La fecha final");
            TablaDatos datos = switch (tipo) {
                case 0 -> reportes.ventasPorPeriodo(usuario, Agrupacion.DIA, desde, hasta);
                case 1 -> reportes.ventasPorPeriodo(usuario, Agrupacion.SEMANA, desde, hasta);
                case 2 -> reportes.ventasPorPeriodo(usuario, Agrupacion.MES, desde, hasta);
                case 3 -> reportes.masVendidos(usuario, desde, hasta);
                case 4 -> reportes.ingresos(usuario, desde, hasta);
                case 5 -> reportes.existencias(usuario);
                default -> {
                    int indice = cmbUsuario.getSelectedIndex();
                    int id = indice <= 0 || indice > listaUsuarios.size() ? 0 : listaUsuarios.get(indice - 1).id();
                    yield reportes.operacionesPorUsuario(usuario, id, desde, hasta);
                }
            };
            ultimo = datos;
            Ui.cargar(modelo, datos);
            Ui.ajustarAnchos(tabla);
            mensaje(" ", false);
            lblResumen.setText(resumen(tipo, datos));
        } catch (NegocioException e) {
            mensaje(e.getMessage(), true);
        } catch (com.puntoventa.util.DataAccessException e) {
            mensaje("Ocurrió un problema con la base de datos: " + e.getMessage(), true);
        }
    }

    private String resumen(int tipo, TablaDatos datos) {
        int n = datos.filas().size();
        if (n == 0) {
            return "Sin resultados para el periodo seleccionado.";
        }
        String texto = n + (n == 1 ? " fila" : " filas");
        int col = datos.columnas().indexOf(tipo == 3 ? "Importe" : "Ingresos");
        if (col >= 0 && (tipo <= 3)) {
            BigDecimal suma = BigDecimal.ZERO;
            for (Object[] f : datos.filas()) {
                if (f[col] instanceof BigDecimal d) {
                    suma = suma.add(d);
                }
            }
            texto += "  ·  " + (tipo == 3 ? "Importe vendido" : "Ingresos del periodo") + ": " + Formato.dinero(suma);
        }
        return texto;
    }

    private void exportar() {
        if (ultimo == null || ultimo.filas().isEmpty()) {
            mensaje("Primero genere un reporte con resultados para poder exportarlo.", true);
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("reporte.csv"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            CsvExporter.guardar(ultimo, selector.getSelectedFile().toPath());
            Ui.aviso(this, "Reporte guardado en " + selector.getSelectedFile().getName(), Ui.Aviso.EXITO);
        } catch (RuntimeException e) {
            Ui.error(this, e.getMessage());
        }
    }
}
