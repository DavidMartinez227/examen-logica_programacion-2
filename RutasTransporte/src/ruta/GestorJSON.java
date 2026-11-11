package ruta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

/**
 * Guarda y carga la ruta en un archivo JSON usando Jackson.
 * Formato: un arreglo de objetos {nombre, latitud, longitud, tiempoEstimado, descripcion}.
 */
public class GestorJSON {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void guardar(ListaRuta ruta, File archivo) throws IOException {
        ArrayNode arreglo = MAPPER.createArrayNode();
        for (Nodo n = ruta.getCabeza(); n != null; n = n.getSiguiente()) {
            Parada p = n.getDato();
            ObjectNode o = arreglo.addObject();
            o.put("nombre", p.getNombre());
            o.put("latitud", p.getLatitud());
            o.put("longitud", p.getLongitud());
            o.put("tiempoEstimado", p.getTiempoEstimado());
            o.put("descripcion", p.getDescripcion());
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(archivo, arreglo);
    }

    /**
     * Lee el archivo y devuelve una lista ligada nueva.
     * Si el archivo es inválido lanza IOException y no modifica nada.
     */
    public static ListaRuta cargar(File archivo) throws IOException {
        JsonNode raiz = MAPPER.readTree(archivo);
        if (raiz == null || !raiz.isArray()) {
            throw new IOException("El archivo no contiene un arreglo JSON de paradas.");
        }
        ListaRuta ruta = new ListaRuta();
        int fila = 0;
        for (JsonNode o : raiz) {
            fila++;
            try {
                if (!o.hasNonNull("nombre") || !o.hasNonNull("latitud") || !o.hasNonNull("longitud")) {
                    throw new IllegalArgumentException("faltan nombre, latitud o longitud.");
                }
                Parada p = new Parada(
                        o.get("nombre").asText(),
                        o.get("latitud").asDouble(Double.NaN),
                        o.get("longitud").asDouble(Double.NaN),
                        o.path("tiempoEstimado").asInt(0),
                        o.path("descripcion").asText(""));
                ruta.agregar(p);
            } catch (IllegalArgumentException e) {
                throw new IOException("Parada #" + fila + " inválida: " + e.getMessage());
            }
        }
        return ruta;
    }
}
