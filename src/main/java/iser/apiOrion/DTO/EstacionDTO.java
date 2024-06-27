package iser.apiOrion.DTO;

import lombok.Data;

@Data
public class EstacionDTO {
    /*
        * id: Identificador de la estaci�n
        * imagen: Imagen de la estaci�n
        * ciudad: Ciudad de la estaci�n
        * departamento: Departamento de la estaci�n
        * nombre: Nombre de la estaci�n
        * encargado: Encargado de la estaci�n
        * detalles: Detalles de la estaci�n
        * estado: Estado de la estaci�n
        * idTipoCultivo: Identificador del tipo de cultivo
        * descripcionTipoCultivo: Descripci�n del tipo de cultivo
        * Numero_Asociados: N�mero de asociados
     */

    private String id;
    private String imagen;
    private String ciudad;
    private String departamento;
    private String nombre;
    private String encargado;
    private String detalles;
    private String estado;
    private String idTipoCultivo;
    private String descripcionTipoCultivo;
    private int Numero_Asociados;

}
