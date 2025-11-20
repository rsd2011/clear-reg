package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.batch.ingestion.feed.DataFeed;
import com.example.batch.ingestion.feed.DataFeedConnector;
import com.example.batch.ingestion.template.DwFeedIngestionTemplate;
import com.example.batch.ingestion.template.DwIngestionResult;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.dw.domain.HrBatchStatus;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootTest(classes = DwIngestionConcurrencyTest.TestConfiguration.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:dw-ingestion-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class DwIngestionConcurrencyTest {

    @Autowired
    private DwIngestionService ingestionService;

    @Autowired
    private HrBatchRepository batchRepository;

    @Autowired
    private TestDataFeedConnector dataFeedConnector;

    @Autowired
    private TestFeedIngestionTemplate ingestionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanup() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute(status -> {
            entityManager.createQuery("DELETE FROM HrImportBatchEntity").executeUpdate();
            entityManager.clear();
            return null;
        });
    }

    @Test
    void concurrentIngestionPersistsDistinctBatches() throws Exception {
        dataFeedConnector.enqueue(feed("feed-1", 1));
        dataFeedConnector.enqueue(feed("feed-2", 2));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ingestionTemplate.setBarriers(ready, release);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Void> task = () -> {
            ingestionService.ingestNextFile();
            return null;
        };

        List<Future<Void>> futures = executor.invokeAll(List.of(task, task));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        for (Future<Void> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        List<HrImportBatchEntity> batches = entityManager.createQuery("SELECT b FROM HrImportBatchEntity b", HrImportBatchEntity.class)
                .getResultList();
        assertThat(batches).hasSize(2);
        assertThat(batches)
                .allSatisfy(batch -> assertThat(batch.getStatus()).isEqualTo(HrBatchStatus.COMPLETED));
        assertThat(batches).extracting(HrImportBatchEntity::getFileName)
                .containsExactlyInAnyOrder("feed-1", "feed-2");
    }

    private DataFeed feed(String id, int sequence) {
        return new DataFeed(id,
                DataFeedType.EMPLOYEE,
                LocalDate.of(2024, 1, 1),
                sequence,
                "{}",
                "TEST",
                java.util.Map.of());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.example.dw")
    @EnableJpaRepositories(basePackages = "com.example.dw.infrastructure.persistence")
    @org.springframework.context.annotation.ComponentScan(basePackages = "com.example.dw.infrastructure.persistence")
    static class TestConfiguration {

        @Bean
        DwIngestionService dwIngestionService(List<DataFeedConnector> connectors,
                                              List<DwFeedIngestionTemplate> templates,
                                              HrBatchRepository repository) {
            return new DwIngestionService(connectors, templates, repository);
        }

        @Bean
        @Primary
        TestDataFeedConnector testDataFeedConnector() {
            return new TestDataFeedConnector();
        }

        @Bean
        @Primary
        TestFeedIngestionTemplate testFeedIngestionTemplate() {
            return new TestFeedIngestionTemplate();
        }
    }

    static class TestDataFeedConnector implements DataFeedConnector {

        private final java.util.concurrent.BlockingQueue<DataFeed> queue = new java.util.concurrent.LinkedBlockingQueue<>();

        void enqueue(DataFeed feed) {
            queue.add(feed);
        }

        @Override
        public java.util.Optional<DataFeed> nextFeed() {
            return java.util.Optional.ofNullable(queue.poll());
        }

        @Override
        public String name() {
            return "test";
        }
    }

    static class TestFeedIngestionTemplate implements DwFeedIngestionTemplate {

        private volatile CountDownLatch ready;
        private volatile CountDownLatch release;

        void setBarriers(CountDownLatch ready, CountDownLatch release) {
            this.ready = ready;
            this.release = release;
        }

        @Override
        public DataFeedType supportedType() {
            return DataFeedType.EMPLOYEE;
        }

        @Override
        public DwIngestionResult ingest(HrImportBatchEntity batch, String payload) {
            CountDownLatch readyLatch = this.ready;
            CountDownLatch releaseLatch = this.release;
            if (readyLatch != null && releaseLatch != null) {
                readyLatch.countDown();
                try {
                    releaseLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return new DwIngestionResult(1, 1, 0, 0);
        }
    }
}
