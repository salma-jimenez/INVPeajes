package com.invpeajes.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtiene la ruta real por carretera entre dos puntos.
 *
 * <p>Usa OSRM (Open Source Routing Machine), un motor de rutas libre que trabaja sobre el
 * grafo de vías de OpenStreetMap: cada vía es una lista de nodos (puntos con latitud y
 * longitud) y el motor busca el camino más corto entre ellos. La respuesta trae todos los
 * nodos del recorrido, así que al dibujarlos la línea sigue la carretera.</p>
 *
 * <p>Se usa el servidor público de OSRM: es gratis y no pide clave, pero es para uso
 * moderado (más o menos una consulta por segundo). Si no hay internet o el servidor no
 * responde, {@link #findRoute} devuelve null y quien lo llama usa el cálculo aproximado.</p>
 *
 * @author Equipo INVPeajes
 * @version 1.0
 */
public final class RouteService {

    private static final String OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final String USER_AGENT = "INVPeajes-ProyectoAula/1.0 (Java)";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final String NUMBER = "(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)";
    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern PAIR = Pattern.compile("\\[\\s*" + NUMBER + "\\s*,\\s*" + NUMBER + "\\s*\\]");
    private static final Pattern DISTANCE = Pattern.compile("\"distance\"\\s*:\\s*" + NUMBER);
    private static final Pattern DURATION = Pattern.compile("\"duration\"\\s*:\\s*" + NUMBER);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private RouteService() {
    }

    /**
     * Pide la ruta por carretera entre dos puntos.
     *
     * Hace una consulta por internet: llamarlo fuera del hilo de la interfaz (por ejemplo
     * dentro de {@code Async.run}).
     *
     * @param originLat latitud del origen
     * @param originLon longitud del origen
     * @param destLat   latitud del destino
     * @param destLon   longitud del destino
     * @return la ruta, o null si no se pudo obtener (sin internet, servidor caído, sin vía)
     */
    public static RoadRoute findRoute(double originLat, double originLon, double destLat, double destLon) {
        String coordinates = String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f",
                originLon, originLat, destLon, destLat);
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(OSRM_URL + coordinates + "?overview=full&geometries=geojson&steps=false"))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return parse(response.statusCode(), response.body());
        } catch (IOException e) {
            System.err.println("Ruta por carretera no disponible: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Lee la respuesta JSON de OSRM. Se hace con expresiones regulares para no agregar
     * una librería de JSON al proyecto: solo se necesitan el código, las coordenadas de
     * la línea, la distancia y la duración.
     *
     * @param status código HTTP de la respuesta
     * @param json   cuerpo de la respuesta
     * @return la ruta
     * @throws IOException si la respuesta no trae una ruta válida
     */
    static RoadRoute parse(int status, String json) throws IOException {
        if (json == null || json.isEmpty()) {
            throw new IOException("respuesta vacía (HTTP " + status + ")");
        }
        Matcher code = CODE.matcher(json);
        if (status != 200 || !code.find() || !"Ok".equals(code.group(1))) {
            Matcher message = MESSAGE.matcher(json);
            throw new IOException("el servidor no dio ruta (HTTP " + status + ")"
                    + (message.find() ? ": " + message.group(1) : ""));
        }
        int routes = json.indexOf("\"routes\"");
        int coords = routes < 0 ? -1 : json.indexOf("\"coordinates\"", routes);
        int start = coords < 0 ? -1 : json.indexOf('[', coords);
        int end = start < 0 ? -1 : json.indexOf("]]", start);
        if (end < 0) {
            throw new IOException("la respuesta no trae el trazado de la ruta");
        }

        // GeoJSON guarda cada punto como [longitud, latitud]
        List<double[]> points = new ArrayList<>();
        Matcher pair = PAIR.matcher(json).region(start + 1, end + 1);
        while (pair.find()) {
            double lon = Double.parseDouble(pair.group(1));
            double lat = Double.parseDouble(pair.group(2));
            points.add(new double[]{lat, lon});
        }
        if (points.size() < 2) {
            throw new IOException("la ruta tiene menos de dos puntos");
        }

        // Después del trazado vienen los tramos (legs) con la distancia (m) y duración (s)
        Matcher distance = DISTANCE.matcher(json).region(end, json.length());
        Matcher duration = DURATION.matcher(json).region(end, json.length());
        if (!distance.find() || !duration.find()) {
            throw new IOException("la respuesta no trae distancia ni duración");
        }
        return new RoadRoute(points,
                Double.parseDouble(distance.group(1)) / 1000.0,
                Double.parseDouble(duration.group(1)) / 60.0);
    }

    /** Ruta por carretera: los puntos de la vía, la distancia y el tiempo estimado. */
    public static final class RoadRoute {
        private final List<double[]> points;
        private final double distanceKm;
        private final double durationMinutes;

        /**
         * @param points          puntos de la vía en orden, cada uno {latitud, longitud}
         * @param distanceKm      distancia total por carretera en km
         * @param durationMinutes tiempo estimado de viaje en minutos
         */
        public RoadRoute(List<double[]> points, double distanceKm, double durationMinutes) {
            this.points = Collections.unmodifiableList(new ArrayList<>(points));
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
        }

        /** @return puntos de la vía en orden; cada uno es {latitud, longitud} */
        public List<double[]> getPoints() {
            return points;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public double getDurationMinutes() {
            return durationMinutes;
        }

        /** @return duración legible, por ejemplo "8 h 20 min" */
        public String durationText() {
            long minutes = Math.round(durationMinutes);
            long h = minutes / 60;
            long m = minutes % 60;
            return h > 0 ? h + " h " + m + " min" : m + " min";
        }
    }
}
