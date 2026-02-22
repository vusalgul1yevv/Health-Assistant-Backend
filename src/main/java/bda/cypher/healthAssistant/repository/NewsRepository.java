package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findByLinkHash(String linkHash);
    List<News> findAllByUsersId(Long userId);
    Optional<News> findByIdAndUsersId(Long id, Long userId);
}
