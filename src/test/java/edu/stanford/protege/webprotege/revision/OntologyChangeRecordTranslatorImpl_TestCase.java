package edu.stanford.protege.webprotege.revision;

import edu.stanford.protege.webprotege.change.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.semanticweb.owlapi.change.*;
import org.semanticweb.owlapi.model.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-16
 */
@SpringBootTest
public class OntologyChangeRecordTranslatorImpl_TestCase {

    private OntologyChangeRecordTranslatorImpl impl;

    @Mock
    private OWLOntologyID ontologyId;

    @Mock
    private OWLAxiom axiom;

    @Mock
    private OWLAnnotation annotation;

    @Mock
    private OWLImportsDeclaration importsDeclaration;

    @Mock
    private OWLOntologyID otherOntologyId;

    @BeforeEach
    public void setUp() {
        impl = new OntologyChangeRecordTranslatorImpl();
    }

    @Test
    public void shouldTranslateAddAxiom() {
        var change = impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new AddAxiomData(axiom)));
        assertThat(change.getOntologyId(), is(ontologyId));
        assertThat(change.getAxiomOrThrow(), is(axiom));
        assertThat(change, is(instanceOf(AddAxiomChange.class)));
    }

    @Test
    public void shouldTranslateRemoveAxiom() {
        var change = impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new RemoveAxiomData(axiom)));
        assertThat(change.getOntologyId(), is(ontologyId));
        assertThat(change.getAxiomOrThrow(), is(axiom));
        assertThat(change, is(instanceOf(RemoveAxiomChange.class)));
    }

    @Test
    public void shouldTranslateAddOntologyAnnotation() {
        var change = impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new AddOntologyAnnotationData(annotation)));
        assertThat(change.getOntologyId(), is(ontologyId));
        assertThat(change, is(instanceOf(AddOntologyAnnotationChange.class)));
        assertThat(((AddOntologyAnnotationChange) change).getAnnotation(), is(annotation));
    }

    @Test
    public void shouldTranslateRemoveOntologyAnnotation() {
        var change = impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new RemoveOntologyAnnotationData(annotation)));
        assertThat(change.getOntologyId(), is(ontologyId));
        assertThat(change, is(instanceOf(RemoveOntologyAnnotationChange.class)));
        assertThat(((RemoveOntologyAnnotationChange) change).getAnnotation(), is(annotation));
    }

    @Test
    public void shouldTranslateAddImport() {
        var change = impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new AddImportData(importsDeclaration)));
        assertThat(change.getOntologyId(), is(ontologyId));
        assertThat(change, is(instanceOf(AddImportChange.class)));
        assertThat(((AddImportChange) change).getImportsDeclaration(), is(importsDeclaration));
    }

    @Test
    public void shouldTranslateRemoveImport() {
        var change = impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new RemoveImportData(importsDeclaration)));
        assertThat(change.getOntologyId(), is(ontologyId));
        assertThat(change, is(instanceOf(RemoveImportChange.class)));
        assertThat(((RemoveImportChange) change).getImportsDeclaration(), is(importsDeclaration));
    }

    @Test
    public void shouldNotTranslateSetOntologyID() {
        assertThrows(UnsupportedOperationException.class, () -> {
            impl.getOntologyChange(new OWLOntologyChangeRecord(ontologyId, new SetOntologyIDData(otherOntologyId)));
        });
    }
}
