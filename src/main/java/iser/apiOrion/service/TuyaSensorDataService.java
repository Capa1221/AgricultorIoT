package iser.apiOrion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iser.apiOrion.DTO.TuyaSensorReading;
import iser.apiOrion.collection.TuyaSensorData;
import iser.apiOrion.repository.TuyaSensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class TuyaSensorDataService {

    @Autowired
    private TuyaSensorDataRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();

    public TuyaSensorData saveSensorData() {
        try {
            String url = "http://localhost:5173/api/tuya";
            ObjectMapper mapper = new ObjectMapper();
            JsonNode response = mapper.readTree(restTemplate.getForObject(url, String.class));

            TuyaSensorData data = new TuyaSensorData();

            data.setNombre("Sensor Tuya");

            data.setPh(getValueFromNode(response, "ph"));
            data.setOrp(getValueFromNode(response, "orp"));
            data.setEc(getValueFromNode(response, "ec"));
            data.setTds(getValueFromNode(response, "tds"));
            data.setSalinidad(getValueFromNode(response, "salinidad"));
            data.setTemperatura(getValueFromNode(response, "temperatura"));

            long timestamp = getTimeFromNode(response, "ph"); // O cualquiera
            data.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));

            return repository.save(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Double getValueFromNode(JsonNode response, String key) {
        JsonNode node = response.get(key);
        if (node != null && node.has("value")) {
            return node.get("value").asDouble();
        }
        return null;
    }

    private Long getTimeFromNode(JsonNode response, String key) {
        JsonNode node = response.get(key);
        if (node != null && node.has("time")) {
            return node.get("time").asLong();
        }
        return System.currentTimeMillis();
    }

    public List<TuyaSensorData> findAll() {
        return repository.findAll();
    }

    public Optional<TuyaSensorData> findById(String id) {
        return repository.findById(id);
    }

    public TuyaSensorData save(TuyaSensorData data) {
        return repository.save(data);
    }

    public TuyaSensorData update(String id, TuyaSensorData newData) {
        return repository.findById(id).map(existing -> {
            existing.setNombre(newData.getNombre());
            existing.setPh(newData.getPh());
            existing.setOrp(newData.getOrp());
            existing.setEc(newData.getEc());
            existing.setTds(newData.getTds());
            existing.setSalinidad(newData.getSalinidad());
            existing.setTemperatura(newData.getTemperatura());
            existing.setTimestamp(newData.getTimestamp());
            return repository.save(existing);
        }).orElse(null);
    }

    public boolean deleteById(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}





