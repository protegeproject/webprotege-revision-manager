package edu.stanford.protege.webprotege.revision;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interners;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.semanticweb.binaryowl.BinaryOWLOntologyChangeLog;
import org.semanticweb.binaryowl.chunk.SkipSetting;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 29/05/15
 */
public class RevisionStoreImpl implements RevisionStore {

    private static final Logger logger = LoggerFactory.getLogger(RevisionStoreImpl.class);

    private final ExecutorService changeSerializationExecutor;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final ChangeHistoryFileFactory changeHistoryFileFactory;

    @Nonnull
    private final OWLDataFactory dataFactory;

    private ImmutableList<Revision> revisions = ImmutableList.of();

    @Nonnull
    private final OntologyChangeRecordTranslator changeRecordTranslator;

    private Runnable savedHook = () -> {};

    private boolean loaded = false;

    @Inject
    public RevisionStoreImpl(@Nonnull ProjectId projectId,
                             @Nonnull ChangeHistoryFileFactory changeHistoryFileFactory,
                             @Nonnull OWLDataFactory dataFactory,
                             @Nonnull OntologyChangeRecordTranslator changeRecordTranslator) {
        this.projectId = checkNotNull(projectId);
        this.changeHistoryFileFactory = changeHistoryFileFactory;
        this.dataFactory = checkNotNull(dataFactory);
        this.changeRecordTranslator = changeRecordTranslator;
        changeSerializationExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName(thread.getName().replace("thread", "change-serializer-thread"));
            return thread;
        });
    }

    public void setSavedHook(Runnable savedHook) {
        this.savedHook = checkNotNull(savedHook);
    }

    @Nonnull
    @Override
    public Optional<Revision> getRevision(@Nonnull RevisionNumber revisionNumber) {
        if(revisions.isEmpty()) {
            return Optional.empty();
        }
        int index = getRevisionIndexForRevision(revisionNumber);
        if(index < 0 || revisions.size() <= index) {
            return Optional.empty();
        }
        else {
            return Optional.of(revisions.get(index));
        }
    }

    private int getRevisionIndexForRevision(RevisionNumber revision) {
        try {
            readLock.lock();
            if(revisions.isEmpty()) {
                return -1;
            }
            if(revision.isHead()) {
                return revisions.size() - 1;
            }
            var firstRevision = revisions.get(0);
            if(revision.compareTo(firstRevision.getRevisionNumber()) < 0) {
                return -1;
            }
            var lastRevision = revisions.get(revisions.size() - 1);
            if(lastRevision.getRevisionNumber() == revision) {
                return revisions.size() - 1;
            }
            var dummyRevision = Revision.createEmptyRevisionWithRevisionNumber(revision);
            return Collections.binarySearch(revisions, dummyRevision);
        } finally {
            readLock.unlock();
        }
    }

    @Nonnull
    @Override
    public ImmutableList<Revision> getRevisions() {
        try {
            readLock.lock();
            return revisions;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addRevision(@Nonnull Revision revision) {
        checkNotNull(revision);
        try {
            writeLock.lock();
            if(revision.getRevisionNumber().compareTo(getCurrentRevisionNumber()) <= 0) {
                throw new IllegalArgumentException(String.format("Revision number (%d) must be greater than the current revision number (%d)", revision
                        .getRevisionNumber()
                        .getValue(), getCurrentRevisionNumber().getValue()));
            }
            var extendedListBuilder = ImmutableList.<Revision>builder();
            extendedListBuilder.addAll(revisions);
            extendedListBuilder.add(revision);
            revisions = extendedListBuilder.build();
            persistChanges(revision);
        } finally {
            writeLock.unlock();
        }

    }

    @Nonnull
    @Override
    public RevisionNumber getCurrentRevisionNumber() {
        try {
            readLock.lock();
            if(revisions.isEmpty()) {
                return RevisionNumber.getRevisionNumber(0);
            }
            return revisions.get(revisions.size() - 1).getRevisionNumber();
        } finally {
            readLock.unlock();
        }

    }

    private void persistChanges(Revision revision) {
        try {
            writeLock.lock();
            var changeHistoryFile = changeHistoryFileFactory.getChangeHistoryFile(projectId);
            var revisionSerializationTask = new RevisionSerializationTask(changeHistoryFile, revision);
            revisionSerializationTask.setSavedHook(savedHook);
            if(revisions.size() != 1) {
                changeSerializationExecutor.submit(revisionSerializationTask);
            }
            else {
                // Save immediately
                try {
                    logger.info("{} Saving first revision of project", projectId);
                    revisionSerializationTask.call();
                } catch(IOException e) {
                    logger.error("{} An error occurred whilst saving the first revision of the project.  Cause: {}.", projectId, e
                            .getMessage(), e);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }


    /**
     * Load this revision store.  This method is safe in that it can be called multiple times by different threads.
     */
    public void load() {
        try {
            writeLock.lock();
            if(loaded) {
                return;
            }
            var changeHistoryFile = changeHistoryFileFactory.getChangeHistoryFile(projectId);
            if(!changeHistoryFile.exists()) {
                changeHistoryFile.getParentFile().mkdirs();
                return;
            }
            var revisionsBuilder = ImmutableList.<Revision>builder();
            var metadataInterner = Interners.<String>newStrongInterner();
            var userIdInterner = Interners.<UserId>newStrongInterner();

            try {
                logger.info("{} Loading change history", projectId);
                var stopwatch = Stopwatch.createStarted();
                var changeLog = new BinaryOWLOntologyChangeLog();
                var inputStream = new BufferedInputStream(new FileInputStream(changeHistoryFile));
                changeLog.readChanges(inputStream, dataFactory, (changeRecordList, skipSetting, l) -> {
                    var metadata = changeRecordList.getMetadata();
                    var userName = metadataInterner.intern(metadata.getStringAttribute(RevisionSerializationVocabulary.USERNAME_METADATA_ATTRIBUTE.getVocabularyName(), ""));
                    var revisionNumberValue = metadata.getLongAttribute(RevisionSerializationVocabulary.REVISION_META_DATA_ATTRIBUTE.getVocabularyName(), 0L);
                    var revisionNumber = RevisionNumber.getRevisionNumber(revisionNumberValue);
                    var description = metadata.getStringAttribute(RevisionSerializationVocabulary.DESCRIPTION_META_DATA_ATTRIBUTE.getVocabularyName(), "");
                    var userId = userIdInterner.intern(UserId.valueOf(userName));

                    var internedChangeRecords = changeRecordList.getChangeRecords()
                            .stream()
                            .map(changeRecordTranslator::getOntologyChange)
                            .collect(toImmutableList());
                    var revision = new Revision(userId, revisionNumber, internedChangeRecords, changeRecordList.getTimestamp(), description);
                    revisionsBuilder.add(revision);
                }, SkipSetting.SKIP_NONE);
                inputStream.close();
                stopwatch.stop();
                logger.info("{} Change history loading complete.  Loaded {} revisions in {} ms.", projectId, revisions.size(), stopwatch
                        .elapsed(TimeUnit.MILLISECONDS));
            } catch(Exception e) {
                logger.error("{} Failed to load change history for project.  Cause: {}", projectId, e.getMessage(), e);
            }
            revisions = revisionsBuilder.build();
            loaded = true;
        } finally {
            writeLock.unlock();
        }


    }

    public void dispose() {
        changeSerializationExecutor.shutdown();
    }
}
