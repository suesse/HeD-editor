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
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.test.XSD2OWL;
import org.w3.x2001.xmlschema.Schema;
import uk.ac.manchester.cs.owl.owlapi.OWL2DatatypeImpl;

import java.math.BigInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TestComplex {

    private static OWLOntology onto;
    private static OWLOntologyManager manager;
    private static OWLDataFactory factory;

    @BeforeClass
    public static void parse() {
        XSD2OWL converter = new XSD2OWL();

        Schema x = converter.parse( "test/complex1.xsd" );

        onto = converter.transform( x, true, true );

        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();
    }

    @Test
    public void testComplexTypeDataAttribs() {

        try {

            OWLClass x = factory.getOWLClass( IRI.create( "http://asu.edu/test#", "TestComplex" ) );

            assertEquals( 5, onto.getSubClassAxiomsForSubClass( x ).size() );

            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    x,
                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLDataAllValuesFrom(
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "field"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMinCardinality(
                                    0,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "field"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMaxCardinality(
                                    1,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "field"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ) )
                )
            ) );
            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    x,
                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLDataAllValuesFrom(
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "defField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMinCardinality(
                                    0,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "defField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMaxCardinality(
                                    1,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "defField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ) )
                )
            ) );
            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    x,
                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLDataAllValuesFrom(
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "otherField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMinCardinality(
                                    1,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "otherField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMaxCardinality(
                                    1,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "otherField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ) )
                )
            ) );
            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    x,
                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLDataAllValuesFrom(
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "noField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMinCardinality(
                                    0,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "noField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ),
                            factory.getOWLDataMaxCardinality(
                                    0,
                                    factory.getOWLDataProperty( IRI.create( "http://asu.edu/test#", "noField"  ) ),
                                    OWL2DatatypeImpl.getDatatype( OWL2Datatype.XSD_STRING ) ) )
                )
            ) );



        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    @Test
    public void testComplexTypeDataSequence() {

        try {

            assertTrue( onto.containsAxiom( factory.getOWLSubClassOfAxiom(
                    factory.getOWLClass( IRI.create( "http://asu.edu/test#", "TestChild" ) ),
                    factory.getOWLClass( IRI.create( "http://asu.edu/test#", "TestComplex" ) ) ) ) );

            assertTrue(onto.containsAxiom(factory.getOWLSubClassOfAxiom(
                    factory.getOWLClass(IRI.create("http://asu.edu/test#", "TestChild")),

                    factory.getOWLObjectIntersectionOf(
                            factory.getOWLObjectIntersectionOf(
                                    factory.getOWLDataAllValuesFrom(
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField1")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING)),
                                    factory.getOWLDataMinCardinality(
                                            0,
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField1")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING)),
                                    factory.getOWLDataMaxCardinality(
                                            1,
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField1")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING))),
                            factory.getOWLObjectIntersectionOf(
                                    factory.getOWLDataAllValuesFrom(
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField2")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING)),
                                    factory.getOWLDataMinCardinality(
                                            0,
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField2")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING))
                            ),
                            factory.getOWLObjectIntersectionOf(
                                    factory.getOWLDataAllValuesFrom(
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField3")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING)),

                                    factory.getOWLDataMinCardinality(
                                            2,
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField3")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING)),
                                    factory.getOWLDataMaxCardinality(
                                            3,
                                            factory.getOWLDataProperty(IRI.create("http://asu.edu/test#", "baseField3")),
                                            OWL2DatatypeImpl.getDatatype(OWL2Datatype.XSD_STRING))
                            )
                    ))
            ));



        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }
}
