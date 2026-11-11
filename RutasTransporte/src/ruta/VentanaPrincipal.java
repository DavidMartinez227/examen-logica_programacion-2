package ruta;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

/**
 * Ventana principal: mapa + formulario + tabla de paradas.
 */
public class VentanaPrincipal extends JFrame implements PanelMapa.EscuchaMapa {

    private ListaRuta ruta = new ListaRuta();
    private final PanelMapa mapa;
    private final ModeloTabla modelo = new ModeloTabla();
    private final JTable tabla = new JTable(modelo);

    private final JTextField txtNombre = new JTextField();
    private final JTextField txtLatitud = new JTextField();
    private final JTextField txtLongitud = new JTextField();
    private final JTextField txtTiempo = new JTextField("0");
    private final JTextField txtDescripcion = new JTextField();
    private final JCheckBox chkDespues = new JCheckBox("Insertar después de la parada seleccionada");
    private final JLabel lblEstado = new JLabel(" ");

    private boolean actualizando = false;
    private File archivoActual = null;

    public VentanaPrincipal() {
        super("Gestor de rutas de transporte - Lista ligada");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        mapa = new PanelMapa(ruta);
        mapa.setEscucha(this);

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapa, construirPanelDerecho());
        division.setResizeWeight(1.0);
        division.setDividerLocation(700);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(division, BorderLayout.CENTER);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        getContentPane().add(lblEstado, BorderLayout.SOUTH);

        setSize(1200, 720);
        setLocationRelativeTo(null);
        actualizarEstado();
    }

    // ---------------------------------------------------------------- UI

    private JPanel construirPanelDerecho() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(460, 600));
        panel.setMinimumSize(new Dimension(380, 300));

        // Formulario
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Datos de la parada"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        agregarCampo(form, c, 0, "Nombre *", txtNombre);
        agregarCampo(form, c, 1, "Latitud *", txtLatitud);
        agregarCampo(form, c, 2, "Longitud *", txtLongitud);
        agregarCampo(form, c, 3, "Tiempo estimado (min)", txtTiempo);
        agregarCampo(form, c, 4, "Descripción (opcional)", txtDescripcion);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        form.add(chkDespues, c);
        c.gridy = 6;
        JLabel ayuda = new JLabel("<html><i>Clic en el mapa: llena latitud/longitud. "
                + "Clic en un marcador: selecciona la parada.</i></html>");
        form.add(ayuda, c);

        // Botones
        JPanel botones = new JPanel(new GridLayout(2, 4, 5, 5));
        botones.add(boton("Agregar", () -> agregar()));
        botones.add(boton("Modificar", () -> modificar()));
        botones.add(boton("Eliminar", () -> eliminar()));
        botones.add(boton("Limpiar", () -> limpiarFormulario()));
        botones.add(boton("Mostrar ruta", () -> mostrarRuta()));
        botones.add(boton("Distancia", () -> mostrarDistancia()));
        botones.add(boton("Guardar", () -> guardar()));
        botones.add(boton("Cargar", () -> cargar()));

        JPanel arriba = new JPanel(new BorderLayout(0, 8));
        arriba.add(form, BorderLayout.CENTER);
        arriba.add(botones, BorderLayout.SOUTH);

        // Tabla
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(0).setMaxWidth(35);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(4).setMaxWidth(40);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !actualizando) {
                seleccionarFila(tabla.getSelectedRow());
            }
        });
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Paradas de la ruta"));

        panel.add(arriba, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void agregarCampo(JPanel form, GridBagConstraints c, int fila, String texto, JTextField campo) {
        c.gridwidth = 1;
        c.gridy = fila;
        c.gridx = 0; c.weightx = 0;
        form.add(new JLabel(texto), c);
        c.gridx = 1; c.weightx = 1;
        form.add(campo, c);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    // ------------------------------------------------------- Operaciones

    private void agregar() {
        try {
            Parada p = leerFormulario();
            int fila = tabla.getSelectedRow();
            int posicion = ruta.tamano();
            if (chkDespues.isSelected() && fila >= 0) {
                posicion = fila + 1;
            }
            ruta.insertar(posicion, p);
            refrescar(posicion);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void modificar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            error("Seleccione primero una parada de la tabla o del mapa.");
            return;
        }
        try {
            Parada p = leerFormulario();
            ruta.modificar(fila, p.getNombre(), p.getLatitud(), p.getLongitud(),
                    p.getTiempoEstimado(), p.getDescripcion());
            refrescar(fila);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            error(ruta.estaVacia() ? "La ruta no tiene paradas." : "Seleccione la parada que desea eliminar.");
            return;
        }
        Parada p = ruta.obtener(fila);
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar la parada \"" + p.getNombre() + "\"?",
                "Eliminar parada", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ruta.eliminar(fila);
            limpiarFormulario();
            refrescar(-1);
        }
    }

    private void mostrarRuta() {
        mapa.ajustarARuta();
        JTextArea area = new JTextArea(ruta.mostrar(), 12, 60);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Paradas de la ruta",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarDistancia() {
        if (ruta.tamano() < 2) {
            error("Se necesitan al menos 2 paradas para calcular la distancia.");
            return;
        }
        JOptionPane.showMessageDialog(this,
                String.format("Distancia total aproximada: %.2f km%nTiempo total estimado: %d min",
                        ruta.distanciaTotalKm(), ruta.tiempoTotal()),
                "Distancia de la ruta", JOptionPane.INFORMATION_MESSAGE);
    }

    private void guardar() {
        if (ruta.estaVacia()) {
            error("No hay paradas para guardar.");
            return;
        }
        JFileChooser fc = selectorArchivo();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".json")) {
            f = new File(f.getParentFile(), f.getName() + ".json");
        }
        if (f.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Sobrescribir?",
                "Guardar ruta", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            GestorJSON.guardar(ruta, f);
            archivoActual = f;
            JOptionPane.showMessageDialog(this, "Ruta guardada en:\n" + f.getAbsolutePath());
        } catch (IOException ex) {
            error("No se pudo guardar: " + ex.getMessage());
        }
    }

    private void cargar() {
        JFileChooser fc = selectorArchivo();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        cargarArchivo(fc.getSelectedFile(), true);
    }

    /** Carga un archivo JSON y reemplaza la lista ligada en memoria. */
    public void cargarArchivo(File f, boolean avisar) {
        try {
            ListaRuta nueva = GestorJSON.cargar(f);
            ruta = nueva;
            archivoActual = f;
            mapa.setRuta(ruta);
            limpiarFormulario();
            refrescar(-1);
            mapa.ajustarARuta();
            if (avisar) {
                JOptionPane.showMessageDialog(this, "Se cargaron " + ruta.tamano() + " paradas.");
            }
        } catch (IOException ex) {
            if (avisar) {
                error("No se pudo cargar el archivo: " + ex.getMessage());
            }
        }
    }

    private JFileChooser selectorArchivo() {
        JFileChooser fc = new JFileChooser(archivoActual != null ? archivoActual.getParentFile() : new File("."));
        fc.setFileFilter(new FileNameExtensionFilter("Archivos JSON (*.json)", "json"));
        return fc;
    }

    // ----------------------------------------------------------- Soporte

    private Parada leerFormulario() {
        String nombre = txtNombre.getText();
        double lat = numero(txtLatitud.getText(), "La latitud");
        double lon = numero(txtLongitud.getText(), "La longitud");
        int tiempo;
        try {
            String t = txtTiempo.getText().trim();
            tiempo = t.isEmpty() ? 0 : Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El tiempo estimado debe ser un número entero de minutos.");
        }
        return new Parada(nombre, lat, lon, tiempo, txtDescripcion.getText());
    }

    private double numero(String texto, String campo) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un número (ej. 6.2442).");
        }
    }

    /** Actualiza tabla, mapa y barra de estado; selecciona la fila indicada (o ninguna con -1). */
    private void refrescar(int filaSeleccionar) {
        actualizando = true;
        modelo.fireTableDataChanged();
        if (filaSeleccionar >= 0 && filaSeleccionar < ruta.tamano()) {
            tabla.setRowSelectionInterval(filaSeleccionar, filaSeleccionar);
        } else {
            tabla.clearSelection();
        }
        actualizando = false;
        seleccionarFila(filaSeleccionar >= 0 && filaSeleccionar < ruta.tamano() ? filaSeleccionar : -1);
        actualizarEstado();
    }

    /** "Seleccionar parada": pasa los datos al formulario y resalta/centra en el mapa. */
    private void seleccionarFila(int fila) {
        mapa.setSeleccionada(fila);
        if (fila >= 0 && fila < ruta.tamano()) {
            Parada p = ruta.obtener(fila);
            txtNombre.setText(p.getNombre());
            txtLatitud.setText(String.valueOf(p.getLatitud()));
            txtLongitud.setText(String.valueOf(p.getLongitud()));
            txtTiempo.setText(String.valueOf(p.getTiempoEstimado()));
            txtDescripcion.setText(p.getDescripcion());
            mapa.centrarEn(fila);
        }
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtLatitud.setText("");
        txtLongitud.setText("");
        txtTiempo.setText("0");
        txtDescripcion.setText("");
        actualizando = true;
        tabla.clearSelection();
        actualizando = false;
        mapa.setSeleccionada(-1);
    }

    private void actualizarEstado() {
        lblEstado.setText(String.format("Paradas: %d   |   Distancia total aproximada: %.2f km   |   Tiempo total: %d min%s",
                ruta.tamano(), ruta.distanciaTotalKm(), ruta.tiempoTotal(),
                archivoActual != null ? "   |   Archivo: " + archivoActual.getName() : ""));
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    // ------------------------------------------------- Eventos del mapa

    @Override
    public void paradaClic(int indice) {
        tabla.setRowSelectionInterval(indice, indice);
    }

    @Override
    public void puntoClic(double latitud, double longitud) {
        txtLatitud.setText(String.format(java.util.Locale.US, "%.6f", latitud));
        txtLongitud.setText(String.format(java.util.Locale.US, "%.6f", longitud));
    }

    // ----------------------------------------------- Modelo de la tabla

    /** Lee directamente de la lista ligada (no copia los datos a otra colección). */
    private class ModeloTabla extends AbstractTableModel {
        private final String[] columnas = {"#", "Nombre", "Latitud", "Longitud", "Min", "Descripción"};

        @Override public int getRowCount() { return ruta.tamano(); }
        @Override public int getColumnCount() { return columnas.length; }
        @Override public String getColumnName(int c) { return columnas[c]; }

        @Override
        public Object getValueAt(int fila, int col) {
            Parada p = ruta.obtener(fila);
            switch (col) {
                case 0: return fila + 1;
                case 1: return p.getNombre();
                case 2: return String.format(java.util.Locale.US, "%.5f", p.getLatitud());
                case 3: return String.format(java.util.Locale.US, "%.5f", p.getLongitud());
                case 4: return p.getTiempoEstimado();
                default: return p.getDescripcion();
            }
        }
    }
}
