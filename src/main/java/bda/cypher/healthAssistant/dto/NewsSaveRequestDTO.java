package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.NotBlank;

public class NewsSaveRequestDTO {
    @NotBlank(message = "Title boş ola bilməz")
    private String title;

    @NotBlank(message = "Link boş ola bilməz")
    private String link;

    private String sourceDomain;

    private String publishedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSourceDomain() {
        return sourceDomain;
    }

    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
}
