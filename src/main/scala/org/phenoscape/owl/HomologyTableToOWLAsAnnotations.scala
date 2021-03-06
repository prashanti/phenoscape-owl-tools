package org.phenoscape.owl

import java.io.File
import java.util.UUID

import scala.collection.JavaConversions._
import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.io.Source

import org.phenoscape.scowl.OWL._
import org.phenoscape.owl.util.OBOUtil
import org.semanticweb.owlapi.model.AddImport
import org.semanticweb.owlapi.model.AddOntologyAnnotation
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAxiom
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.vocab.DublinCoreVocabulary

object HomologyTableToOWLAsAnnotations extends OWLTask {

  val manager = this.getOWLOntologyManager;
  val homologousTo = ObjectProperty(Vocab.HOMOLOGOUS_TO);
  val hasEvidence = ObjectProperty(Vocab.EVIDENCE);
  val source = factory.getOWLAnnotationProperty(DublinCoreVocabulary.SOURCE.getIRI());
  val description = factory.getOWLAnnotationProperty(DublinCoreVocabulary.DESCRIPTION.getIRI());
  val aboutStructure = ObjectProperty("http://example.org/about_structure");
  val homologyAnnotation = Class("http://example.org/HomologyAnnotation");
  val negativeHomologyAnnotation = Class("http://example.org/NegativeHomologyAnnotation");

  def main(args: Array[String]): Unit = {
    val input = Source.fromFile(args(0), "utf-8");
    val output = convertFile(input);
    manager.saveOntology(output, IRI.create(new File(args(1))));
  }

  def convertFile(file: Source): OWLOntology = {
    val axioms = file.getLines().drop(1).map(processEntry(_)).flatten.toSet;
    val ontology = manager.createOntology(IRI.create("http://purl.obolibrary.org/obo/uberon/homology_annotations.owl"));
    manager.applyChange(new AddOntologyAnnotation(ontology, factory.getOWLAnnotation(description, factory.getOWLLiteral("Homology Assertions"))));
    manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/uberon/ext.owl"))));
    manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/eco.owl"))));
    manager.addAxioms(ontology, axioms);
    return ontology;
  }

  def processEntry(line: String): Set[OWLAxiom] = {
    val items = line.split("\t", -1);
    val annotation = Individual("http://example.org/" + UUID.randomUUID().toString());
    val structure1 = Individual(IRI.create(items(1).trim()));
    val structure2 = Individual(IRI.create(items(6).trim()));
    val evidenceCode = Class(OBOUtil.iriForTermID(items(10).trim()));
    val evidence = Individual("http://example.org/" + UUID.randomUUID().toString());
    val pub = factory.getOWLLiteral(items(11).trim());
    Set(
      if (items(4).trim() == "hom to") {
        annotation Type homologyAnnotation
      } else {
        annotation Type negativeHomologyAnnotation
      },
      annotation Fact (aboutStructure, structure1),
      annotation Fact (aboutStructure, structure2),
      annotation Fact (hasEvidence, evidence),
      evidence Type evidenceCode,
      evidence Annotation (source, pub));

  }

}