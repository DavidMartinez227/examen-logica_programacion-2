package ruta;

/**
 * Lista ligada simple de paradas, implementada desde cero
 * (sin java.util.List / LinkedList / ArrayList).
 */
public class ListaRuta {

    private Nodo cabeza;
    private int tamano;

    public ListaRuta() {
        cabeza = null;
        tamano = 0;
    }

    public Nodo getCabeza() { return cabeza; }
    public int tamano() { return tamano; }
    public boolean estaVacia() { return cabeza == null; }

    /** Agrega una parada al final de la ruta. */
    public void agregar(Parada p) {
        Nodo nuevo = new Nodo(p);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            Nodo actual = cabeza;
            while (actual.siguiente != null) {
                actual = actual.siguiente;
            }
            actual.siguiente = nuevo;
        }
        tamano++;
    }

    /** Inserta una parada en la posición indicada (0 = al inicio, tamano = al final). */
    public void insertar(int indice, Parada p) {
        if (indice < 0 || indice > tamano) {
            throw new IndexOutOfBoundsException("Posición inválida: " + indice);
        }
        if (indice == 0) {
            Nodo nuevo = new Nodo(p);
            nuevo.siguiente = cabeza;
            cabeza = nuevo;
            tamano++;
        } else if (indice == tamano) {
            agregar(p);
        } else {
            Nodo anterior = nodoEn(indice - 1);
            Nodo nuevo = new Nodo(p);
            nuevo.siguiente = anterior.siguiente;
            anterior.siguiente = nuevo;
            tamano++;
        }
    }

    private Nodo nodoEn(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Posición inválida: " + indice);
        }
        Nodo actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual;
    }

    /** Selecciona (obtiene) la parada en la posición indicada. */
    public Parada obtener(int indice) {
        return nodoEn(indice).dato;
    }

    /** Busca una parada por nombre (sin distinguir mayúsculas). Devuelve su posición o -1. */
    public int buscarPorNombre(String nombre) {
        int i = 0;
        for (Nodo n = cabeza; n != null; n = n.siguiente) {
            if (n.dato.getNombre().equalsIgnoreCase(nombre.trim())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /** Modifica los datos de la parada en la posición indicada. */
    public void modificar(int indice, String nombre, double lat, double lon, int tiempo, String descripcion) {
        nodoEn(indice).dato.modificar(nombre, lat, lon, tiempo, descripcion);
    }

    /** Elimina la parada en la posición indicada y la devuelve. */
    public Parada eliminar(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Posición inválida: " + indice);
        }
        Parada eliminada;
        if (indice == 0) {
            eliminada = cabeza.dato;
            cabeza = cabeza.siguiente;
        } else {
            Nodo anterior = nodoEn(indice - 1);
            eliminada = anterior.siguiente.dato;
            anterior.siguiente = anterior.siguiente.siguiente;
        }
        tamano--;
        return eliminada;
    }

    /** Vacía la ruta. */
    public void vaciar() {
        cabeza = null;
        tamano = 0;
    }

    /** Distancia total aproximada (km): suma de los tramos entre paradas consecutivas. */
    public double distanciaTotalKm() {
        double total = 0;
        for (Nodo n = cabeza; n != null && n.siguiente != null; n = n.siguiente) {
            total += n.dato.distanciaKm(n.siguiente.dato);
        }
        return total;
    }

    /** Suma de los tiempos estimados de todas las paradas (minutos). */
    public int tiempoTotal() {
        int total = 0;
        for (Nodo n = cabeza; n != null; n = n.siguiente) {
            total += n.dato.getTiempoEstimado();
        }
        return total;
    }

    /** Texto con el recorrido completo de la ruta. */
    public String mostrar() {
        if (cabeza == null) {
            return "La ruta no tiene paradas.";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Nodo n = cabeza; n != null; n = n.siguiente) {
            Parada p = n.dato;
            sb.append(i).append(". ").append(p.getNombre())
              .append("  [").append(String.format("%.5f, %.5f", p.getLatitud(), p.getLongitud())).append("]")
              .append("  ").append(p.getTiempoEstimado()).append(" min");
            if (!p.getDescripcion().isEmpty()) {
                sb.append("  - ").append(p.getDescripcion());
            }
            sb.append("\n");
            i++;
        }
        return sb.toString();
    }
}
