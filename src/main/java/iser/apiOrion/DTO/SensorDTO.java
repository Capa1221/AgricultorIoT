package iser.apiOrion.DTO;


import lombok.Data;

@Data
public class SensorDTO {

    /*
     * id: Identificador del sensor
     * idEstacion: Identificador de la estaci�n
     * nombre: Nombre del sensor
     * descripcion: Descripci�n del sensor
     * config: Configuraci�n del sensor
     * ubicacion: Ubicaci�n del sensor
     */

    private String id;
    private String idEstacion;
    private String nombre;
    private String descripcion;
    private boolean config;
    private String ubicacion;

}
