package com.javaisland.bank_backend.comuni.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.bank_backend.comuni.dto.ComuneDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ComuniService {

    private List<ComuneDto> allComuni;
    private List<String> allComuniLower;

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("comuni.json").getInputStream();
            List<Map<String, Object>> raw = mapper.readValue(is, new TypeReference<>() {});
            allComuni = new ArrayList<>(raw.size());
            allComuniLower = new ArrayList<>(raw.size());
            for (Map<String, Object> c : raw) {
                if (c.get("sigla") != null && c.get("nome") != null) {
                    String nome = (String) c.get("nome");
                    allComuni.add(new ComuneDto(nome, (String) c.get("sigla"), (String) c.get("codiceCatastale")));
                    allComuniLower.add(nome.toLowerCase());
                }
            }
            log.info("Caricati {} comuni da comuni.json", allComuni.size());
        } catch (Exception e) {
            log.error("Errore caricamento comuni.json", e);
            allComuni = List.of();
            allComuniLower = List.of();
        }
    }

    public List<ComuneDto> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String q = query.trim().toLowerCase();

        List<ComuneDto> result = new ArrayList<>(10);

        for (int i = 0; i < allComuni.size() && result.size() < 10; i++) {
            if (allComuniLower.get(i).startsWith(q)) {
                result.add(allComuni.get(i));
            }
        }
        if (result.size() >= 10) {
            return result;
        }

        for (int i = 0; i < allComuni.size() && result.size() < 10; i++) {
            if (allComuniLower.get(i).contains(q) && !allComuniLower.get(i).startsWith(q)) {
                result.add(allComuni.get(i));
            }
        }
        return result;
    }
}
