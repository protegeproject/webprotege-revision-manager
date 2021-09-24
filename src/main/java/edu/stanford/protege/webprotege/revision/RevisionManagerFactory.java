package edu.stanford.protege.webprotege.revision;

import edu.stanford.protege.webprotege.common.ProjectId;

import javax.annotation.Nonnull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-24
 */
public class RevisionManagerFactory {

    private final RevisionStoreFactory revisionStoreFactory;

    public RevisionManagerFactory(RevisionStoreFactory revisionStoreFactory) {
        this.revisionStoreFactory = revisionStoreFactory;
    }

    @Nonnull
    public RevisionManager createRevisionManager(@Nonnull ProjectId projectId) {
        var revisionStore = revisionStoreFactory.createRevisionStore(projectId);
        return new RevisionManagerImpl(revisionStore);
    }
}
