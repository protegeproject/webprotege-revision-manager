package edu.stanford.protege.webprotege.revision;

import edu.stanford.protege.webprotege.common.ProjectId;
import org.semanticweb.owlapi.model.OWLDataFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-29
 */
public class RevisionStoreFactory {

    @Nonnull
    private final ChangeHistoryFileFactory changeHistoryFileFactory;

    @Nonnull
    private final OWLDataFactory dataFactory;

    @Nonnull
    private final OntologyChangeRecordTranslator changeRecordTranslator;

    @Inject
    public RevisionStoreFactory(@Nonnull ChangeHistoryFileFactory changeHistoryFileFactory,
                                @Nonnull OWLDataFactory dataFactory,
                                @Nonnull OntologyChangeRecordTranslator changeRecordTranslator) {
        this.changeHistoryFileFactory = checkNotNull(changeHistoryFileFactory);
        this.dataFactory = checkNotNull(dataFactory);
        this.changeRecordTranslator = checkNotNull(changeRecordTranslator);
    }

    /**
     * Create and load a revision store for the specified project
     * @param projectId The project
     * @return The loaded revision store for the project
     */
    @Nonnull
    public RevisionStore createRevisionStore(@Nonnull ProjectId projectId) {
        checkNotNull(projectId);
        var revisionStore = new RevisionStoreImpl(projectId,
                                     changeHistoryFileFactory,
                                     dataFactory,
                                     changeRecordTranslator);
        revisionStore.load();
        return revisionStore;
    }

}
