package iser.apiOrion.controller;

import iser.apiOrion.DTO.TuyaSensorDataDTO;
import iser.apiOrion.collection.TuyaSensorData;
import iser.apiOrion.service.TuyaSensorDataService;
import io.swagger.v3.oas.annotations.Operation; // Importación de Swagger
import io.swagger.v3.oas.annotations.responses.ApiResponse; // Importación de Swagger
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Importación de Swagger
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping(value = "/api/v1/tuya", produces = MediaType.APPLICATION_JSON_VALUE)
public class TuyaSensorDataController {

    @Autowired
    private TuyaSensorDataService service;

    // Endpoint para obtener y guardar datos del sensor Tuya
    @Operation(summary = "Obtener y guardar datos del sensor Tuya desde la API de Tuya")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos y guardados exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error al obtener o guardar datos del sensor Tuya")
    })
    @PostMapping("/fetch-and-save")
    public ResponseEntity<TuyaSensorData> fetchAndSave() {
        TuyaSensorData savedData = service.saveSensorData();
        if (savedData != null) {
            return ResponseEntity.ok(savedData);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    // Endpoint para insertar datos manualmente (si aún lo necesitas)
    // Nota: Si este endpoint ya no se usar para datos manuales, puedes eliminarlo.
    // Si lo mantienes, asegrate de que el DTO sea apropiado para datos manuales.
    
    @Operation(summary = "Insertar datos del sensor Tuya manualmente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacin exitosa"),
            @ApiResponse(responseCode = "400", description = "Peticin fallida")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TuyaSensorData> save(@RequestBody TuyaSensorDataDTO dto) {
        TuyaSensorData data = convertDtoToEntity(dto);
        return ResponseEntity.ok(service.save(data));
    }
    private TuyaSensorData convertDtoToEntity(TuyaSensorDataDTO dto) {
        TuyaSensorData data = new TuyaSensorData();
        data.setNombre(dto.getNombre());
        data.setPh(dto.getPh());
        data.setOrp(dto.getOrp());
        data.setEc(dto.getEc());
        data.setTds(dto.getTds());
        data.setSalinidad(dto.getSalinidad());
        data.setTemperatura(dto.getTemperatura());
        data.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now());
        return data;
    }
    

    @Operation(summary = "Obtener todos los datos del sensor Tuya")
    @ApiResponse(responseCode = "200", description = "Operacin exitosa")
    @GetMapping
    public ResponseEntity<List<TuyaSensorData>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Obtener sensor Tuya por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacin exitosa"),
            @ApiResponse(responseCode = "404", description = "Sensor no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TuyaSensorData> findById(@PathVariable String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Actualizar datos del sensor Tuya")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacin exitosa"),
            @ApiResponse(responseCode = "404", description = "Sensor no encontrado")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TuyaSensorData> update(@PathVariable String id, @RequestBody TuyaSensorData newData) {
        // Ya no necesitas un DTO aqu si ests actualizando directamente la entidad
        // TuyaSensorData data = convertDtoToEntity(dto); // Esta lnea ya no es necesaria
        TuyaSensorData updated = service.update(id, newData); // Usar newData directamente
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Eliminar datos del sensor Tuya por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminacin exitosa"),
            @ApiResponse(responseCode = "404", description = "Sensor no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = service.deleteById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}













