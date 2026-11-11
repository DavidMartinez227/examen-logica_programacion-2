package ruta;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se usa el aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal v = new VentanaPrincipal();
            v.setVisible(true);
            File ejemplo = new File("datos/Medellin.json");
            if (ejemplo.exists()) {
                SwingUtilities.invokeLater(() -> v.cargarArchivo(ejemplo, false));
            }
        });
    }
}
