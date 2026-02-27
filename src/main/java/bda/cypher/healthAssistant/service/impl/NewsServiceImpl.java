package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.KeywordResponseDTO;
import bda.cypher.healthAssistant.dto.NewsItemResponseDTO;
import bda.cypher.healthAssistant.dto.NewsSaveRequestDTO;
import bda.cypher.healthAssistant.dto.SavedNewsResponseDTO;
import bda.cypher.healthAssistant.entity.Keyword;
import bda.cypher.healthAssistant.entity.News;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.ConditionCategoryTranslationRepository;
import bda.cypher.healthAssistant.repository.HealthConditionTranslationRepository;
import bda.cypher.healthAssistant.repository.KeywordRepository;
import bda.cypher.healthAssistant.repository.NewsRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewsServiceImpl implements NewsService {
    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final NewsRepository newsRepository;
    private final HealthConditionTranslationRepository conditionTranslationRepository;
    private final ConditionCategoryTranslationRepository categoryTranslationRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public NewsServiceImpl(UserRepository userRepository,
                           KeywordRepository keywordRepository,
                           NewsRepository newsRepository,
                           HealthConditionTranslationRepository conditionTranslationRepository,
                           ConditionCategoryTranslationRepository categoryTranslationRepository) {
        this.userRepository = userRepository;
        this.keywordRepository = keywordRepository;
        this.newsRepository = newsRepository;
        this.conditionTranslationRepository = conditionTranslationRepository;
        this.categoryTranslationRepository = categoryTranslationRepository;
    }

    @Override
    @Transactional
    public List<NewsItemResponseDTO> loadNews(String userEmail) {
        User user = getUser(userEmail);
        if (user.getKeywords().isEmpty()) {
            String conditionKeyword = conditionKeyword(user);
            String categoryKeyword = conditionCategoryKeyword(user);
            String conditionKeywordEn = conditionKeywordEn(user);
            String categoryKeywordEn = conditionCategoryKeywordEn(user);
            List<String> queries = new ArrayList<>();
            if (conditionKeyword != null) {
                queries.add(conditionKeyword);
                ensureKeyword(user, conditionKeyword);
            }
            if (conditionKeywordEn != null) {
                queries.add(conditionKeywordEn);
                ensureKeyword(user, conditionKeywordEn);
            }
            if (categoryKeyword != null && (conditionKeyword == null || !categoryKeyword.equalsIgnoreCase(conditionKeyword))) {
                queries.add(categoryKeyword);
                ensureKeyword(user, categoryKeyword);
            }
            if (categoryKeywordEn != null && (categoryKeyword == null || !categoryKeywordEn.equalsIgnoreCase(categoryKeyword))) {
                queries.add(categoryKeywordEn);
                ensureKeyword(user, categoryKeywordEn);
            }
            if (queries.isEmpty()) {
                queries.add("health");
            }
            return filterResults(loadByQueries(queries), queries);
        }
        Map<String, NewsItemResponseDTO> deduped = new HashMap<>();
        for (Keyword keyword : user.getKeywords()) {
            mergeResults(deduped, keyword.getKeyword());
        }
        return filterResults(new ArrayList<>(deduped.values()),
                user.getKeywords().stream().map(Keyword::getKeyword).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public SavedNewsResponseDTO saveNews(String userEmail, NewsSaveRequestDTO request) {
        User user = getUser(userEmail);
        String link = request.getLink().trim();
        String linkHash = hashLink(link);
        News news = newsRepository.findByLinkHash(linkHash).orElseGet(News::new);
        if (news.getId() == null) {
            news.setTitle(request.getTitle().trim());
            news.setLink(link);
            news.setLinkHash(linkHash);
            String sourceDomain = request.getSourceDomain();
            if (sourceDomain == null || sourceDomain.isBlank()) {
                sourceDomain = extractDomain(link);
            }
            news.setSourceDomain(sourceDomain);
            news.setPublishedAt(request.getPublishedAt());
        }
        user.getSavedNews().add(news);
        news.getUsers().add(user);
        newsRepository.save(news);
        userRepository.save(user);
        return mapSaved(news);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedNewsResponseDTO> getSavedNews(String userEmail) {
        User user = getUser(userEmail);
        return newsRepository.findAllByUsersId(user.getId())
                .stream()
                .map(this::mapSaved)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSavedNews(String userEmail, Long newsId) {
        User user = getUser(userEmail);
        News news = newsRepository.findByIdAndUsersId(newsId, user.getId())
                .orElseThrow(() -> new RuntimeException("Xəbər tapılmadı"));
        user.getSavedNews().remove(news);
        news.getUsers().remove(user);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeywordResponseDTO> getFollowingKeywords(String userEmail) {
        User user = getUser(userEmail);
        return user.getKeywords().stream()
                .filter(k -> !isIgnoredKeyword(k.getKeyword()))
                .map(this::mapKeyword)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public KeywordResponseDTO addKeyword(String userEmail, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new RuntimeException("Keyword boş ola bilməz");
        }
        User user = getUser(userEmail);
        String normalized = normalizeKeyword(keyword);
        Optional<Keyword> existing = keywordRepository.findByKeyword(normalized);
        Keyword entity = existing.orElseGet(() -> {
            Keyword k = new Keyword();
            k.setKeyword(normalized);
            return k;
        });
        user.getKeywords().add(entity);
        entity.getUsers().add(user);
        keywordRepository.save(entity);
        userRepository.save(user);
        return mapKeyword(entity);
    }

    @Override
    @Transactional
    public void deleteKeyword(String userEmail, Long keywordId) {
        User user = getUser(userEmail);
        Keyword keyword = user.getKeywords().stream()
                .filter(k -> k.getId().equals(keywordId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Keyword tapılmadı"));
        user.getKeywords().remove(keyword);
        keyword.getUsers().remove(user);
        userRepository.save(user);
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));
    }

    private String conditionKeyword(User user) {
        if (user.getHealthCondition() == null) {
            return null;
        }
        String name = user.getHealthCondition().getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private String conditionCategoryKeyword(User user) {
        if (user.getHealthCondition() == null || user.getHealthCondition().getCategory() == null) {
            return null;
        }
        String name = user.getHealthCondition().getCategory().getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private String conditionKeywordEn(User user) {
        if (user.getHealthCondition() == null) {
            return null;
        }
        return conditionTranslationRepository.findByConditionId(user.getHealthCondition().getId())
                .map(t -> t.getNameEn() == null || t.getNameEn().isBlank() ? null : t.getNameEn().trim())
                .orElse(null);
    }

    private String conditionCategoryKeywordEn(User user) {
        if (user.getHealthCondition() == null || user.getHealthCondition().getCategory() == null) {
            return null;
        }
        return categoryTranslationRepository.findByCategoryId(user.getHealthCondition().getCategory().getId())
                .map(t -> t.getNameEn() == null || t.getNameEn().isBlank() ? null : t.getNameEn().trim())
                .orElse(null);
    }

    private void ensureKeyword(User user, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String normalized = normalizeKeyword(keyword);
        Optional<Keyword> existing = keywordRepository.findByKeyword(normalized);
        Keyword entity = existing.orElseGet(() -> {
            Keyword k = new Keyword();
            k.setKeyword(normalized);
            return k;
        });
        if (!user.getKeywords().contains(entity)) {
            user.getKeywords().add(entity);
            entity.getUsers().add(user);
            keywordRepository.save(entity);
            userRepository.save(user);
        }
    }

    private List<NewsItemResponseDTO> loadByQueries(List<String> queries) {
        Map<String, NewsItemResponseDTO> deduped = new HashMap<>();
        for (String query : queries) {
            mergeResults(deduped, query);
        }
        return new ArrayList<>(deduped.values());
    }

    private List<NewsItemResponseDTO> filterResults(List<NewsItemResponseDTO> items, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return items;
        }
        List<String> normalized = keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .filter(k -> !isIgnoredKeyword(k))
                .map(k -> k.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return items;
        }
        List<NewsItemResponseDTO> filtered = new ArrayList<>();
        for (NewsItemResponseDTO item : items) {
            String title = item.getTitle();
            if (title == null) {
                continue;
            }
            String lower = title.toLowerCase(Locale.ROOT);
            boolean match = false;
            for (String key : normalized) {
                if (lower.contains(key)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean isIgnoredKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return "digər".equals(normalized) || "other".equals(normalized);
    }

    private void mergeResults(Map<String, NewsItemResponseDTO> deduped, String query) {
        try {
            String url = buildRssUrl(query);
            String body = fetchRss(url);
            List<NewsItemResponseDTO> items = parseRss(body);
            for (NewsItemResponseDTO item : items) {
                if (item.getLink() == null || item.getLink().isBlank()) {
                    continue;
                }
                String hash = hashLink(item.getLink());
                deduped.putIfAbsent(hash, item);
            }
        } catch (Exception ex) {
            log.warn("RSS oxunmadı: {} ({})", query, ex.toString());
        }
    }

    private String buildRssUrl(String keyword) {
        String encoded = URLEncoder.encode(keyword + " when:1d", StandardCharsets.UTF_8);
        return "https://news.google.com/rss/search?q=" + encoded;
    }

    private String fetchRss(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("RSS status=" + response.statusCode());
        }
        return response.body();
    }

    private List<NewsItemResponseDTO> parseRss(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList items = doc.getElementsByTagName("item");
        List<NewsItemResponseDTO> results = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String title = text(item, "title");
            String link = text(item, "link");
            String publishedAt = text(item, "pubDate");
            if (link == null || link.isBlank()) {
                continue;
            }
            String sourceUrl = null;
            NodeList sourceNodes = item.getElementsByTagName("source");
            if (sourceNodes.getLength() > 0) {
                Element source = (Element) sourceNodes.item(0);
                String urlAttr = source.getAttribute("url");
                if (urlAttr != null && !urlAttr.isBlank()) {
                    sourceUrl = urlAttr.trim();
                }
            }
            String domain = extractDomain(sourceUrl != null ? sourceUrl : link);
            NewsItemResponseDTO dto = new NewsItemResponseDTO();
            dto.setTitle(title);
            dto.setLink(link);
            dto.setSourceDomain(domain);
            dto.setPublishedAt(publishedAt);
            results.add(dto);
        }
        return results;
    }

    private String text(Element element, String tag) {
        NodeList list = element.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return null;
        }
        String value = list.item(0).getTextContent();
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private boolean isAllowedDomain(String domain) {
        if (domain == null) {
            return false;
        }
        String lower = domain.toLowerCase(Locale.ROOT);
        return lower.endsWith(".gov") || lower.endsWith(".edu") || lower.endsWith(".org");
    }

    private String extractDomain(String link) {
        try {
            URI uri = URI.create(link);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String hashLink(String link) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(link.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Link hash xətası");
        }
    }

    private SavedNewsResponseDTO mapSaved(News news) {
        SavedNewsResponseDTO dto = new SavedNewsResponseDTO();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setLink(news.getLink());
        dto.setSourceDomain(news.getSourceDomain());
        dto.setPublishedAt(news.getPublishedAt());
        return dto;
    }

    private KeywordResponseDTO mapKeyword(Keyword keyword) {
        KeywordResponseDTO dto = new KeywordResponseDTO();
        dto.setId(keyword.getId());
        dto.setKeyword(keyword.getKeyword());
        return dto;
    }
}
