package org.phenoscape.mod.zfin

import org.phenoscape.owl.OWLTask
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.Set
import org.semanticweb.owlapi.model.OWLOntology
import java.io.File
import scala.io.Source
import org.semanticweb.owlapi.model.OWLAxiom
import org.phenoscape.owl.Vocab
import org.apache.commons.lang3.StringUtils
import org.phenoscape.owl.util.OBOUtil
import org.semanticweb.owlapi.model.IRI

object ZFINExpressionToOWL extends OWLTask {

	val occursIn = factory.getOWLObjectProperty(Vocab.OCCURS_IN);
	val partOf = factory.getOWLObjectProperty(Vocab.PART_OF);
	val annotatedGene = factory.getOWLObjectProperty(Vocab.ANNOTATED_GENE);

	def main(args: Array[String]): Unit = {
			val file = Source.fromFile(args(0), "utf-8");
			file.close();
			val ontology = convert(file);
	}

	def convert(expressionData: Source): OWLOntology = {
			val manager = this.getOWLOntologyManager();
			val ontology = manager.createOntology();
			manager.addAxioms(ontology, expressionData.getLines.map(translate(_)).flatten.toSet[OWLAxiom]);
			return ontology;
	}

	def translate(expressionLine: String): Set[OWLAxiom] = {
			val items = expressionLine.split("\t");
			val axioms = mutable.Set[OWLAxiom]();
			val expression = nextIndividual();
			axioms.add(factory.getOWLDeclarationAxiom(expression));
			val structure = nextIndividual();
			axioms.add(factory.getOWLDeclarationAxiom(structure));
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(occursIn, expression, structure));
			val superStructureID = StringUtils.stripToNull(items(3));
			val subStructureID = StringUtils.stripToNull(items(6));
			val structureType = if (subStructureID == null) {
				factory.getOWLClass(OBOUtil.iriForTermID(superStructureID));
			} else {
				val superStructure = factory.getOWLClass(OBOUtil.iriForTermID(superStructureID));
				val subStructure = factory.getOWLClass(OBOUtil.iriForTermID(subStructureID));
				factory.getOWLObjectIntersectionOf(subStructure, factory.getOWLObjectSomeValuesFrom(partOf, superStructure));
			}
			axioms.add(factory.getOWLClassAssertionAxiom(structureType, structure));
			val geneIRI = IRI.create("http://zfin.org/" + StringUtils.stripToNull(items(0)));
			val gene = factory.getOWLNamedIndividual(geneIRI);
			axioms.add(factory.getOWLDeclarationAxiom(gene));
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(annotatedGene, expression, gene));
			return axioms;
	}

}