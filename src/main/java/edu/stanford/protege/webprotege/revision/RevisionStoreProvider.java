package edu.stanford.protege.webprotege.revision;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 02/06/15
 */
public class RevisionStoreProvider {

    @Nonnull
    private final RevisionStoreImpl revisionStore;

    private boolean loaded = false;

    @Inject
    public RevisionStoreProvider(@Nonnull RevisionStoreImpl revisionStore) {
        this.revisionStore = checkNotNull(revisionStore);
    }

    public synchronized RevisionStoreImpl get() {
        if(!loaded) {
            revisionStore.load();
            loaded = true;
        }
        return revisionStore;
    }
}
