package iser.apiOrion.serviceImplement;


import iser.apiOrion.DTO.EstacionDTO;
import iser.apiOrion.collection.Estacion;
import iser.apiOrion.repository.EstacionRepository;
import iser.apiOrion.repository.TipoCultivoRepository;
import iser.apiOrion.repository.UsuarioEstacionRepository;
import iser.apiOrion.repository.UsuarioRepository;
import iser.apiOrion.service.EstacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static iser.apiOrion.constant.messageConstant.buildMessage;

@Service
public class EstacionServiceImpl implements EstacionService {

    /**
     * Repositorio de estaciones
     */
    @Autowired
    EstacionRepository estacionRepository;

    /**
     * Repositorio de tipos de cultivo
     */
    @Autowired
    TipoCultivoRepository tipoCultivoRepository;

    /**
     * Repositorio de usuarios estacion
     */
    @Autowired
    UsuarioEstacionRepository usuarioEstacionRepository;

    /**
     * Repositorio de usuarios
     */
    @Autowired
    UsuarioRepository usuarioRepository;

    /**
     * Metodo que permite obtener todos los estaciones
     * @return lista de estaciones
     */
    @Override
    public ResponseEntity<?> buscarTodos() {
        List<EstacionDTO> estaciones = null;
        String descripcionTipoCultivo = "";
        try {
            List<Estacion> estacionesList = estacionRepository.findAll();
            if (estacionesList.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
             estaciones = new ArrayList<>();
            for (Estacion estacion : estacionesList) {
                EstacionDTO estacionDTO = new EstacionDTO();
                estacionDTO.setId(estacion.getId());
                estacionDTO.setNombre(estacion.getNombre());
                estacionDTO.setDetalles(estacion.getDetalles());
                estacionDTO.setImagen(estacion.getImagen());
                estacionDTO.setCiudad(estacion.getCiudad());
                estacionDTO.setDepartamento(estacion.getDepartamento());
                estacionDTO.setEncargado(estacion.getEncargado());
                estacionDTO.setEstado(estacion.getEstado());
                estacionDTO.setIdTipoCultivo(estacion.getIdTipoCultivo());
                descripcionTipoCultivo = tipoCultivoRepository.findById(estacion.getIdTipoCultivo()).get().getNombre();
                estacionDTO.setDescripcionTipoCultivo((!Objects.equals(descripcionTipoCultivo, "") && descripcionTipoCultivo != null) ? descripcionTipoCultivo : "No se encontro el tipo de cultivo");
                estacionDTO.setNumero_Asociados(usuarioEstacionRepository.countByIdEstacion(estacion.getId()));
                String usuarioEncargado = usuarioRepository.findById(estacion.getEncargado()).get().getUsuario();
                estacionDTO.setUsuarioEncargado(usuarioEncargado);
                estaciones.add(estacionDTO);
            }

            return ResponseEntity.ok().body(estaciones);
        } catch (Exception e) {
            System.out.println("Error: "+e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Metodo que permite obtener un estacion por su id
     * @param id id del estacion
     * @return estacion
     */
    @Override
    public ResponseEntity<?> buscar(String id) {
        try {
            Optional<Estacion> estacion = estacionRepository.findById(id);
            if (estacion.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            EstacionDTO estacionDTO = new EstacionDTO();
            estacionDTO.setId(estacion.get().getId());
            estacionDTO.setNombre(estacion.get().getNombre());
            estacionDTO.setDetalles(estacion.get().getDetalles());
            estacionDTO.setImagen(estacion.get().getImagen());
            estacionDTO.setCiudad(estacion.get().getCiudad());
            estacionDTO.setDepartamento(estacion.get().getDepartamento());
            estacionDTO.setEncargado(estacion.get().getEncargado());
            estacionDTO.setEstado(estacion.get().getEstado());
            estacionDTO.setIdTipoCultivo(estacion.get().getIdTipoCultivo());
            estacionDTO.setDescripcionTipoCultivo(tipoCultivoRepository.findById(estacion.get().getIdTipoCultivo()).get().getNombre());
            estacionDTO.setNumero_Asociados(usuarioEstacionRepository.countByIdEstacion(estacion.get().getId()));
            String usuarioEncargado = usuarioRepository.findById(estacion.get().getEncargado()).get().getUsuario();
            estacionDTO.setUsuarioEncargado(usuarioEncargado);
            return ResponseEntity.ok().body(estacionDTO);
        } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Metodo que permite insertar un estacion
     * @param object estacion
     * @return estacion insertado
     */
    @Override
    public ResponseEntity<?> insertar(Object object) {
        try {
            Estacion estacion = (Estacion) object;
            estacionRepository.save(estacion);
            return ResponseEntity.ok().body(estacion);
        } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Metodo que permite actualizar un estacion
     * @param object estacion
     * @return estacion actualizado
     */
    @Override
    public ResponseEntity<?> actualizar(Object object) {
        try {
            Estacion estacion = (Estacion) object;
            estacionRepository.save(estacion);
            return ResponseEntity.ok().body(estacion);
        } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Metodo que permite eliminar un estacion
     * @param id id del estacion
     * @return mensaje de confirmacion
     */
    @Override
    public ResponseEntity<?> eliminar(String id) {
        try {
            Optional<Estacion> estacion = estacionRepository.findById(id);
            if (estacion.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            estacionRepository.deleteById(estacion.get().getId());
            return ResponseEntity.ok().body(buildMessage("Estacion eliminada"));
        } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Metodo que permite obtener las estaciones de un propietario
     * @param idUsuario id del usuario
     * @return lista de estaciones
     */
    @Override
    public ResponseEntity<?> buscarEstacionesPropietario(String idUsuario) {
        try {
            List<Estacion> estaciones = estacionRepository.findAllByEncargado(idUsuario);
            if (estaciones.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            List<EstacionDTO> estacionesDTO = new ArrayList<>();
            for (Estacion estacion : estaciones) {
                EstacionDTO estacionDTO = new EstacionDTO();
                estacionDTO.setId(estacion.getId());
                estacionDTO.setNombre(estacion.getNombre());
                estacionDTO.setDetalles(estacion.getDetalles());
                estacionDTO.setImagen(estacion.getImagen());
                estacionDTO.setCiudad(estacion.getCiudad());
                estacionDTO.setDepartamento(estacion.getDepartamento());
                estacionDTO.setEncargado(estacion.getEncargado());
                estacionDTO.setEstado(estacion.getEstado());
                estacionDTO.setIdTipoCultivo(estacion.getIdTipoCultivo());
                estacionDTO.setDescripcionTipoCultivo(tipoCultivoRepository.findById(estacion.getIdTipoCultivo()).get().getNombre());
                estacionDTO.setNumero_Asociados(usuarioEstacionRepository.countByIdEstacion(estacion.getId()));
                String usuarioEncargado = usuarioRepository.findById(estacion.getEncargado()).get().getUsuario();
                estacionDTO.setUsuarioEncargado(usuarioEncargado);
                estacionesDTO.add(estacionDTO);
            }

            return ResponseEntity.ok().body(estacionesDTO);
        } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
