/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.test.XSD2OWL;
import org.w3.x2001.xmlschema.Schema;
import uk.ac.manchester.cs.owl.owlapi.OWL2DatatypeImpl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class TestGroupCard {

    private static OWLOntology onto;
    private static OWLOntologyManager manager;
    private static OWLDataFactory factory;
    private static String tns;

    @BeforeClass
    public static void parse() {

        XSD2OWL converter = new XSD2OWL();

        Schema x = converter.parse( "test/groupCard.xsd");
        tns = x.getTargetNamespace() + "#";

        onto = converter.transform( x, true, true );

        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();
    }


    @Test
    public void testMultiCardinalityGroups() {

        try {
            String px = tns;
            OWLClass k = factory.getOWLClass( IRI.create( px, "MultiSource" ) );
            OWLClass j = factory.getOWLClass( IRI.create( px, "AnotherSource" ) );
            OWLClass x = factory.getOWLClass( IRI.create( px, "X" ) );

            OWLDataProperty field = factory.getOWLDataProperty( IRI.create( px, "field" ) );
            OWLObjectProperty prop = factory.getOWLObjectProperty( IRI.create( px, "prop" ) );

            assertTrue( onto.containsAxiom( factory.getOWLDeclarationAxiom( k ) ) );
            assertTrue( onto.containsAxiom( factory.getOWLDeclarationAxiom( j ) ) );
            assertTrue( onto.containsAxiom( factory.getOWLDeclarationAxiom( x ) ) );
            
            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    j,
                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLObjectIntersectionOf(
                                    factory.getOWLObjectAllValuesFrom(
                                            prop,
                                            x ),
                                    factory.getOWLObjectMinCardinality(
                                            0,
                                            prop,
                                            x )
                            )
                    ))
            ));
            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    k,
                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLObjectUnionOf(
                                    factory.getOWLObjectIntersectionOf(
                                            factory.getOWLDataAllValuesFrom(
                                                    field,
                                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_FLOAT) ),
                                            factory.getOWLDataMinCardinality(
                                                    2,
                                                    field,
                                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_FLOAT) ),
                                            factory.getOWLDataMaxCardinality(
                                                    4,
                                                    field,
                                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_FLOAT) )
                                    )
                            )))
            ));


        } catch ( Exception e ) {
            fail( e.getMessage() );
        }

    }


}
