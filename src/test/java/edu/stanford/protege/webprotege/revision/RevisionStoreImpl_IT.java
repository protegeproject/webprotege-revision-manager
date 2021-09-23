package edu.stanford.protege.webprotege.revision;

import com.google.common.collect.ImmutableList;
import edu.stanford.protege.webprotege.change.AddAxiomChange;
import edu.stanford.protege.webprotege.change.OntologyChange;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.semanticweb.owlapi.model.*;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-29
 */
@SpringBootTest
public class RevisionStoreImpl_IT {

    @TempDir
    Path tempDir;

    private RevisionStoreImpl store;

    private OWLOntologyID ontologyId;

    private OWLAxiom axiom;

    private File changeHistoryFile;

    private ProjectId projectId;

    private OWLDataFactoryImpl dataFactory;

    private OntologyChangeRecordTranslatorImpl changeRecordTranslator;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Mock
    private ChangeHistoryFileFactory changeHistoryFileFactory;

    @BeforeEach
    public void setUp() throws IOException {
        projectId = ProjectId.valueOf(UUID.randomUUID()
                                          .toString());
        changeHistoryFile = tempDir.resolve("change-history-file").toFile();
        when(changeHistoryFileFactory.getChangeHistoryFile(projectId))
                .thenReturn(changeHistoryFile);
        dataFactory = new OWLDataFactoryImpl();
        changeRecordTranslator = new OntologyChangeRecordTranslatorImpl();

        ontologyId = new OWLOntologyID(IRI.create("http://example.org/OntA"));
        var clsA = dataFactory.getOWLClass(IRI.create("http://example.org/A"));
        var clsB = dataFactory.getOWLClass(IRI.create("http://example.org/A"));
        axiom = dataFactory.getOWLSubClassOfAxiom(clsA, clsB);

        store = new RevisionStoreImpl(projectId,
                                      changeHistoryFileFactory,
                                      dataFactory,
                                      changeRecordTranslator);
    }

    @Test
    public void shouldAddRevision() {
        var revision = createRevision();
        store.addRevision(revision);
        var revisions = store.getRevisions();
        assertThat(revisions, contains(revision));
    }

    @Test
    public void shouldHaveZeroRevisionNumberAtStart() {
        var revisionNumber = store.getCurrentRevisionNumber();
        assertThat(revisionNumber.getValue(), is(0L));
    }

    @Test
    public void shouldIncrementCurrentRevisionNumber() {
        var revision = createRevision();
        store.addRevision(revision);
        var revisionNumber = store.getCurrentRevisionNumber();
        assertThat(revisionNumber.getValue(), is(1L));
    }

    @Test
    public void shouldThrowIllegalArgumentIfAddedRevisionNumberIsEqualToCurrentRevisionNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            var revision = createRevision();
            store.addRevision(revision);
            store.addRevision(revision);
        });
    }

    @Test
    public void shouldThrowIllegalArgumentIfAddedRevisionNumberIsLessThanCurrentRevisionNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            var revision = createRevision();
            store.addRevision(revision);
            var smallerRevision = createRevision(RevisionNumber.getRevisionNumber(0));
            store.addRevision(smallerRevision);
        });
    }

    @Test
    public void shouldSaveFirstRevisionImmediately() {
        var revision = createRevision();
        assertThat(changeHistoryFile.length(), is(0L));
        store.addRevision(revision);
        assertThat(changeHistoryFile.length(), is(greaterThan(0L)));
    }

    @Test
    public synchronized void shouldSaveSubsequentRevisions() throws InterruptedException {
        store.addRevision(createRevision(RevisionNumber.getRevisionNumber(1)));
        var initialLength = changeHistoryFile.length();
        // Add hook to be notified of when the save has taken place
        store.setSavedHook(() -> countDownLatch.countDown());
        store.addRevision(createRevision(RevisionNumber.getRevisionNumber(2)));
        // Wait until it has been saved
        countDownLatch.await();
        var nextLength = changeHistoryFile.length();
        assertThat(nextLength, is(greaterThan(initialLength)));
    }

    @Test
    public void shouldGetRevision() {
        var revision = createRevision();
        store.addRevision(revision);
        var retrievedRevision = store.getRevision(RevisionNumber.getRevisionNumber(1));
        assertThat(retrievedRevision, is(equalTo(Optional.of(revision))));
    }

    @Test
    public void shouldNotGetRevision() {
        var revision = store.getRevision(RevisionNumber.getRevisionNumber(1));
        assertThat(revision.isEmpty(), is(true));
    }

    @Test
    public void shouldLoadSavedRevision() {
        var revision = createRevision();
        store.addRevision(revision);
        var otherStore = new RevisionStoreImpl(projectId, changeHistoryFileFactory, dataFactory, changeRecordTranslator);
        otherStore.load();
        var revisions = store.getRevisions();
        assertThat(revisions, contains(revision));
        otherStore.dispose();
    }

    private Revision createRevision() {
        var revisionNumber = RevisionNumber.getRevisionNumber(1);
        return createRevision(revisionNumber);
    }

    private Revision createRevision(RevisionNumber revisionNumber) {
        var changes = ImmutableList.<OntologyChange>of(
                AddAxiomChange.of(ontologyId, axiom)
        );
        var userId = UserId.valueOf("The User");
        var timestamp = System.currentTimeMillis();
        var highLevelDescription = "A change that was mad";
        return new Revision(userId,
                                         revisionNumber,
                                         changes,
                                         timestamp,
                                         highLevelDescription);
    }

    @AfterEach
    public void tearDown() throws Exception {
        store.dispose();
    }
}
