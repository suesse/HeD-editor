package sharpc2b.transform

import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.util.DefaultPrefixManager

import java.util.regex.Pattern

/**
 * User: rk
 * Date: 4/24/13
 *
 * Transform a SKOS A-Box ICD9 codes ontology into a T-Box ontology.
 */
public class MakeCodeClasses {

    static String ontologiesHttpFileRoot =
        "/Users/rk/asu/prj" +
                "/sharp-editor/model-transform/src/test/resources/http";
    static String ontologiesDocUriRoot = "file:" + ontologiesHttpFileRoot;

    static String sharpCodesOntsRelPath = "/asu.edu/sharpc2b/codes/03/";

    /*
     * SKOS
     */
    static String skosRelPath = "/www.w3.org/2004/02/skos/core";
    static String skosRootPath = ontologiesHttpFileRoot + skosRelPath;
    static String skosUriPath = "http:/" + skosRelPath;
    static String skosNamespace = skosUriPath + "#";
    static IRI skosIRI = new IRI( skosUriPath );
    static IRI skosDocIRI = new IRI( ontologiesDocUriRoot + skosRelPath + ".rdf" );

    /*
     * Published ICD9 Codes Ontology
     */
    static String pubCodesOntRelPath = sharpCodesOntsRelPath + "icd9-pub";
//    static String sharpCodesOntRelPath = sharpCodesOntsRelPath +"icd9-Sharp" ;
    static String pubCodesUriPath = "http:/" + pubCodesOntRelPath;
    static String pubCodesNamespace = pubCodesUriPath + "#";
    static IRI pubCodesIRI = new IRI( pubCodesUriPath );
    static IRI pubCodesDocIRI = new IRI( ontologiesDocUriRoot + pubCodesOntRelPath + ".ofn" );

    /*
     * Sharp Ontology of ICD9 Code OWL Classes
     */
    static String sharpCodesOntRelPath = sharpCodesOntsRelPath + "icd9-classes";
    static String sharpCodesUriPath = "http:/" + sharpCodesOntRelPath;
    static String sharpCodesNamespace = sharpCodesUriPath + "#";
    static IRI sharpCodesIRI = new IRI( sharpCodesUriPath );
    static IRI sharpCodesDocIRI = new IRI( ontologiesDocUriRoot + sharpCodesOntRelPath + ".ofn" );


    OWLOntologyManager oom;
    OWLDataFactory odf;

    PrefixManager pm;

    OWLOntology skos;
    OWLOntology onta;
    OWLOntology ontt;
    Set<OWLOntology> onts;

    OWLClass topCodeClass;
    OWLObjectProperty refinesProp;
    OWLObjectProperty skosBroaderTransitive;
    OWLAnnotationProperty prefLabelProp;
    OWLDataProperty icd9Prop;

    void setUp (OWLOntology onta) {
        oom = onta.getOWLOntologyManager();
        odf = oom.getOWLDataFactory();

//        skos = oom.loadOntologyFromOntologyDocument( new File( skosRootPath + ".rdf" ) );
//        onta = oom.loadOntologyFromOntologyDocument( new File(
//                ontologiesHttpFileRoot + pubCodesOntRelPath + ".ofn" ) );
        println "SKOS Doc IRI = <${skosDocIRI}>";

        assert new File( skosDocIRI.toURI() ).exists();
        assert new File( pubCodesDocIRI.toURI() ).exists();

//        skos = oom.loadOntologyFromOntologyDocument( skosDocIRI );

        onts = new HashSet<OWLOntology>();
//        onts.add( skos );
        onts.add( onta );
//        onts = [skos, onta];
    }

    public static void main (String[] args) {

        new MakeCodeClasses().createClassesOntology();
    }

    OWLOntology createClassesOntology (OWLOntology icd9Published) {

//        OWLOntology ontt;
        onta = icd9Published;

        setUp( onta );

        initNamespaces();
        ontt = createNewOntology();
        addImports();
        initObjects();
        addCommonAxioms();
        addAxiomsForCodes();

        setUpOntologyFormat();
        serialize();

        return this.ontt;
    }

    def initNamespaces () {

//        icd9ClassesIriString = sharpUriRoot + "/" + "icd9Classes"
//        icd9ClassesNamespace = icd9ClassesIriString + "#"
//        icd9ClassesIRI = new IRI( icd9ClassesIriString );
//        icd9ClassesDocIRI = new IRI( ontRootPath + "/" + "icd9Classes" + ".ofn" );

        pm = new DefaultPrefixManager( sharpCodesNamespace );
        pm.setPrefix( "a:", pubCodesNamespace );
        pm.setPrefix( "t:", sharpCodesNamespace );
        pm.setPrefix( "skos:", skosNamespace );
    }

    def createNewOntology () {

        return oom.createOntology( sharpCodesIRI );
    }

    def addImports () {
        OWLImportsDeclaration importsAxiom;
        AddImport imp;

        importsAxiom = odf.getOWLImportsDeclaration( onta.getOntologyID().getOntologyIRI() );
        imp = new AddImport( ontt, importsAxiom );
        oom.applyChange( imp );

        importsAxiom = odf.getOWLImportsDeclaration( skos.getOntologyID().getOntologyIRI() );
        imp = new AddImport( ontt, importsAxiom );
        oom.applyChange( imp );
    }

    def initObjects () {

        topCodeClass = odf.getOWLClass( "a:ICD9_Concept", pm );
        refinesProp = odf.getOWLObjectProperty( "t:refines", pm );
        icd9Prop = odf.getOWLDataProperty( "skos:notation", pm );
        skosBroaderTransitive = odf.getOWLObjectProperty( "skos:broaderTransitive", pm );
        prefLabelProp = odf.getOWLAnnotationProperty( "skos:prefLabel", pm );
    }

    def addCommonAxioms () {

        Set<OWLAxiom> axioms = new TreeSet();
        axioms.add( odf.getOWLSubObjectPropertyOfAxiom( skosBroaderTransitive, refinesProp ) );
        oom.addAxioms( ontt, axioms );
    }

    def addAxiomsForCodes () {

        Set<OWLIndividual> codeIndividuals = topCodeClass.getIndividuals( onta );
        assert codeIndividuals.size() > 0;

        for (OWLIndividual ind : codeIndividuals) {
            addAxiomsForCode( (OWLNamedIndividual) ind );
        }
    }

    def addAxiomsForCode (OWLNamedIndividual codeInd) {

        Set<OWLAnnotationAssertionAxiom> annos = onta.getAnnotationAssertionAxioms( codeInd.getIRI() );
        Set<OWLAnnotationAssertionAxiom> labelAnnos = annos.findAll {
            it.getProperty().equals( prefLabelProp )
        };
        assert 1 == labelAnnos.size();
        OWLAnnotationValue value = labelAnnos.iterator().next().getValue();
        assert value instanceof OWLLiteral;
        String label = ((OWLLiteral) value).getLiteral();
        String name = localName( label );

        OWLClass codeClass = odf.getOWLClass( ":" + name, pm );

        addDefinitionUsingIndividual( codeInd, codeClass )
        addDefinitionUsingCodeValue( codeInd, codeClass )
    }

    def addDefinitionUsingIndividual (OWLNamedIndividual codeInd, OWLClass codeClass) {

        OWLObjectHasValue hasCodeValue = odf.getOWLObjectHasValue( refinesProp, codeInd );
        OWLObjectIntersectionOf codeConceptAndValue = odf.getOWLObjectIntersectionOf( hasCodeValue,
                topCodeClass );
        OWLEquivalentClassesAxiom eqAxiom = odf.getOWLEquivalentClassesAxiom( codeClass,
                codeConceptAndValue );
        assert eqAxiom;
        oom.addAxiom( ontt, eqAxiom );
    }

    def addDefinitionUsingCodeValue (OWLNamedIndividual codeInd, OWLClass codeClass) {

        Set<OWLLiteral> codeValues = codeInd.getDataPropertyValues( icd9Prop, onta );
//        println codeValues.size();
        if (codeValues.isEmpty()) {
            println "no icd9 code: " + codeInd;
            return;
        }
        OWLLiteral litValue = codeValues.iterator().next();
        assert litValue;
//        println "icd9 code = " + litValue;
//        OWLLiteral litValue = odf.getOWLLiteral( codeValue );
        OWLDataHasValue hasCodeValue = odf.getOWLDataHasValue( icd9Prop, litValue );
        OWLObjectSomeValuesFrom some = odf.getOWLObjectSomeValuesFrom( refinesProp, hasCodeValue );

        OWLObjectIntersectionOf codeConceptAndValue = odf.getOWLObjectIntersectionOf( some,
                topCodeClass );
        OWLSubClassOfAxiom eqAxiom = odf.getOWLSubClassOfAxiom( codeConceptAndValue, codeClass );
        assert eqAxiom;
        oom.addAxiom( ontt, eqAxiom );
    }

    String localName (String s) {

        Pattern pat
        pat = ~/[^a-zA-Z0-9_]/;
        s.replaceAll( pat ) { "_" }
    }

    def setUpOntologyFormat () {

        OWLOntologyFormat oFormat = new OWLFunctionalSyntaxOntologyFormat();
        oFormat.copyPrefixesFrom( pm );
        oom.setOntologyFormat( ontt, oFormat );
    }

    def serialize () {

        oom.saveOntology( ontt, sharpCodesDocIRI );
    }

}
