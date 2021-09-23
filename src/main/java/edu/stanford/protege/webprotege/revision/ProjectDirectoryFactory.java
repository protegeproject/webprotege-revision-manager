package edu.stanford.protege.webprotege.revision;

import edu.stanford.protege.webprotege.common.ProjectId;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.io.File;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-23
 */
public class ProjectDirectoryFactory {

    private final File dataDirectory;

    @Inject
    public ProjectDirectoryFactory(@Value("${webprotege.directories.data}") File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public File getProjectDirectory(ProjectId projectId) {
        return new File(getProjectDataDirectory(), projectId.id());
    }

    private File getProjectDataDirectory() {
        return new File(getDataStoreDirectory(), "project-data");
    }

    private File getDataStoreDirectory() {
        return new File(dataDirectory, "data-store");
    }

}