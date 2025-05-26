package iser.apiOrion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iser.apiOrion.collection.TuyaSensorData;
import iser.apiOrion.repository.TuyaSensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio Tuya con URLs CORREGIDAS
 * 
 * CAMBIOS CRÍTICOS IMPLEMENTADOS:
 * ✅ URLs corregidas según especificación oficial de Tuya
 * ✅ /v1.0/iot-03/devices/{id}/status (era /v1.0/devices/{id}/status)
 * ✅ /v1.0/iot-03/devices/{id}/specification (era /v1.0/devices/{id}/specification)  
 * ✅ /v2.0/cloud/thing/{id}/report-logs (era /v1.0/devices/{id}/datapoints - NO EXISTÍA)
 * ✅ Mapeo completo de códigos reales del sensor
 * ✅ Logging detallado para debugging
 */
@Service
@EnableScheduling
public class TuyaSensorDataService {

    @Autowired
    private TuyaSensorDataRepository repository;

    // CONFIGURACIÓN DESDE application.properties
    @Value("${tuya.api.access-id}")
    private String clientId;

    @Value("${tuya.api.access-secret}")
    private String clientSecret;

    @Value("${tuya.api.base-url:https://openapi.tuyaus.com}")
    private String baseUrl;

    @Value("${tuya.api.device-id}")
    private String deviceId;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // CACHE INTELIGENTE DE ACCESS TOKEN
    private String cachedAccessToken = null;
    private long tokenExpirationTime = 0;

    /**
     * MÉTODO PRINCIPAL CORREGIDO: URLs actualizadas
     */
    public TuyaSensorData saveSensorData() {
        try {
            System.out.println("==========================================");
            System.out.println("🔄 TUYA URLs CORREGIDAS - INICIANDO CAPTURA");
            System.out.println("==========================================");

            // PASO 1: Obtener access_token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                System.err.println("❌ No se pudo obtener access_token");
                return null;
            }

            // PASO 2: Intentar obtener datos directamente de /iot-03/status (URL CORREGIDA)
            TuyaSensorData data = getDeviceStatusDirect(accessToken);
            if (data == null) {
                System.err.println("❌ Error obteniendo datos de /iot-03/status");
                return null;
            }

            /*
            // PASO 3: Si faltan parámetros, usar /report-logs (ENDPOINT CORRECTO)
            if (needsHistoricalData(data)) {
                System.out.println("🔍 Parámetros faltantes, buscando en report-logs...");
                enrichWithReportLogs(accessToken, data);
            }
             */
            
            // PASO 4: Guardar y mostrar resumen
            data.setTimestamp(LocalDateTime.now());
            TuyaSensorData saved = repository.save(data);
            
            logDataSummary(saved);
            return saved;

        } catch (Exception e) {
            System.err.println("❌ Error en saveSensorData: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * DEBUGGING: Raw response con URL CORREGIDA
     */
    public String getRawSensorResponse() {
        try {
            System.out.println("🐛 DEBUG - URLs corregidas para raw data...");
            
            String token = getAccessToken();
            if (token == null) {
                return "{\"error\":\"No se pudo obtener access_token\"}";
            }

            // URL CORREGIDA: /v1.0/iot-03/devices/{device_id}/status
            String path = "/v1.0/iot-03/devices/" + deviceId + "/status";
            String url = baseUrl + path;

            HttpHeaders headers = createTuyaAuthHeadersWithToken("GET", path, "", getAccessToken());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            String rawResponse = response.getBody();
            System.out.println("📡 RAW SENSOR RESPONSE (URL corregida): " + rawResponse);
            
            return rawResponse;

        } catch (Exception e) {
            String errorResponse = "{\"error\":\"" + e.getMessage() + "\"}";
            System.err.println("❌ Error obteniendo datos raw: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * ESPECIFICACIONES: Con URL CORREGIDA
     */
    public Map<String, Object> getDeviceSpecifications() {
        try {
            System.out.println("🔍 Obteniendo especificaciones (URL corregida)...");
            
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return Map.of("error", "No se pudo obtener access_token");
            }

            // URL CORREGIDA: /v1.0/iot-03/devices/{device_id}/specification
            String path = "/v1.0/iot-03/devices/" + deviceId + "/specification";
            String url = baseUrl + path;

            System.out.println("📡 Specifications URL (corregida): " + url);

            String token = null;
            HttpHeaders headers = createTuyaAuthHeadersWithToken("GET", path, "", token);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(response.getBody());

            if (!jsonResponse.path("success").asBoolean()) {
                return Map.of(
                    "error", "Error obteniendo especificaciones",
                    "code", jsonResponse.path("code").asInt(),
                    "message", jsonResponse.path("msg").asText(),
                    "raw_response", response.getBody()
                );
            }

            // PROCESAR ESPECIFICACIONES
            JsonNode result = jsonResponse.get("result");
            Map<String, Object> specifications = new HashMap<>();
            
            specifications.put("category", result.path("category").asText());
            specifications.put("raw_response", response.getBody());

            // PROCESAR STATUS DPs
            if (result.has("status") && result.get("status").isArray()) {
                List<Map<String, Object>> statusList = new ArrayList<>();
                for (JsonNode status : result.get("status")) {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("code", status.path("code").asText());
                    stat.put("name", status.path("name").asText());
                    stat.put("type", status.path("type").asText());
                    stat.put("values", status.has("values") ? status.get("values").asText() : "");
                    statusList.add(stat);
                }
                specifications.put("status", statusList);
                System.out.println("📊 Status DPs encontrados: " + statusList.size());
                
                // LOG DETALLADO DE CÓDIGOS ENCONTRADOS
                for (Map<String, Object> status : statusList) {
                    System.out.println("   📋 DP: " + status.get("code") + " (" + status.get("name") + ")");
                }
            }

            return specifications;

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo especificaciones: " + e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * SCHEDULER AUTOMÁTICO
     */
    @Scheduled(fixedRateString = "${tuya.polling.interval:30000}")
    public void scheduledPoll() {
        System.out.println("⏰ Polling automático (URLs corregidas) - " + LocalDateTime.now());
        saveSensorData();
    }

    /**
     * PASO 1: Obtener access_token (sin cambios)
     */
    private String getAccessToken() throws Exception {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            System.out.println("✅ Usando access_token desde cache");
            return cachedAccessToken;
        }

        System.out.println("🔑 Obteniendo nuevo access_token...");

        String path = "/v1.0/token?grant_type=1";
        String url = baseUrl + path;

        HttpHeaders headers = createTuyaAuthHeaders("GET", path, "");
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.readTree(response.getBody());

        if (!jsonResponse.path("success").asBoolean()) {
            System.err.println("❌ Error obteniendo access_token:");
            System.err.println("   Código: " + jsonResponse.path("code").asInt());
            System.err.println("   Mensaje: " + jsonResponse.path("msg").asText());
            return null;
        }

        JsonNode result = jsonResponse.get("result");
        String accessToken = result.get("access_token").asText();
        int expiresIn = result.get("expire_time").asInt();

        cachedAccessToken = accessToken;
        tokenExpirationTime = System.currentTimeMillis() + ((expiresIn - 300) * 1000L);

        System.out.println("✅ Access token obtenido exitosamente");
        return accessToken;
    }

    /**
     * PASO 2: Obtener estado con URL CORREGIDA
     */
    private TuyaSensorData getDeviceStatusDirect(String accessToken) throws Exception {
        System.out.println("📊 Obteniendo estado directo (URL corregida)...");

        // URL CORREGIDA: /v1.0/iot-03/devices/{device_id}/status
        String path = "/v1.0/iot-03/devices/" + deviceId + "/status";
        String url = baseUrl + path;

        System.out.println("📡 Status URL (corregida): " + url);

        HttpHeaders headers = createTuyaAuthHeadersWithToken("GET", path, "", accessToken);
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        System.out.println("📡 Status Response: " + response.getBody());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.readTree(response.getBody());

        if (!jsonResponse.path("success").asBoolean()) {
            System.err.println("❌ Error obteniendo estado del dispositivo:");
            System.err.println("   Código: " + jsonResponse.path("code").asInt());
            System.err.println("   Mensaje: " + jsonResponse.path("msg").asText());
            System.err.println("   Respuesta completa: " + response.getBody());
            return null;
        }

        // PROCESAR DATOS CON MAPEO COMPLETO
        TuyaSensorData data = new TuyaSensorData();
        data.setNombre("Sensor Tuya Multi-Parámetro");

        JsonNode result = jsonResponse.get("result");
        if (result != null && result.isArray()) {
            System.out.println("📈 Procesando " + result.size() + " códigos DP del /iot-03/status:");
            
            int parametrosProcesados = 0;
            
            for (JsonNode item : result) {
                String code = item.get("code").asText();
                double value = item.get("value").asDouble();
                System.out.println("   " + code + " = " + value);

                // MAPEO COMPLETO CON CÓDIGOS REALES
                switch (code) {
                    // === CÓDIGOS EXACTOS SEGÚN TU EJEMPLO ===
                    case "temp_current":
                        double tempValue = value > 100 ? value / 10.0 : value;
                        data.setTemperatura(tempValue);
                        System.out.println("   → Temperatura: " + tempValue + "°C");
                        parametrosProcesados++;
                        break;

                    /*case "ph_value":
                        double phValue = value > 14 ? value / 100.0 : value;
                        data.setPh(phValue);
                        System.out.println("   → pH: " + phValue);
                        parametrosProcesados++;
                        break;

                    case "orp_value":
                        data.setOrp(value);
                        System.out.println("   → ORP: " + value + " mV");
                        parametrosProcesados++;
                        break;

                    case "ec_value":
                        double ecValue = value > 1000 ? value / 1000.0 : value;
                        data.setEc(ecValue);
                        System.out.println("   → EC: " + ecValue + " mS/cm");
                        parametrosProcesados++;
                        break;

                    case "tds_value":
                        data.setTds(value);
                        System.out.println("   → TDS: " + value + " ppm");
                        parametrosProcesados++;
                        break;

                    case "salinity_value":
                        data.setSalinidad(value);
                        System.out.println("   → Salinidad: " + value + " ppm");
                        parametrosProcesados++;
                        break;
                        */

                    // === PARÁMETROS INFORMATIVOS ===
                    case "湿度值":
                        System.out.println("   → Humedad: " + value + "% (informativo)");
                        break;

                    default:
                        System.out.println("   ⚠️  Código no mapeado: " + code + " = " + value);
                        break;
                }
            }

            System.out.println("📊 Status directo - Parámetros procesados: " + parametrosProcesados + "/" + result.size());
        }

        return data;
    }

    /**
     * PASO 3: Verificar si necesitamos datos históricos
     */
    /*private boolean needsHistoricalData(TuyaSensorData data) {
        int nullCount = 0;
        int totalCriticalParams = 5; // pH, ORP, EC, TDS, Salinidad

        if (data.getPh() == null) nullCount++;
        if (data.getOrp() == null) nullCount++;
        if (data.getEc() == null) nullCount++;
        if (data.getTds() == null) nullCount++;
        if (data.getSalinidad() == null) nullCount++;

        boolean needsHistorical = nullCount > 0; // Si falta cualquier parámetro
        
        System.out.println("🔍 Análisis de datos faltantes:");
        System.out.println("   Parámetros críticos vacíos: " + nullCount + "/" + totalCriticalParams);
        System.out.println("   ¿Necesita histórico?: " + (needsHistorical ? "SÍ" : "NO"));

        return needsHistorical;
    }
        */

    /**
     * PASO 4: NUEVO - Usar /report-logs en lugar de /datapoints
     */
    private void enrichWithReportLogs(String accessToken, TuyaSensorData data) {
        try {
            System.out.println("📜 Buscando datos en report-logs (endpoint correcto)...");

            List<String> missingCodes = new ArrayList<>();
            //if (data.getPh() == null) missingCodes.add("ph_value");
            //if (data.getOrp() == null) missingCodes.add("orp_value");
            //if (data.getEc() == null) missingCodes.add("ec_value");
            //if (data.getTds() == null) missingCodes.add("tds_value");
            //if (data.getSalinidad() == null) missingCodes.add("salinity_value");

            if (missingCodes.isEmpty()) {
                System.out.println("✅ No hay parámetros faltantes");
                return;
            }

            System.out.println("🔍 Buscando códigos en report-logs: " + String.join(", ", missingCodes));

            Map<String, Double> reportData = fetchLatestReportLogs(accessToken, missingCodes);

            // MAPEAR DATOS DE REPORT-LOGS
           /* int foundInReports = 0;
            
            if (reportData.containsKey("ph_value") && data.getPh() == null) {
                data.setPh(reportData.get("ph_value"));
                System.out.println("✅ pH obtenido de report-logs: " + data.getPh());
                foundInReports++;
            }
            
            if (reportData.containsKey("orp_value") && data.getOrp() == null) {
                data.setOrp(reportData.get("orp_value"));
                System.out.println("✅ ORP obtenido de report-logs: " + data.getOrp());
                foundInReports++;
            }
            
            if (reportData.containsKey("ec_value") && data.getEc() == null) {
                data.setEc(reportData.get("ec_value"));
                System.out.println("✅ EC obtenido de report-logs: " + data.getEc());
                foundInReports++;
            }
            
            if (reportData.containsKey("tds_value") && data.getTds() == null) {
                data.setTds(reportData.get("tds_value"));
                System.out.println("✅ TDS obtenido de report-logs: " + data.getTds());
                foundInReports++;
            }
            
            if (reportData.containsKey("salinity_value") && data.getSalinidad() == null) {
                data.setSalinidad(reportData.get("salinity_value"));
                System.out.println("✅ Salinidad obtenida de report-logs: " + data.getSalinidad());
                foundInReports++;
            }

            System.out.println("📜 Datos de report-logs encontrados: " + foundInReports + "/" + missingCodes.size());
             */

        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo report-logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * NUEVO: Usar /v2.0/cloud/thing/{device_id}/report-logs
     */
    private Map<String, Double> fetchLatestReportLogs(String accessToken, List<String> codes) throws Exception {
        long now = System.currentTimeMillis();
        long oneHourAgo = now - 3_600_000L; // Última hora

        String codeParam = String.join(",", codes);
        
        // URL CORREGIDA: /v2.0/cloud/thing/{device_id}/report-logs
        String path = String.format(
            "/v2.0/cloud/thing/%s/report-logs?codes=%s&start_time=%d&end_time=%d&size=10",
            deviceId, codeParam, oneHourAgo, now
        );
        String url = baseUrl + path;

        System.out.println("📡 Report-logs URL (corregida): " + url);

        HttpHeaders headers = createTuyaAuthHeadersWithToken("GET", path, "", accessToken);
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        System.out.println("📡 Report-logs Response: " + response.getBody());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.readTree(response.getBody());

        Map<String, Double> reportMap = new HashMap<>();

        if (jsonResponse.path("success").asBoolean()) {
            JsonNode result = jsonResponse.get("result");
            if (result != null && result.isArray()) {
                for (JsonNode entry : result) {
                    String code = entry.get("code").asText();
                    double value = entry.get("value").asDouble();
                    long time = entry.get("time").asLong();
                    
                    // Tomar el más reciente de cada código
                    if (!reportMap.containsKey(code)) {
                        reportMap.put(code, value);
                        System.out.println("📜 Report-log encontrado: " + code + " = " + value + " (time: " + time + ")");
                    }
                }
            }
        } else {
            System.err.println("⚠️ Error en report-logs:");
            System.err.println("   Código: " + jsonResponse.path("code").asInt());
            System.err.println("   Mensaje: " + jsonResponse.path("msg").asText());
            System.err.println("   Respuesta completa: " + response.getBody());
        }

        return reportMap;
    }

    /**
     * LOGGING DETALLADO del resumen final
     */
    private void logDataSummary(TuyaSensorData data) {
        System.out.println("==========================================");
        System.out.println("📊 RESUMEN FINAL (URLs CORREGIDAS)");
        System.out.println("==========================================");
        System.out.println("   ID: " + data.getId());
        System.out.println("   Nombre: " + data.getNombre());
        System.out.println("   Timestamp: " + data.getTimestamp());

        int capturedParams = 0;
        int totalParams = 6;
        System.out.println("   📈 Parámetros capturados:");

        if (data.getTemperatura() != null) {
            System.out.println("     ✅ Temperatura: " + data.getTemperatura() + "°C");
            capturedParams++;
        } else {
            System.out.println("     ❌ Temperatura: N/A");
        }

       /*  if (data.getPh() != null) {
            System.out.println("     ✅ pH: " + data.getPh());
            capturedParams++;
        } else {
            System.out.println("     ❌ pH: N/A");
        }

        if (data.getOrp() != null) {
            System.out.println("     ✅ ORP: " + data.getOrp() + " mV");
            capturedParams++;
        } else {
            System.out.println("     ❌ ORP: N/A");
        }

        if (data.getEc() != null) {
            System.out.println("     ✅ EC: " + data.getEc() + " mS/cm");
            capturedParams++;
        } else {
            System.out.println("     ❌ EC: N/A");
        }

        if (data.getTds() != null) {
            System.out.println("     ✅ TDS: " + data.getTds() + " ppm");
            capturedParams++;
        } else {
            System.out.println("     ❌ TDS: N/A");
        }

        if (data.getSalinidad() != null) {
            System.out.println("     ✅ Salinidad: " + data.getSalinidad() + " ppm");
            capturedParams++;
        } else {
            System.out.println("     ❌ Salinidad: N/A");
        }
         */

        double successRate = (capturedParams * 100.0) / totalParams;
        System.out.println("   📊 Estadísticas finales:");
        System.out.println("     Parámetros capturados: " + capturedParams + "/" + totalParams);
        System.out.println("     Tasa de éxito: " + String.format("%.1f%%", successRate));
        
        if (successRate >= 80) {
            System.out.println("     Estado: ✅ EXCELENTE");
        } else if (successRate >= 50) {
            System.out.println("     Estado: ⚠️ PARCIAL");
        } else {
            System.out.println("     Estado: ❌ INSUFICIENTE");
        }
        
        System.out.println("==========================================");
    }

    // === MÉTODOS DE AUTENTICACIÓN (sin cambios) ===

    private HttpHeaders createTuyaAuthHeaders(String method, String path, String body) throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String bodyHash = sha256Hash(body);
        String stringToSign = method + "\n" + bodyHash + "\n\n" + path;
        String signStr = clientId + timestamp + nonce + stringToSign;
        String signature = hmacSha256(signStr, clientSecret).toUpperCase();

        HttpHeaders headers = new HttpHeaders();
        headers.set("client_id", clientId);
        headers.set("sign", signature);
        headers.set("t", String.valueOf(timestamp));
        headers.set("nonce", nonce);
        headers.set("sign_method", "HMAC-SHA256");
        
        return headers;
    }

    private HttpHeaders createTuyaAuthHeadersWithToken(String method, String path, String body, String accessToken) throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String bodyHash = sha256Hash(body);
        String stringToSign = method + "\n" + bodyHash + "\n\n" + path;
        String signStr = clientId + accessToken + timestamp + nonce + stringToSign;
        String signature = hmacSha256(signStr, clientSecret).toUpperCase();

        HttpHeaders headers = new HttpHeaders();
        headers.set("client_id", clientId);
        headers.set("access_token", accessToken);
        headers.set("sign", signature);
        headers.set("t", String.valueOf(timestamp));
        headers.set("nonce", nonce);
        headers.set("sign_method", "HMAC-SHA256");
        headers.set("Content-Type", "application/json");
        
        return headers;
    }

    private String sha256Hash(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // === MÉTODOS CRUD EXISTENTES (sin cambios) ===
    
    public List<TuyaSensorData> findAll() {
        return repository.findAll();
    }

    public Optional<TuyaSensorData> findById(String id) {
        return repository.findById(id);
    }

    public TuyaSensorData save(TuyaSensorData data) {
        return repository.save(data);
    }

    public TuyaSensorData update(String id, TuyaSensorData newData) {
        return repository.findById(id).map(existing -> {
            existing.setNombre(newData.getNombre());
            //existing.setPh(newData.getPh());
            //existing.setOrp(newData.getOrp());
            //existing.setEc(newData.getEc());
            //existing.setTds(newData.getTds());
            //existing.setSalinidad(newData.getSalinidad());
            existing.setTemperatura(newData.getTemperatura());
            existing.setTimestamp(newData.getTimestamp());
            return repository.save(existing);
        }).orElse(null);
    }

    public boolean deleteById(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}









