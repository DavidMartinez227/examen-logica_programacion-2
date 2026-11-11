package ruta;

/**
 * Datos de una parada de la ruta.
 */
public class Parada {

    private String nombre;
    private double latitud;
    private double longitud;
    private int tiempoEstimado; // minutos
    private String descripcion; // opcional

    public Parada(String nombre, double latitud, double longitud, int tiempoEstimado, String descripcion) {
        validar(nombre, latitud, longitud, tiempoEstimado);
        this.nombre = nombre.trim();
        this.latitud = latitud;
        this.longitud = longitud;
        this.tiempoEstimado = tiempoEstimado;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
    }

    /** Valida los datos; lanza IllegalArgumentException si algo es inválido. */
    public static void validar(String nombre, double latitud, double longitud, int tiempoEstimado) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la parada es obligatorio.");
        }
        if (Double.isNaN(latitud) || latitud < -90 || latitud > 90) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90.");
        }
        if (Double.isNaN(longitud) || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180.");
        }
        if (tiempoEstimado < 0) {
            throw new IllegalArgumentException("El tiempo estimado no puede ser negativo.");
        }
    }

    /** Distancia en kilómetros a otra parada (fórmula de Haversine). */
    public double distanciaKm(Parada otra) {
        final double R = 6371.0;
        double dLat = Math.toRadians(otra.latitud - latitud);
        double dLon = Math.toRadians(otra.longitud - longitud);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitud)) * Math.cos(Math.toRadians(otra.latitud))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public String getNombre() { return nombre; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public int getTiempoEstimado() { return tiempoEstimado; }
    public String getDescripcion() { return descripcion; }

    /** Reemplaza todos los datos de la parada (validando primero). */
    public void modificar(String nombre, double latitud, double longitud, int tiempoEstimado, String descripcion) {
        validar(nombre, latitud, longitud, tiempoEstimado);
        this.nombre = nombre.trim();
        this.latitud = latitud;
        this.longitud = longitud;
        this.tiempoEstimado = tiempoEstimado;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
    }

    @Override
    public String toString() {
        return nombre + " (" + latitud + ", " + longitud + ")";
    }
}
