package edu.stanford.protege.webprotege.revision;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;

@SpringBootApplication
public class WebProtegeRevisionManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebProtegeRevisionManagerApplication.class, args);
	}

	@ConditionalOnMissingBean
	@Bean
	OWLDataFactory dataFactory() {
		return new OWLDataFactoryImpl();
	}

	@ConditionalOnMissingBean
	@Bean
	OntologyChangeRecordTranslator ontologyChangeRecordTranslator() {
		return new OntologyChangeRecordTranslatorImpl();
	}

	@ConditionalOnMissingBean
	@Bean
	ChangeHistoryFileFactory changeHistoryFileFactory(ProjectDirectoryFactory projectDirectoryFactory) {
		return new ChangeHistoryFileFactory(projectDirectoryFactory);
	}

	@ConditionalOnMissingBean
	@Bean
	ProjectDirectoryFactory projectDirectoryFactory(@Value("${webprotege.directories.data}") File dataDirectory) {
		return new ProjectDirectoryFactory(dataDirectory);
	}

	@ConditionalOnMissingBean
	@Bean
	RevisionManagerFactory revisionManagerFactory(RevisionStoreFactory p1) {
		return new RevisionManagerFactory(p1);
	}

	@ConditionalOnMissingBean
	@Bean
	RevisionStoreFactory revisionStoreFactory(ChangeHistoryFileFactory p1,
											  OWLDataFactory p2,
											  OntologyChangeRecordTranslator p3) {
		return new RevisionStoreFactory(p1, p2, p3);
	}
}
