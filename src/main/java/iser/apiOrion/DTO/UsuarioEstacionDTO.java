package iser.apiOrion.DTO;

import lombok.Data;

@Data
public class UsuarioEstacionDTO {

    /*
     * id: Identificador de la estaci�n
     * idEstacion: Identificador de la estaci�n
     * idUsuario: Identificador del usuario
     * imagen: Imagen de la estaci�n
     * ciudad: Ciudad de la estaci�n
     * departamento: Departamento de la estaci�n
     * nombre: Nombre de la estaci�n
     * encargado: Encargado de la estaci�n
     * detalles: Detalles de la estaci�n
     * estado: Estado de la estaci�n
     * idTipoCultivo: Identificador del tipo de cultivo
     * nombreTipoCultivo: Nombre del tipo de cultivo
     * Numero_Asociados: N�mero de asociados
     */

    private String id;
    private String idEstacion;
    private String idUsuario;
    private String imagen;
    private String ciudad;
    private String departamento;
    private String nombre;
    private String encargado;
    private String detalles;
    private String estado;
    private String idTipoCultivo;
    private String nombreTipoCultivo;
    private Integer Numero_Asociados;

}
