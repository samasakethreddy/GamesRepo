package com.Games.deployment.controller;

import com.Games.deployment.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        System.out.println("test");
        return ResponseEntity.ok("Hello World");
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadGame(
            @RequestParam("gameFile") MultipartFile gameFile,
            @RequestParam("gameName") String gameName,
            @RequestParam("titleImage") MultipartFile titleImage,
            @RequestParam("tags") List<String> tags) {
        try {
            if(gameFile.isEmpty() || titleImage.isEmpty() || tags.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Fields");
            }
            gameService.processAndUploadGame(gameFile, gameName, titleImage, tags);
            return ResponseEntity.ok("Game uploaded successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading game: " + e.getMessage());
        }
    }
}

