package iser.apiOrion.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TuyaSensorDataDTO {
    private String nombre;
    //private Double ph;
    //private Double orp;
    //private Double ec;
    //private Double tds;
    //private Double salinidad;
    private Double temperatura;
    private LocalDateTime timestamp; 
}
