package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.KeywordResponseDTO;
import bda.cypher.healthAssistant.dto.NewsItemResponseDTO;
import bda.cypher.healthAssistant.dto.NewsSaveRequestDTO;
import bda.cypher.healthAssistant.dto.SavedNewsResponseDTO;

import java.util.List;

public interface NewsService {
    List<NewsItemResponseDTO> loadNews(String userEmail);
    SavedNewsResponseDTO saveNews(String userEmail, NewsSaveRequestDTO request);
    List<SavedNewsResponseDTO> getSavedNews(String userEmail);
    void deleteSavedNews(String userEmail, Long newsId);
    List<KeywordResponseDTO> getFollowingKeywords(String userEmail);
    KeywordResponseDTO addKeyword(String userEmail, String keyword);
    void deleteKeyword(String userEmail, Long keywordId);
}
