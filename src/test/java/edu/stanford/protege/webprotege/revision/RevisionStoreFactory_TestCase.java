package edu.stanford.protege.webprotege.revision;

import edu.stanford.protege.webprotege.common.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-24
 */
@SpringBootTest
public class RevisionStoreFactory_TestCase {

    @TempDir
    static Path tempDir;

    @Autowired
    private RevisionStoreFactory revisionStoreFactory;

    private ProjectId projectId;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("webprotege.directories.data", () -> getDataPath().toString());
    }

    private static Path getDataPath() {
        return tempDir;
    }

    @BeforeEach
    void setUp() {
        projectId = ProjectId.generate();
    }

    @Test
    void shouldCreateRevisionStoreAndDirectoryLayout() {
        var revisionStore = revisionStoreFactory.createRevisionStore(projectId);
        assertThat(revisionStore).isNotNull();
        assertThat(getDataPath().resolve("data-store").resolve("project-data").resolve(projectId.id()).resolve("change-data")).exists();
    }
}
