package iser.apiOrion.controller;

import iser.apiOrion.DTO.DatosDTO;
import iser.apiOrion.DTO.DatosGraficaDTO;
import iser.apiOrion.service.DatosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/v1/datos")   // ← Con la barra al inicio
public class DatosController {

    @Autowired
    private DatosService datosService;

    @Operation(
      summary     = "Rangos de fechas por sensor",
      description = "Obtiene los datos de un sensor en un rango de fechas determinado"
    )
    @ApiResponses({
      @ApiResponse(
        responseCode = "200",
        description  = "Operación exitosa",
        content = @Content(
          mediaType = "application/json",
          array     = @ArraySchema(schema = @Schema(implementation = DatosGraficaDTO.class))
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description  = "Petición inválida",
        content      = @Content(
          mediaType = "application/json",
          examples    = @io.swagger.v3.oas.annotations.media.ExampleObject(
            value = "{\"message\":\"petición fallida\"}"
          )
        )
      )
    })
    @GetMapping(
      value    = "/rangoFechasporSensor",
      produces = "application/json"
    )
    public ResponseEntity<?> rangoFechasPorSensor(
      @RequestParam("fechaInicial") String fechaInicial,
      @RequestParam("fechaFinal")   String fechaFinal,
      @RequestParam("idSensor")     String idSensor
    ) throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date inicio = fmt.parse(fechaInicial);
        Date fin    = fmt.parse(fechaFinal);
        return datosService.rangofecha(inicio, fin, idSensor);
    }

    @Operation(
      summary     = "Insertar datos de un sensor",
      description = "Inserta los datos de un sensor en la base de datos. " +
                    "Este endpoint debe consumirlo el Arduino."
    )
    @ApiResponses({
      @ApiResponse(
        responseCode = "200",
        description  = "Dato insertado con éxito",
        content      = @Content(
          mediaType = "application/json",
          schema    = @Schema(implementation = DatosDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description  = "JSON inválido o dato mal formado",
        content      = @Content(
          mediaType = "application/json",
          examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
            value = "{\"message\":\"petición fallida\"}"
          )
        )
      )
    })
    @PostMapping(
      value    = "/insertar",
      consumes = "application/json",
      produces = "application/json"
    )
    public ResponseEntity<?> insertar(@RequestBody DatosDTO datosDTO) {
        // Log para debug
        Date ahora = new Date();
        System.out.printf(
          "POST /insertar → idSensor=%s, valor=%s, fecha=%s%n",
          datosDTO.getIdSensor(),
          datosDTO.getValor(),
          ahora
        );
        return datosService.insertar(
          datosDTO.getIdSensor(),
          datosDTO.getValor(),
          ahora
        );
    }
}