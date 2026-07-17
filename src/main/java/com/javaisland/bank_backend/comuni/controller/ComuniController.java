package com.javaisland.bank_backend.comuni.controller;

import com.javaisland.bank_backend.comuni.dto.ComuneDto;
import com.javaisland.bank_backend.comuni.service.ComuniService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comuni")
@RequiredArgsConstructor
public class ComuniController {

    private final ComuniService comuniService;

    @GetMapping
    public ResponseEntity<List<ComuneDto>> search(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(comuniService.search(search));
    }
}
