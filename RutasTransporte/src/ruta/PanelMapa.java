package ruta;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.event.MouseInputListener;

/**
 * Mapa (OpenStreetMap vía JXMapViewer2) que dibuja la lista ligada de paradas
 * recorriendo los nodos directamente: línea de la ruta + marcadores numerados.
 */
public class PanelMapa extends JXMapViewer {

    /** Avisa a la ventana cuando el usuario hace clic en el mapa. */
    public interface EscuchaMapa {
        void paradaClic(int indice);
        void puntoClic(double latitud, double longitud);
    }

    private static final int RADIO = 13;

    private ListaRuta ruta;
    private int seleccionada = -1;
    private EscuchaMapa escucha;

    public PanelMapa(ListaRuta ruta) {
        this.ruta = ruta;

        System.setProperty("http.agent", "RutasTransporte/1.0 (proyecto academico)");
        TileFactory fabrica = new DefaultTileFactory(
                new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org"));
        setTileFactory(fabrica);
        setZoom(5);
        setAddressLocation(new GeoPosition(6.2442, -75.5812)); // Medellín

        MouseInputListener arrastre = new PanMouseInputListener(this);
        addMouseListener(arrastre);
        addMouseMotionListener(arrastre);
        addMouseWheelListener(new ZoomMouseWheelListenerCenter(this));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (escucha == null || e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int indice = paradaEn(e.getPoint());
                if (indice >= 0) {
                    escucha.paradaClic(indice);
                } else {
                    GeoPosition g = convertPointToGeoPosition(e.getPoint());
                    escucha.puntoClic(g.getLatitude(), g.getLongitude());
                }
            }
        });

        setOverlayPainter(new Painter<JXMapViewer>() {
            @Override
            public void paint(Graphics2D g, JXMapViewer mapa, int w, int h) {
                dibujarRuta(g, mapa);
            }
        });
    }

    public void setRuta(ListaRuta ruta) { this.ruta = ruta; repaint(); }
    public void setEscucha(EscuchaMapa escucha) { this.escucha = escucha; }

    public void setSeleccionada(int indice) {
        this.seleccionada = indice;
        repaint();
    }

    /** Centra el mapa en la parada indicada. */
    public void centrarEn(int indice) {
        if (indice < 0 || indice >= ruta.tamano()) {
            return;
        }
        Parada p = ruta.obtener(indice);
        setCenterPosition(new GeoPosition(p.getLatitud(), p.getLongitud()));
    }

    /** Ajusta zoom y centro para que toda la ruta quede visible. */
    public void ajustarARuta() {
        if (ruta.estaVacia()) {
            return;
        }
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (Nodo n = ruta.getCabeza(); n != null; n = n.getSiguiente()) {
            Parada p = n.getDato();
            minLat = Math.min(minLat, p.getLatitud());
            maxLat = Math.max(maxLat, p.getLatitud());
            minLon = Math.min(minLon, p.getLongitud());
            maxLon = Math.max(maxLon, p.getLongitud());
        }
        TileFactory tf = getTileFactory();
        int ancho = Math.max(getWidth(), 400);
        int alto = Math.max(getHeight(), 300);
        int zoomFinal = tf.getInfo().getMaximumZoomLevel();
        for (int z = 0; z <= tf.getInfo().getMaximumZoomLevel(); z++) {
            Point2D a = tf.geoToPixel(new GeoPosition(maxLat, minLon), z);
            Point2D b = tf.geoToPixel(new GeoPosition(minLat, maxLon), z);
            if (Math.abs(b.getX() - a.getX()) <= ancho * 0.75 && Math.abs(b.getY() - a.getY()) <= alto * 0.75) {
                zoomFinal = z;
                break;
            }
        }
        setZoom(zoomFinal);
        setCenterPosition(new GeoPosition((minLat + maxLat) / 2, (minLon + maxLon) / 2));
    }

    private Point2D pixel(Parada p) {
        return getTileFactory().geoToPixel(new GeoPosition(p.getLatitud(), p.getLongitud()), getZoom());
    }

    /** Índice de la parada bajo el punto (coordenadas del componente) o -1. */
    private int paradaEn(Point clic) {
        Rectangle vista = getViewportBounds();
        int i = 0;
        for (Nodo n = ruta.getCabeza(); n != null; n = n.getSiguiente()) {
            Point2D px = pixel(n.getDato());
            double dx = px.getX() - vista.x - clic.x;
            double dy = px.getY() - vista.y - clic.y;
            if (dx * dx + dy * dy <= (RADIO + 3) * (RADIO + 3)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void dibujarRuta(Graphics2D g0, JXMapViewer mapa) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle vista = mapa.getViewportBounds();
        g.translate(-vista.x, -vista.y);

        // Línea de la ruta: recorre los nodos enlazados
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x1E, 0x6F, 0xD9, 200));
        for (Nodo n = ruta.getCabeza(); n != null && n.getSiguiente() != null; n = n.getSiguiente()) {
            Point2D a = pixel(n.getDato());
            Point2D b = pixel(n.getSiguiente().getDato());
            g.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
        }

        // Marcadores numerados
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int i = 0;
        for (Nodo n = ruta.getCabeza(); n != null; n = n.getSiguiente()) {
            Point2D p = pixel(n.getDato());
            int x = (int) p.getX();
            int y = (int) p.getY();
            boolean sel = (i == seleccionada);
            int r = sel ? RADIO + 3 : RADIO;
            g.setColor(Color.WHITE);
            g.fillOval(x - r - 2, y - r - 2, 2 * r + 4, 2 * r + 4);
            g.setColor(sel ? new Color(0xD3, 0x2F, 0x2F) : new Color(0x1E, 0x6F, 0xD9));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(Color.WHITE);
            String num = String.valueOf(i + 1);
            g.drawString(num, x - fm.stringWidth(num) / 2, y + fm.getAscent() / 2 - 1);
            if (sel) {
                String nombre = n.getDato().getNombre();
                int w = fm.stringWidth(nombre) + 10;
                g.setColor(new Color(0, 0, 0, 190));
                g.fillRoundRect(x - w / 2, y - r - 26, w, 20, 8, 8);
                g.setColor(Color.WHITE);
                g.drawString(nombre, x - w / 2 + 5, y - r - 11);
            }
            i++;
        }
        g.dispose();
    }
}
