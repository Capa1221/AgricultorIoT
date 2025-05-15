package iser.apiOrion.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import iser.apiOrion.collection.Estacion;
import iser.apiOrion.service.EstacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estacion")
@CrossOrigin(origins = "*")
public class EstacionController {

    /**
     * Servicio de hibernadero
     */
    @Autowired
    EstacionService hibernaderoService;

    @Operation(summary = "Buscar todos los datos de los hibernaderos",
            description = "Obtiene todos los datos de los hibernaderos registrados en la base de datos, Consulta detalles de los hibernaderos.")
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "operacion exitosa",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Estacion.class)))}),
            @ApiResponse(responseCode = "400", description = "peticion fallida", content = { @io.swagger.v3.oas.annotations.media.Content (mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"peticion fallida\"}"))}),
    })
    @GetMapping("/buscarTodos")
    public ResponseEntity<?> buscarTodos() {
        try {
                return hibernaderoService.buscarTodos();
                
        } catch (Exception e) {
                System.out.println("Error", e);
        }
    }

    @Operation(summary = "Busca una estacion por su id",
            description = "Obtiene los datos de una estacion en especifico por su id, Consulta detalles de una estacion en especifico.")
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "operacion exitosa",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Estacion.class))}),
            @ApiResponse(responseCode = "400", description = "peticion fallida", content = { @io.swagger.v3.oas.annotations.media.Content (mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"peticion fallida\"}"))}),
    })
    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(@RequestParam String id, HttpServletRequest request) {
        return hibernaderoService.buscar(id);
    }

    @Operation(summary = "Insertar una estacion",
            description = "Inserta una estacion en la base de datos, Insertando los detalles de una estacion en la base de datos. \n Nota: el campo de imagen debe ir un base 64.")
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "operacion exitosa",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Estacion.class))}),
            @ApiResponse(responseCode = "400", description = "peticion fallida", content = { @io.swagger.v3.oas.annotations.media.Content (mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"peticion fallida\"}"))}),
    })
    @PostMapping("/insertar")
    public ResponseEntity<?> insertar(@RequestBody Estacion object) {
        return hibernaderoService.insertar(object);
    }

    @Operation(summary = "Actualizar una estacion",
            description = "Actualiza los datos de una estacion en la base de datos, Actualizando los detalles de una estacion en la base de datos.")
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "operacion exitosa",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Estacion.class))}),
            @ApiResponse(responseCode = "400", description = "peticion fallida", content = { @io.swagger.v3.oas.annotations.media.Content (mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"peticion fallida\"}"))}),
    })
    @PutMapping("/actualizar")
    public ResponseEntity<?> actualizar(@RequestBody Estacion object) {
        return hibernaderoService.actualizar(object);
    }

    @Operation(summary = "Eliminar una estacion",
            description = "Elimina una estacion en la base de datos, Eliminando los detalles de una estacion en la base de datos.")
    @ApiResponses(value = {

                @ApiResponse(responseCode = "200", description = "operacion exitosa",
                        content = { @Content(mediaType = "application/json",
                                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"operacion exitosa\"}"))}),
                @ApiResponse(responseCode = "400", description = "peticion fallida", content = { @io.swagger.v3.oas.annotations.media.Content (mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"peticion fallida\"}"))}),
    })
    @DeleteMapping("/eliminar")
    public ResponseEntity<?> eliminar(String id) {
        return hibernaderoService.eliminar(id);
    }

    @Operation(summary = "Buscar las estaciones asociadas a un usuario",
            description = "Obtiene los datos de las estaciones asociadas a un usuario en especifico por su id, Consulta detalles de las estaciones asociadas a un usuario en especifico.")
    @ApiResponses(value = {

                @ApiResponse(responseCode = "200", description = "operacion exitosa",
                        content = { @Content(mediaType = "application/json",
                                schema = @Schema(implementation = Estacion.class))}),
                @ApiResponse(responseCode = "400", description = "peticion fallida", content = { @io.swagger.v3.oas.annotations.media.Content (mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"message\":\"peticion fallida\"}"))}),
    })
    @GetMapping("/buscarEstacionesPropietario")
    public ResponseEntity<?> buscarEstacionesPropietario(String idUsuario) {
        return hibernaderoService.buscarEstacionesPropietario(idUsuario);
    }


}
