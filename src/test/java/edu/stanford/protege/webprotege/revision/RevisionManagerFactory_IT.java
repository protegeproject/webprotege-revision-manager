package edu.stanford.protege.webprotege.revision;

import edu.stanford.protege.webprotege.change.AddAxiomChange;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-24
 */
@SpringBootTest
public class RevisionManagerFactory_IT {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("webprotege.directories.data", () -> tempDir.toString());
    }


    @Autowired
    private RevisionManagerFactory revisionManagerFactory;

    private ProjectId projectId;



    @BeforeEach
    void setUp() {
        projectId = ProjectId.generate();
    }

    @Test
    void shouldCreateRevisionFactory() {
        revisionManagerFactory.createRevisionManager(projectId);
        assertThat(tempDir.resolve("data-store").resolve("project-data").resolve(projectId.id()).resolve("change-data")).exists();
    }
}
