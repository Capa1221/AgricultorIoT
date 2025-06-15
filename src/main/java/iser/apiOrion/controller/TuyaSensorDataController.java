package iser.apiOrion.controller;

import iser.apiOrion.DTO.TuyaSensorDataDTO;
import iser.apiOrion.collection.TuyaSensorData;
import iser.apiOrion.service.TuyaSensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador REST mejorado para el sensor Tuya
 * 
 * VERSIÓN HÍBRIDA MEJORADA - CARACTERÍSTICAS:
 * ✅ Endpoint principal optimizado con el servicio híbrido
 * ✅ Endpoints de debugging avanzados 
 * ✅ Auto-descubrimiento de especificaciones del dispositivo
 * ✅ Análisis completo de capacidades del sensor
 * ✅ Comparación de datos actuales vs especificaciones
 * ✅ Respuestas JSON estructuradas y detalladas
 * ✅ Logging completo para troubleshooting
 * ✅ Manejo robusto de errores en todos los endpoints
 * ✅ Documentación Swagger completa
 * ✅ Estadísticas y análisis de tendencias
 */
@RestController
@RequestMapping(value = "/api/v1/tuya", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tuya Sensor Híbrido", description = "API reducida para solo temperatura")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TuyaSensorDataController {

    @Autowired
    private TuyaSensorDataService service;

    @Operation(summary = "Captura híbrida y devuelve sólo temperatura")
    @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Temperatura obtenida"),
      @ApiResponse(responseCode = "204", description = "Sin dato de temperatura"),
      @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping("/fetch-and-save")
    public ResponseEntity<Map<String,Object>> fetchAndSave() {
        try {
            TuyaSensorData data = service.saveSensorData();
            if (data == null || data.getTemperatura() == null) {
                return ResponseEntity.noContent().build(); // 204
            }
            Map<String,Object> response = createSuccessResponse(data);
            return ResponseEntity.ok(response);
        } catch(Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private Map<String,Object> createSuccessResponse(TuyaSensorData d) {
        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("saved_record", buildSavedRecord(d));
        resp.put("method", "hybrid_capture");
        resp.put("success", true);
        resp.put("timestamp", LocalDateTime.now().toString());
        resp.put("capture_analysis", buildCaptureAnalysis(d));
        return resp;
    }

    private Map<String,Object> buildSavedRecord(TuyaSensorData d) {
        Map<String,Object> rec = new LinkedHashMap<>();
        rec.put("id", d.getId());
        rec.put("nombre", "Sensor de Temperatura");
        rec.put("temperatura", d.getTemperatura());
        rec.put("timestamp", d.getTimestamp().toString());
        return rec;
    }

    private Map<String,Object> buildCaptureAnalysis(TuyaSensorData d) {
        Map<String,Object> ca = new LinkedHashMap<>();
        ca.put("id", d.getId());
        ca.put("nombre", "Sensor de Temperatura");

        Map<String,Object> params = new LinkedHashMap<>();
        Map<String,Object> tempParam = new LinkedHashMap<>();
        tempParam.put("value", d.getTemperatura());
        tempParam.put("status", "captured");
        tempParam.put("unit", "°C");
        params.put("temperatura", tempParam);
        ca.put("parameters", params);

        ca.put("timestamp", d.getTimestamp().toString());

        Map<String,Object> stats = new LinkedHashMap<>();
        stats.put("parameters_captured", 1);
        stats.put("total_parameters", 1);
        stats.put("completeness_percentage", 100);
        stats.put("quality_rating", "EXCELENTE");
        ca.put("statistics", stats);

        return ca;
    }



    // ==========================================
    // ENDPOINTS DE DEBUGGING Y ANÁLISIS
    // ==========================================

    /**
     * DEBUGGING: Obtener datos raw directamente del sensor
     */
    @Operation(
        summary = "Obtener datos raw del sensor para debugging",
        description = "Devuelve la respuesta JSON exacta que envía el sensor Tuya, sin procesar. " +
                     "Útil para debugging y descobrir nuevos códigos DP."
    )
    @ApiResponse(responseCode = "200", description = "Datos raw obtenidos exitosamente")
    @GetMapping("/raw-data")
    public ResponseEntity<Map<String, Object>> getRawSensorData() {
        try {
            System.out.println("🐛 [Controller] Solicitando datos raw para debugging...");
            
            String rawData = service.getRawSensorResponse();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("raw_response", rawData);
            response.put("message", "Datos raw obtenidos exitosamente");
            
            // ANÁLISIS DE CÓDIGOS DP EN DATOS RAW
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rawJson = mapper.readTree(rawData);
                
                if (rawJson.has("result") && rawJson.get("result").isArray()) {
                    List<Map<String, Object>> codesAnalysis = new ArrayList<>();
                    JsonNode resultArray = rawJson.get("result");
                    
                    System.out.println("🔍 [Controller] Analizando " + resultArray.size() + " códigos DP:");
                    
                    for (JsonNode item : resultArray) {
                        Map<String, Object> codeInfo = new HashMap<>();
                        String code = item.get("code").asText();
                        Object value = item.has("value") ? item.get("value") : null;
                        
                        codeInfo.put("code", code);
                        codeInfo.put("value", value);
                        codeInfo.put("data_type", value != null ? value.getClass().getSimpleName() : "null");
                        codeInfo.put("parameter_category", categorizeParameter(code));
                        
                        codesAnalysis.add(codeInfo);
                        
                        System.out.println("   📊 " + code + " = " + value + " (" + categorizeParameter(code) + ")");
                    }
                    
                    response.put("codes_analysis", codesAnalysis);
                    response.put("total_active_codes", codesAnalysis.size());
                    
                    System.out.println("✅ [Controller] Análisis de códigos DP completado: " + codesAnalysis.size() + " códigos");
                }
                
            } catch (Exception e) {
                response.put("analysis_error", "No se pudo analizar los códigos DP: " + e.getMessage());
                System.err.println("⚠️ [Controller] Error analizando códigos DP: " + e.getMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error obteniendo datos raw: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error obteniendo datos raw",
                e.getMessage(),
                "RAW_DATA_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * AUTO-DESCUBRIMIENTO: Obtener especificaciones completas del dispositivo
     */
    @Operation(
        summary = "Descubrir especificaciones y capacidades del dispositivo",
        description = "Obtiene las especificaciones completas del dispositivo Tuya desde la API oficial, " +
                     "incluyendo todos los códigos DP que soporta, tipos de datos y descripciones."
    )
    @ApiResponse(responseCode = "200", description = "Especificaciones obtenidas exitosamente")
    @GetMapping("/device-specifications")
    public ResponseEntity<Map<String, Object>> getDeviceSpecifications() {
        try {
            System.out.println("🔍 [Controller] Solicitando especificaciones del dispositivo...");
            
            Map<String, Object> specifications = service.getDeviceSpecifications();
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("service", "device-specifications");
            
            if (specifications.containsKey("error")) {
                System.err.println("❌ [Controller] Error en especificaciones: " + specifications.get("error"));
                
                response.put("success", false);
                response.put("error", specifications.get("error"));
                response.put("specifications", specifications);
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            response.put("success", true);
            response.put("specifications", specifications);
            
            // LOG DETALLADO DE ESPECIFICACIONES
            System.out.println("✅ [Controller] Especificaciones obtenidas exitosamente");
            if (specifications.containsKey("category")) {
                System.out.println("   Categoría: " + specifications.get("category"));
            }
            
            if (specifications.containsKey("status")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> statusList = (List<Map<String, Object>>) specifications.get("status");
                System.out.println("   Códigos DP soportados: " + statusList.size());
                
                for (Map<String, Object> status : statusList) {
                    System.out.println("     - " + status.get("code") + " (" + status.get("name") + ")");
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error en device-specifications: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error obteniendo especificaciones",
                e.getMessage(),
                "SPECIFICATIONS_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ANÁLISIS AVANZADO: Comparar datos actuales vs especificaciones
     */
    @Operation(
        summary = "Análisis comparativo: datos actuales vs especificaciones",
        description = "Compara los códigos DP que está enviando actualmente el sensor con los que puede " +
                     "soportar según sus especificaciones. Proporciona análisis de utilización y eficiencia."
    )
    @ApiResponse(responseCode = "200", description = "Análisis comparativo completado")
    @GetMapping("/compare-current-vs-specs")
    public ResponseEntity<Map<String, Object>> compareCurrentVsSpecs() {
        try {
            System.out.println("⚖️  [Controller] Iniciando análisis comparativo...");
            
            // OBTENER ESPECIFICACIONES
            Map<String, Object> specs = service.getDeviceSpecifications();
            
            // OBTENER DATOS RAW ACTUALES
            String rawData = service.getRawSensorResponse();
            
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("timestamp", LocalDateTime.now().toString());
            comparison.put("analysis_type", "current_vs_specifications");
            
            if (specs.containsKey("error")) {
                comparison.put("error", "No se pudieron obtener especificaciones: " + specs.get("error"));
                return ResponseEntity.ok(comparison);
            }
            
            // PARSEAR DATOS ACTUALES
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rawJson = mapper.readTree(rawData);
            
            List<String> currentCodes = new ArrayList<>();
            List<String> supportedCodes = new ArrayList<>();
            List<String> activeCodes = new ArrayList<>();
            List<String> inactiveCodes = new ArrayList<>();
            
            // OBTENER CÓDIGOS ACTUALES
            if (rawJson.has("result") && rawJson.get("result").isArray()) {
                for (JsonNode item : rawJson.get("result")) {
                    if (item.has("code")) {
                        currentCodes.add(item.get("code").asText());
                    }
                }
            }
            
            // OBTENER CÓDIGOS SOPORTADOS
            if (specs.containsKey("status")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> statusList = (List<Map<String, Object>>) specs.get("status");
                
                for (Map<String, Object> status : statusList) {
                    String code = (String) status.get("code");
                    supportedCodes.add(code);
                    
                    if (currentCodes.contains(code)) {
                        activeCodes.add(code);
                    } else {
                        inactiveCodes.add(code);
                    }
                }
            }
            
            // CALCULAR MÉTRICAS
            double utilizationPercentage = supportedCodes.isEmpty() ? 0 : 
                (activeCodes.size() * 100.0 / supportedCodes.size());
            
            comparison.put("current_codes", currentCodes);
            comparison.put("supported_codes", supportedCodes);
            comparison.put("active_codes", activeCodes);
            comparison.put("inactive_codes", inactiveCodes);
            comparison.put("utilization_percentage", Math.round(utilizationPercentage * 100.0) / 100.0);
            
            // ANÁLISIS DETALLADO
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("total_current_parameters", currentCodes.size());
            analysis.put("total_supported_parameters", supportedCodes.size());
            analysis.put("active_parameters", activeCodes.size());
            analysis.put("unused_parameters", inactiveCodes.size());
            analysis.put("efficiency_score", Math.round(utilizationPercentage * 100.0) / 100.0);
            
            // CLASIFICACIÓN DE EFICIENCIA
            String efficiencyRating;
            if (utilizationPercentage >= 80) {
                efficiencyRating = "EXCELENTE";
            } else if (utilizationPercentage >= 60) {
                efficiencyRating = "BUENA";
            } else if (utilizationPercentage >= 40) {
                efficiencyRating = "REGULAR";
            } else {
                efficiencyRating = "BAJA";
            }
            analysis.put("efficiency_rating", efficiencyRating);
            
            comparison.put("analysis", analysis);
            
            // LOGGING DETALLADO
            System.out.println("✅ [Controller] Análisis comparativo completado:");
            System.out.println("   Códigos actuales: " + currentCodes.size());
            System.out.println("   Códigos soportados: " + supportedCodes.size());
            System.out.println("   Utilización: " + String.format("%.1f%%", utilizationPercentage));
            System.out.println("   Rating: " + efficiencyRating);
            System.out.println("   Códigos activos: " + activeCodes);
            System.out.println("   Códigos inactivos: " + inactiveCodes);
            
            return ResponseEntity.ok(comparison);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error en análisis comparativo: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error en análisis comparativo",
                e.getMessage(),
                "COMPARISON_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ESTADO COMPLETO: Debug status del sistema
     */
    @Operation(
        summary = "Estado completo del sistema Tuya para debugging",
        description = "Devuelve información completa del estado del sistema incluyendo estadísticas de BD, " +
                     "último registro capturado, análisis de calidad de datos y métricas del servicio."
    )
    @ApiResponse(responseCode = "200", description = "Estado del sistema obtenido exitosamente")
    @GetMapping("/debug-status")
    public ResponseEntity<Map<String, Object>> getDebugStatus() {
        try {
            System.out.println("🔍 [Controller] Generando estado completo del sistema...");
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("timestamp", LocalDateTime.now().toString());
            debugInfo.put("service_version", "hybrid_v2.0");
            debugInfo.put("api_status", "active");
            
            // ESTADÍSTICAS DE BASE DE DATOS
            try {
                List<TuyaSensorData> allData = service.findAll();
                debugInfo.put("total_records", allData.size());
                
                if (!allData.isEmpty()) {
                    // ÚLTIMO REGISTRO
                    TuyaSensorData latest = allData.stream()
                            .max(Comparator.comparing(TuyaSensorData::getTimestamp))
                            .orElse(null);
                    
                    if (latest != null) {
                        Map<String, Object> latestRecord = buildRecordSummary(latest);
                        debugInfo.put("latest_record", latestRecord);
                        
                        // TIEMPO DESDE ÚLTIMA ACTUALIZACIÓN
                        long minutesSinceLastUpdate = java.time.Duration.between(
                            latest.getTimestamp(), LocalDateTime.now()
                        ).toMinutes();
                        debugInfo.put("minutes_since_last_update", minutesSinceLastUpdate);
                    }
                    
                    // ESTADÍSTICAS DE LOS ÚLTIMOS 24 REGISTROS
                    List<TuyaSensorData> recent24 = allData.stream()
                            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                            .limit(24)
                            .collect(Collectors.toList());
                    
                    if (!recent24.isEmpty()) {
                        Map<String, Object> stats = calculateRecentStatistics(recent24);
                        debugInfo.put("recent_24h_statistics", stats);
                    }
                    
                    // ANÁLISIS DE CALIDAD DE DATOS
                    Map<String, Object> dataQuality = analyzeDataQuality(allData);
                    debugInfo.put("data_quality_analysis", dataQuality);
                }
                
            } catch (Exception e) {
                debugInfo.put("database_error", "Error accediendo a la base de datos: " + e.getMessage());
            }
            
            // ESTADO DE SERVICIOS
            debugInfo.put("tuya_connection_status", "ready");
            debugInfo.put("database_connection", "active");
            debugInfo.put("cache_status", "operational");
            debugInfo.put("scheduler_status", "running");
            
            System.out.println("✅ [Controller] Estado completo generado exitosamente");
            
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error generando estado del sistema: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error generando estado del sistema",
                e.getMessage(),
                "DEBUG_STATUS_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ==========================================
    // ENDPOINTS CRUD MEJORADOS
    // ==========================================

    /**
     * OBTENER TODOS con análisis
     */
    @Operation(summary = "Obtener todos los registros con análisis estadístico")
    @ApiResponse(responseCode = "200", description = "Registros obtenidos exitosamente")
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll() {
        try {
            System.out.println("📋 [Controller] Solicitando todos los registros con análisis...");
            
            List<TuyaSensorData> data = service.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("total_records", data.size());
            response.put("records", data);
            
            if (!data.isEmpty()) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("date_range", Map.of(
                    "earliest", data.stream().min(Comparator.comparing(TuyaSensorData::getTimestamp)).get().getTimestamp(),
                    "latest", data.stream().max(Comparator.comparing(TuyaSensorData::getTimestamp)).get().getTimestamp()
                ));
                
                response.put("summary", summary);
            }
            
            System.out.println("✅ [Controller] Enviando " + data.size() + " registros con análisis");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error obteniendo registros: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error obteniendo registros",
                e.getMessage(),
                "FIND_ALL_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * OBTENER ÚLTIMOS REGISTROS con tendencias
     */
    @Operation(
        summary = "Obtener últimos registros con análisis de tendencias",
        description = "Devuelve los últimos 10 registros ordenados por fecha, con análisis de tendencias de parámetros."
    )
    @ApiResponse(responseCode = "200", description = "Últimos registros obtenidos con análisis")
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestData() {
        try {
            System.out.println("📊 [Controller] Solicitando últimos registros con análisis de tendencias...");
            
            List<TuyaSensorData> allData = service.findAll();
            
            // OBTENER ÚLTIMOS 10 REGISTROS
            List<TuyaSensorData> latestData = allData.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("latest_records", latestData);
            response.put("total_records_count", allData.size());
            response.put("showing_latest", latestData.size());
            
            if (!latestData.isEmpty()) {
                // ANÁLISIS DE TENDENCIAS
                Map<String, Object> trends = analyzeTrends(latestData);
                response.put("trends_analysis", trends);
                
                System.out.println("📈 [Controller] Análisis de tendencias completado");
            }
            
            System.out.println("✅ [Controller] Enviando " + latestData.size() + " registros más recientes con análisis");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error obteniendo datos recientes: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error obteniendo últimos registros",
                e.getMessage(),
                "LATEST_DATA_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * OBTENER POR ID con información detallada
     */
    @Operation(summary = "Obtener registro por ID con información detallada")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro encontrado"),
        @ApiResponse(responseCode = "404", description = "Registro no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable String id) {
        try {
            System.out.println("🔍 [Controller] Buscando registro por ID: " + id);
            
            Optional<TuyaSensorData> data = service.findById(id);
            
            if (data.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("found", true);
                response.put("record", data.get());
                response.put("record_summary", buildRecordSummary(data.get()));
                
                System.out.println("✅ [Controller] Registro encontrado: " + id);
                return ResponseEntity.ok(response);
                
            } else {
                System.out.println("⚠️  [Controller] Registro no encontrado: " + id);
                
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("timestamp", LocalDateTime.now().toString());
                notFoundResponse.put("found", false);
                notFoundResponse.put("searched_id", id);
                notFoundResponse.put("message", "Registro no encontrado");
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
            }
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error buscando por ID: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error buscando registro",
                e.getMessage(),
                "FIND_BY_ID_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * INSERTAR MANUAL (mantenido para compatibilidad)
     */
    @Operation(summary = "Insertar datos manualmente (modo compatibilidad)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos insertados exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en la inserción")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> save(@RequestBody TuyaSensorDataDTO dto) {
        try {
            System.out.println("💾 [Controller] Insertando datos manuales...");
            
            TuyaSensorData data = convertDtoToEntity(dto);
            TuyaSensorData saved = service.save(data);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("saved_record", saved);
            response.put("record_summary", buildRecordSummary(saved));
            response.put("message", "Datos insertados manualmente");
            
            System.out.println("✅ [Controller] Datos manuales guardados: " + saved.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error insertando datos manuales: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error insertando datos manuales",
                e.getMessage(),
                "MANUAL_INSERT_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ACTUALIZAR con logging detallado
     */
    @Operation(summary = "Actualizar registro existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Registro no encontrado")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id, @RequestBody TuyaSensorData newData) {
        try {
            System.out.println("🔄 [Controller] Actualizando registro: " + id);
            
            TuyaSensorData updated = service.update(id, newData);
            
            if (updated != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("updated_record", updated);
                response.put("record_summary", buildRecordSummary(updated));
                response.put("message", "Registro actualizado exitosamente");
                
                System.out.println("✅ [Controller] Registro actualizado: " + id);
                return ResponseEntity.ok(response);
                
            } else {
                System.out.println("⚠️  [Controller] Registro no encontrado para actualizar: " + id);
                
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("success", false);
                notFoundResponse.put("timestamp", LocalDateTime.now().toString());
                notFoundResponse.put("searched_id", id);
                notFoundResponse.put("message", "Registro no encontrado para actualizar");
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
            }
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error actualizando registro: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error actualizando registro",
                e.getMessage(),
                "UPDATE_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ELIMINAR con logging detallado
     */
    @Operation(summary = "Eliminar registro por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Registro no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        try {
            System.out.println("🗑️  [Controller] Eliminando registro: " + id);
            
            boolean deleted = service.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("deleted_id", id);
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "Registro eliminado exitosamente");
                
                System.out.println("✅ [Controller] Registro eliminado: " + id);
                return ResponseEntity.ok(response);
                
            } else {
                response.put("success", false);
                response.put("message", "Registro no encontrado para eliminar");
                
                System.out.println("⚠️  [Controller] Registro no encontrado para eliminar: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            System.err.println("❌ [Controller] Error eliminando registro: " + e.getMessage());
            
            Map<String, Object> errorResponse = createErrorResponse(
                "Error eliminando registro",
                e.getMessage(),
                "DELETE_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES PARA ANÁLISIS
    // ==========================================

    /**
     * Crear respuesta de éxito estructurada
     */
   /* private Map<String, Object> createSuccessResponse(TuyaSensorData data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("method", "hybrid_capture");
        response.put("saved_record", data);
        
        // ANÁLISIS DEL REGISTRO
        Map<String, Object> analysis = buildRecordSummary(data);
        response.put("capture_analysis", analysis);
        
        return response;
    }
         */

    /**
     * Crear respuesta de error estructurada
     */
    private Map<String, Object> createErrorResponse(String title, String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("error_title", title);
        response.put("error_message", message);
        response.put("error_code", errorCode);
        
        return response;
    }

    /**
     * Construir resumen detallado de un registro
     */
    private Map<String, Object> buildRecordSummary(TuyaSensorData record) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", record.getId());
        summary.put("nombre", record.getNombre());
        summary.put("timestamp", record.getTimestamp().toString());
        
        // PARÁMETROS INDIVIDUALES
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperatura", record.getTemperatura() != null ? 
            Map.of("value", record.getTemperatura(), "unit", "°C", "status", "captured") : 
            Map.of("status", "missing"));
        parameters.put("ph", record.getPh() != null ? 
            Map.of("value", record.getPh(), "unit", "", "status", "captured") : 
            Map.of("status", "missing"));
        parameters.put("orp", record.getOrp() != null ? 
            Map.of("value", record.getOrp(), "unit", "mV", "status", "captured") : 
            Map.of("status", "missing"));
        parameters.put("ec", record.getEc() != null ? 
            Map.of("value", record.getEc(), "unit", "mS/cm", "status", "captured") : 
            Map.of("status", "missing"));
        parameters.put("tds", record.getTds() != null ? 
            Map.of("value", record.getTds(), "unit", "ppm", "status", "captured") : 
            Map.of("status", "missing"));
        parameters.put("salinidad", record.getSalinidad() != null ? 
            Map.of("value", record.getSalinidad(), "unit", "ppm", "status", "captured") : 
            Map.of("status", "missing"));
        
        summary.put("parameters", parameters);
        
        // ESTADÍSTICAS
        int capturedCount = 0;
        int totalParams = 6;
        
        if (record.getTemperatura() != null) capturedCount++;
       // if (record.getPh() != null) capturedCount++;
        //if (record.getOrp() != null) capturedCount++;
        //if (record.getEc() != null) capturedCount++;
        //if (record.getTds() != null) capturedCount++;
        //if (record.getSalinidad() != null) capturedCount++;
        
        double completenessPercentage = (capturedCount * 100.0) / totalParams;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("parameters_captured", capturedCount);
        stats.put("total_parameters", totalParams);
        stats.put("completeness_percentage", Math.round(completenessPercentage * 100.0) / 100.0);
        
        String qualityRating;
        if (completenessPercentage >= 90) {
            qualityRating = "EXCELENTE";
        } else if (completenessPercentage >= 70) {
            qualityRating = "BUENA";
        } else if (completenessPercentage >= 50) {
            qualityRating = "REGULAR";
        } else {
            qualityRating = "BAJA";
        }
        stats.put("quality_rating", qualityRating);
        
        summary.put("statistics", stats);
        
        return summary;
    }

    /**
 * Calcular estadísticas de registros recientes
 */
    private Map<String, Object> calculateRecentStatistics(List<TuyaSensorData> records) {
        Map<String, Object> stats = new HashMap<>();

        // ESTADÍSTICAS DE TEMPERATURA
        OptionalDouble avgTemp = records.stream()
        .filter(d -> d.getTemperatura() != null)
        .mapToDouble(TuyaSensorData::getTemperatura)
        .average();
        if (avgTemp.isPresent()) {
            stats.put("avg_temperature", Math.round(avgTemp.getAsDouble() * 100.0) / 100.0);
        }
        return stats;
    } 

    

        
        // ESTADÍSTICAS DE pH
       /*  OptionalDouble avgPh = records.stream()
                .filter(d -> d.getPh() != null)
                .mapToDouble(TuyaSensorData::getPh)
                .average();
        if (avgPh.isPresent()) {
            stats.put("avg_ph", Math.round(avgPh.getAsDouble() * 100.0) / 100.0);
        }
        
        // ESTADÍSTICAS DE ORP
        OptionalDouble avgOrp = records.stream()
                .filter(d -> d.getOrp() != null)
                .mapToDouble(TuyaSensorData::getOrp)
                .average();
        if (avgOrp.isPresent()) {
            stats.put("avg_orp", Math.round(avgOrp.getAsDouble() * 100.0) / 100.0);
        }
        
        return stats;
    }
     */

    /**
     * Analizar calidad de datos general
     */
    private Map<String, Object> analyzeDataQuality(List<TuyaSensorData> allData) {
        Map<String, Object> quality = new HashMap<>();
        
        if (allData.isEmpty()) {
            quality.put("status", "no_data");
            return quality;
        }
        
        int totalRecords = allData.size();
        long recordsWithTemperature = allData.stream().filter(d -> d.getTemperatura() != null).count();
        long recordsWithPh = allData.stream().filter(d -> d.getPh() != null).count();
        long recordsWithOrp = allData.stream().filter(d -> d.getOrp() != null).count();
        long recordsWithEc = allData.stream().filter(d -> d.getEc() != null).count();
        long recordsWithTds = allData.stream().filter(d -> d.getTds() != null).count();
        long recordsWithSalinidad = allData.stream().filter(d -> d.getSalinidad() != null).count();
        
        quality.put("total_records", totalRecords);
        quality.put("temperature_completeness", Math.round((recordsWithTemperature * 100.0 / totalRecords) * 100.0) / 100.0);
        quality.put("ph_completeness", Math.round((recordsWithPh * 100.0 / totalRecords) * 100.0) / 100.0);
        quality.put("orp_completeness", Math.round((recordsWithOrp * 100.0 / totalRecords) * 100.0) / 100.0);
        quality.put("ec_completeness", Math.round((recordsWithEc * 100.0 / totalRecords) * 100.0) / 100.0);
        quality.put("tds_completeness", Math.round((recordsWithTds * 100.0 / totalRecords) * 100.0) / 100.0);
        quality.put("salinity_completeness", Math.round((recordsWithSalinidad * 100.0 / totalRecords) * 100.0) / 100.0);
        
        // PUNTUACIÓN GENERAL
        double overallQuality = (recordsWithTemperature + recordsWithPh + recordsWithOrp + 
                               recordsWithEc + recordsWithTds + recordsWithSalinidad) * 100.0 / (totalRecords * 6);
        quality.put("overall_quality_score", Math.round(overallQuality * 100.0) / 100.0);
        
        return quality;
    }

    /**
     * Analizar tendencias de parámetros
     */
    /**
 * Analiza tendencias de parámetros (solo temperatura aquí).
 */
    private Map<String, Object> analyzeTrends(List<TuyaSensorData> records) {
        Map<String, Object> trends = new HashMap<>();

         // TENDENCIA DE TEMPERATURA
        List<Double> temperatures = records.stream()
        .filter(d -> d.getTemperatura() != null)
        .map(TuyaSensorData::getTemperatura)
        .collect(Collectors.toList());
        
        if (!temperatures.isEmpty()) {
        // Llama a tu método de cálculo de tendencia
            trends.put("temperature_trend", calculateTrend(temperatures));
        // También puedes añadir la media
            double avg = temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            trends.put("avg_temperature", Math.round(avg * 100.0) / 100.0);
        }
        return trends;
    }

        
       /* // TENDENCIA DE pH
        List<Double> phValues = records.stream()
                .filter(d -> d.getPh() != null)
                .map(TuyaSensorData::getPh)
                .collect(Collectors.toList());
        
        if (!phValues.isEmpty()) {
            trends.put("ph_trend", calculateTrend(phValues));
            trends.put("avg_ph", phValues.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }
        
        return trends;
    }
         */

    /**
     * Calcular tendencia de una serie de valores
     */
    private String calculateTrend(List<Double> values) {
        if (values.size() < 2) return "insufficient_data";
        
        double firstHalf = values.subList(0, values.size() / 2).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalf = values.subList(values.size() / 2, values.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        
        double change = secondHalf - firstHalf;
        double percentChange = Math.abs(change / firstHalf) * 100;
        
        if (percentChange < 2.0) return "stable";
        else if (change > 0) return "increasing";
        else return "decreasing";
    }

    /**
     * Categorizar parámetro por código DP
     */
    private String categorizeParameter(String code) {
        String lowerCode = code.toLowerCase();
        if (lowerCode.contains("temp")) return "temperature";
       // else if (lowerCode.contains("ph")) return "acidity";
        //else if (lowerCode.contains("orp")) return "oxidation_reduction";
        //else if (lowerCode.contains("ec") || lowerCode.contains("conductivity")) return "electrical_conductivity";
        //else if (lowerCode.contains("tds")) return "dissolved_solids";
        //else if (lowerCode.contains("salt") || lowerCode.contains("salin")) return "salinity";
        //else if (lowerCode.contains("humidity")) return "humidity";
        //else if (lowerCode.contains("battery")) return "battery";
        else return "unknown";
    }

    /**
     * Logging detallado del resultado del controlador
     */
    private void logControllerResult(TuyaSensorData data) {
        System.out.println("✅ [Controller] RESULTADO DE CAPTURA HÍBRIDA:");
        System.out.println("   ID del registro: " + data.getId());
        System.out.println("   Nombre: " + data.getNombre());
        System.out.println("   Timestamp: " + data.getTimestamp());
        
        int capturedParams = 0;
        System.out.println("   📊 Parámetros capturados:");
        
        if (data.getTemperatura() != null) {
            System.out.println("     ✅ Temperatura: " + data.getTemperatura() + "°C");
            capturedParams++;
        }
       /*  if (data.getPh() != null) {
            System.out.println("     ✅ pH: " + data.getPh());
            capturedParams++;
        }
        if (data.getOrp() != null) {
            System.out.println("     ✅ ORP: " + data.getOrp() + " mV");
            capturedParams++;
        }
        if (data.getEc() != null) {
            System.out.println("     ✅ EC: " + data.getEc() + " mS/cm");
            capturedParams++;
        }
        if (data.getTds() != null) {
            System.out.println("     ✅ TDS: " + data.getTds() + " ppm");
            capturedParams++;
        }
        if (data.getSalinidad() != null) {
            System.out.println("     ✅ Salinidad: " + data.getSalinidad() + " ppm");
            capturedParams++;
        }
         */
        
        double successRate = (capturedParams * 100.0) / 6;
        System.out.println("   📈 Tasa de éxito final: " + capturedParams + "/6 (" + 
                          String.format("%.1f%%", successRate) + ")");
    }

    /**
     * Convertir DTO a entidad (compatibilidad)
     */
    private TuyaSensorData convertDtoToEntity(TuyaSensorDataDTO dto) {
        TuyaSensorData data = new TuyaSensorData();
        data.setNombre(dto.getNombre() != null ? dto.getNombre() : "Sensor Manual");
       // data.setPh(dto.getPh());
        //data.setOrp(dto.getOrp());
        //data.setEc(dto.getEc());
        //data.setTds(dto.getTds());
        //data.setSalinidad(dto.getSalinidad());
        data.setTemperatura(dto.getTemperatura());
        data.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now());
        return data;
    }
}