package edu.asu.sharpc2b.hed.impl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;
import org.drools.io.Resource;
import org.drools.io.impl.ClassPathResource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DomainHierarchyExplorer {

    private List<String> vmrRoots = Arrays.asList( "VMR", "EntityBase", "ClinicalStatement", "ClinicalStatementRelationship" );

    private static ConcurrentHashMap<String, DomainHierarchyExplorer> domainExplorers = new ConcurrentHashMap<String, DomainHierarchyExplorer>(  );

    private String domainNs;

    private String domainHierarchy;

    private Map<String,String> domKlasses = new HashMap<String,String>();
    private Map<String,Map<String,String>> domProptis = new HashMap<String,Map<String,String>>();


    public static DomainHierarchyExplorer getInstance( String domainModelPath, String domainNs ) {
        if ( ! domainExplorers.containsKey( domainModelPath ) ) {
            DomainHierarchyExplorer explorer = new DomainHierarchyExplorer( domainModelPath, domainNs );
            domainExplorers.put( domainModelPath, explorer );
        }
        return domainExplorers.get( domainModelPath );
    }


    public DomainHierarchyExplorer( String domainModelPath, String domainNs ) {
        this.domainNs = domainNs;

        Resource domainRes = new ClassPathResource( domainModelPath );
        OWLOntology onto = getDomainOntology( domainRes );

        explore( onto );

    }




    protected String explore( OWLOntology onto )  {
        IRI subKlassOf = IRI.create( "http://asu.edu/sharpc2b/skos-ext#subConceptOf" );
        IRI partOf = IRI.create( "http://asu.edu/sharpc2b/skos-ext#partOf" );

        Map<String,Node> frame = new HashMap<>();
        //TODO Works with vMR only, fix when the vMR ontology generation has been improved
        Node root = initRoot( frame, "Thing", vmrRoots );

        for ( OWLObjectPropertyAssertionAxiom opa : onto.getAxioms( AxiomType.OBJECT_PROPERTY_ASSERTION, false ) ) {
            if ( subKlassOf.equals( opa.getProperty().asOWLObjectProperty().getIRI() ) ) {
                IRI sup = IRI.create( domainNs, opa.getObject().asOWLNamedIndividual().getIRI().getFragment() );
                IRI sub = IRI.create( domainNs, opa.getSubject().asOWLNamedIndividual().getIRI().getFragment() );

                if ( sub.equals( sup ) ) {
                    continue;
                }

                Node supNode = getNode( frame, sup.getFragment() );
                Node subNode = getNode( frame, sub.getFragment() );
                supNode.addChild( subNode );

            }
        }

        traverseKlasses( root );

        for ( OWLObjectPropertyAssertionAxiom opa : onto.getAxioms( AxiomType.OBJECT_PROPERTY_ASSERTION, false ) ) {
            if ( partOf.equals( opa.getProperty().asOWLObjectProperty().getIRI() ) ) {
                IRI prop = IRI.create( domainNs, opa.getSubject().asOWLNamedIndividual().getIRI().getFragment() );
                IRI klass = IRI.create( domainNs, opa.getObject().asOWLNamedIndividual().getIRI().getFragment() );

                if ( domKlasses.containsKey( klass.toString() ) ) {
                    Map<String, String> propsForKlass = domProptis.get( klass.toString() );
                    if ( propsForKlass == null ) {
                        propsForKlass = new HashMap<String,String>();
                        domProptis.put( klass.toString(), propsForKlass );
                    }
                    propsForKlass.put( prop.toString(), prop.getFragment() );
                }
            }
        }

        traverseProperties( root );

        XStream xstream = new XStream(new JsonHierarchicalStreamDriver() {
            public HierarchicalStreamWriter createWriter(Writer writer) {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE );
            }
        });

        domainHierarchy = xstream.toXML( root );
        return domainHierarchy;
    }

    private OWLOntology getDomainOntology( Resource domainRes ) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            ClassPathResource skos = new ClassPathResource( "ontologies/editor_models/skos-core.owl" );
            ClassPathResource skos_ext = new ClassPathResource( "ontologies/editor_models/skos-ext.owl" );
            ClassPathResource ops = new ClassPathResource( "ontologies/editor_models/expr-core.owl" );


            manager.loadOntologyFromOntologyDocument( skos.getInputStream() );
            manager.loadOntologyFromOntologyDocument( skos_ext.getInputStream() );
            manager.loadOntologyFromOntologyDocument( ops.getInputStream() );
            OWLOntology domain = manager.loadOntologyFromOntologyDocument( domainRes.getInputStream() );

            return domain;
        } catch ( Exception e ) {
            e.printStackTrace();
            try {
                return manager.createOntology();
            } catch ( OWLOntologyCreationException e1 ) {
                return null;
            }
        }
    }


    public String getDomainHierarchy() {
        return domainHierarchy;
    }

    public void setDomainHierarchy( String domainHierarchy ) {
        this.domainHierarchy = domainHierarchy;
    }

    public Map<String, String> getDomKlasses() {
        return domKlasses;
    }

    public void setDomKlasses( Map<String, String> domKlasses ) {
        this.domKlasses = domKlasses;
    }

    public Map<String, Map<String, String>> getDomProptis() {
        return domProptis;
    }

    public void setDomProptis( Map<String, Map<String, String>> domProptis ) {
        this.domProptis = domProptis;
    }

    private void traverseKlasses( Node root ) {
        for ( Node child : root.children ) {
            visitDomainKlass( child );
        }
    }

    private void visitDomainKlass( Node node ) {
        domKlasses.put( IRI.create( domainNs + node.name ).toString(), node.name );
        if ( node.getChildren() != null ) {
            for ( Node n : node.getChildren() ) {
                visitDomainKlass( n );
            }
        }
    }

    private void traverseProperties( Node root ) {
        for ( Node child : root.children ) {
            visitDomainProperties( child );
        }
    }

    private void visitDomainProperties( Node node ) {
        if ( node.getChildren() != null ) {
            IRI parent = IRI.create( domainNs + node.getName() );
            Map<String,String> parentProperties = domProptis.get( parent.toString() );
            for ( Node n : node.getChildren() ) {
                IRI child = IRI.create( domainNs + n.getName() );
                Map<String,String> childProperties = domProptis.get( child.toString() );
                if ( childProperties == null ) {
                    childProperties = new HashMap<String, String>();
                    domProptis.put( child.toString(), childProperties );
                }
                childProperties.putAll( parentProperties );

                visitDomainProperties( n );
            }
        }
    }


    private static Node initRoot( Map<String, Node> frame, String top, List<String> roots ) {
        Node root = getNode( frame, top );
        for ( String base : roots ) {
            Node sub = getNode( frame, base );
            root.addChild( sub );
        }
        return root;
    }

    private static Node getNode( Map<String, Node> frame, String key ) {
        if ( frame.containsKey( key ) ) {
            return frame.get( key );
        } else {
            Node n = new Node( key );
            frame.put( key, n );
            return n;
        }
    }



    public static class Node {
        private String name;
        private List<Node> children;
        private Integer size = 1;

        public Node( String name ) {
            this.name = name;
        }

        public void addChild( Node n ) {
            if ( this.children == null ) {
                this.children = new ArrayList( 3 );
            }
            this.children.add( n );
        }

        public String getName() {
            return name;
        }

        public void setName( String name ) {
            this.name = name;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void setChildren( List<Node> children ) {
            this.children = children;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize( Integer size ) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "Node{" +
                   "children=" + children +
                   ", name='" + name + '\'' +
                   '}';
        }
    }
}