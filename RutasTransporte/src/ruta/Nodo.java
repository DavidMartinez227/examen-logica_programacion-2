package ruta;

/**
 * Nodo de la lista ligada: guarda una parada y la referencia al siguiente nodo.
 */
public class Nodo {

    Parada dato;
    Nodo siguiente;

    public Nodo(Parada dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public Parada getDato() { return dato; }
    public Nodo getSiguiente() { return siguiente; }
}
