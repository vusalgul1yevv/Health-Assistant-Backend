package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.KeywordResponseDTO;
import bda.cypher.healthAssistant.dto.NewsItemResponseDTO;
import bda.cypher.healthAssistant.dto.NewsSaveRequestDTO;
import bda.cypher.healthAssistant.dto.SavedNewsResponseDTO;
import bda.cypher.healthAssistant.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping("/load")
    public ResponseEntity<List<NewsItemResponseDTO>> load(Authentication authentication) {
        return ResponseEntity.ok(newsService.loadNews(authentication.getName()));
    }

    @PostMapping("/save")
    public ResponseEntity<SavedNewsResponseDTO> save(@Valid @RequestBody NewsSaveRequestDTO request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(newsService.saveNews(authentication.getName(), request));
    }

    @GetMapping("/saved/all")
    public ResponseEntity<List<SavedNewsResponseDTO>> saved(Authentication authentication) {
        return ResponseEntity.ok(newsService.getSavedNews(authentication.getName()));
    }

    @PostMapping("/saved/{id}/delete")
    public ResponseEntity<Void> deleteSaved(@PathVariable Long id, Authentication authentication) {
        newsService.deleteSavedNews(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/following-keyword/all")
    public ResponseEntity<List<KeywordResponseDTO>> following(Authentication authentication) {
        return ResponseEntity.ok(newsService.getFollowingKeywords(authentication.getName()));
    }

    @PostMapping("/following-keyword/add")
    public ResponseEntity<KeywordResponseDTO> add(@RequestParam String keyword, Authentication authentication) {
        return ResponseEntity.ok(newsService.addKeyword(authentication.getName(), keyword));
    }

    @PostMapping("/following-keyword/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        newsService.deleteKeyword(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
