package iser.apiOrion.collection;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;



@Data
@Document(collection = "tuya_sensor_data") // coleccion creada en BD
public class TuyaSensorData {

    @Id
    private String id; // ID automático generado en BD
    private String nombre; //nombre del sensor, como "Sensor Tuya"
 
    private Double ph;
    private Double orp;
    private Double ec;
    private Double tds;
    private Double salinidad;
    private Double temperatura;
    private LocalDateTime timestamp;  // Fecha y hora exacta de la medición
}

