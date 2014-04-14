package edu.asu.sharpc2b.hed.impl;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import edu.asu.sharpc2b.ops.BooleanExpression;
import edu.asu.sharpc2b.ops.ClinicalRequestExpression;
import edu.asu.sharpc2b.ops.DomainClassExpression;
import edu.asu.sharpc2b.ops.DomainPropertyExpression;
import edu.asu.sharpc2b.ops.IteratorExpression;
import edu.asu.sharpc2b.ops.OperatorExpression;
import edu.asu.sharpc2b.ops.PropertyExpression;
import edu.asu.sharpc2b.ops.PropertySetExpression;
import edu.asu.sharpc2b.ops.SharpExpression;
import edu.asu.sharpc2b.ops.VariableExpression;
import edu.asu.sharpc2b.ops_set.AndExpression;
import edu.asu.sharpc2b.ops_set.IsEmptyExpression;
import edu.asu.sharpc2b.ops_set.IsNotEmptyExpression;
import edu.asu.sharpc2b.ops_set.NotExpression;
import edu.asu.sharpc2b.ops_set.OrExpression;
import edu.asu.sharpc2b.prr.NamedElement;
import edu.asu.sharpc2b.prr.RuleVariable;
import edu.asu.sharpc2b.prr.TypedElement;
import org.hl7.knowledgeartifact.r1.ExpressionRef;
import org.w3._2002._07.owl.Thing;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BlocklyFactory {

    private static final String exprNS = "http://asu.edu/sharpc2b/ops-set#";
    private Map<String,String> requiredDomainClasses;

    private Map<String,String> domainClasses;
    private Map<String, Map<String, String>> domainProperties;


    public static enum ExpressionRootType {
        TRIGGER,
        CONDITION,
        EXPRESSION,
        ACTION
    }


    public BlocklyFactory( Map<String, String> domainClasses, Map<String, Map<String, String>> domainProperties ) {
        this.domainClasses = Collections.unmodifiableMap( domainClasses );
        this.domainProperties = Collections.unmodifiableMap( domainProperties );
        this.requiredDomainClasses = new HashMap<String,String>();
    }

    public Map<? extends String, ? extends String> getRequiredDomainClasses() {
        return requiredDomainClasses;
    }


    public byte[] fromExpression( String name, SharpExpression sharpExpression, ExpressionRootType type ) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            Document dox = builderFactory.newDocumentBuilder().newDocument();

            visitRoot( name, sharpExpression, dox, type );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            dump( dox, baos );
            return baos.toByteArray();
        } catch ( ParserConfigurationException e ) {
            e.printStackTrace();
            return new byte[ 0 ];
        }
    }

    private Element visitRoot( String name, SharpExpression sharpExpression, Document dox, ExpressionRootType type ) {
        Element root = dox.createElement( "xml" );

        Element exprRoot;
        switch ( type ) {
            case CONDITION:
                exprRoot = createLogicRoot( name, root, dox );
                visitCondition( sharpExpression, null, exprRoot, dox );
                break;
            case EXPRESSION:
            default:
                exprRoot = createExpressionRoot( name, root, dox );
                visit( sharpExpression, exprRoot, dox );
                break;
        }

        dox.appendChild( root );
        return root;
    }

    private Element createExpressionRoot( String exprName, Element root, Document dox ) {
        Element block = dox.createElement( "block" );
        block.setAttribute( "type", "logic_root" );
        block.setAttribute( "inline", "false" );
        block.setAttribute( "deletable", "false" );
        block.setAttribute( "movable", "false" );
        block.setAttribute( "x", "0" );
        block.setAttribute( "y", "0" );

        Element eName = dox.createElement( "field" );
        eName.setAttribute( "name", "NAME" );
        eName.setTextContent( exprName );
        block.appendChild( eName );

        Element value = dox.createElement( "value" );
        value.setAttribute( "name", "NAME" );
        block.appendChild( value );

        root.appendChild( block );
        return value;
    }

    private Element createLogicRoot( String exprName, Element root, Document dox ) {
        Element block = dox.createElement( "block" );
        block.setAttribute( "type", "logic_root" );
        block.setAttribute( "inline", "false" );
        block.setAttribute( "deletable", "false" );
        block.setAttribute( "movable", "false" );
        block.setAttribute( "x", "0" );
        block.setAttribute( "y", "0" );

        Element value = dox.createElement( "value" );
        value.setAttribute( "name", "ROOT" );
        block.appendChild( value );

        root.appendChild( block );
        return value;
    }


    private void visit( SharpExpression sharpExpression, Element parent, Document dox ) {
        if ( sharpExpression instanceof IteratorExpression ) {
            IteratorExpression iter = (IteratorExpression) sharpExpression;
            sharpExpression = iter.getSource().get( 0 );
        }

        visitExpression( null, sharpExpression, parent, "block", dox );
    }




    private void fillProperties( Element elem, OperatorExpression expr, Document dox ) {
        try {
            for ( Method m : expr.getClass().getMethods() ) {
                if ( m.getAnnotation( RdfProperty.class ) != null ) {
                    String propIri = m.getAnnotation( RdfProperty.class ).value();
                    if ( "http://asu.edu/sharpc2b/ops#opCode".equals( propIri ) ) {
                        continue;
                    }

                    if ( Collection.class.isAssignableFrom( m.getReturnType() ) ) {
                        Collection coll = (Collection) m.invoke( expr );
                        int j = 0;
                        for ( Object o : coll ) {
                            fillArgument( elem, propIri, o, dox, coll.size(), j++ );
                        }
                    } else {
                        Object o = m.invoke( expr );
                        if ( o != null ) {
                            fillArgument( elem, propIri, o, dox, 1, 0 );
                        }
                    }
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private void fillArgument( Element elem, String propIri, Object o, Document dox, int numRepeats, int currentItem ) {
        if ( o instanceof Thing ) {
            String oid = ((Thing) o).getRdfId().value().toString();
            if ( o instanceof SharpExpression ) {

                if ( isMultiple( propIri ) && currentItem == 0 ) {
                    String type = elem.getAttribute( "type" );
                    type = type.substring( type.indexOf( "#" ) + 1 );
                    type = type.replaceFirst( "Expression", "" ).toLowerCase();
                    Element mutate = dox.createElement( "mutation" );
                    mutate.setAttribute( "items", "" + numRepeats );
                    elem.appendChild( mutate );
                }


                Element value = dox.createElement( "value" );
                value.setAttribute( "name",
                                    propIri
                                    + ( ( isMultiple( propIri ) ) ? ("_" + currentItem) : "" )
                );
                visitExpression( null, (SharpExpression) o, value, "block", dox );
                elem.appendChild( value );
            }
        } else {
            String literalValue = o.toString();
            String literalType = mapLiteral( o );
            if ( "xsd:boolean".equals( literalType ) ) {
                literalValue = literalValue.toUpperCase();
            }

            Element value = dox.createElement( "value" );
            value.setAttribute( "name", propIri );

            Element block = dox.createElement( "block" );
            block.setAttribute( "type", literalType );

            Element field = dox.createElement( "field" );
            field.setAttribute( "name", "VALUE" );

            field.setTextContent( literalValue );

            block.appendChild( field );
            value.appendChild( block );
            elem.appendChild( value );

        }
    }

    private String mapLiteral( Object o ) {
        if ( o instanceof String ) {
            return "xsd:string";
        } else if ( o instanceof Boolean ) {
            return "xsd:boolean";
        } else if ( o instanceof Integer ) {
            return "xsd:int";
        } else if ( o instanceof Double ) {
            return "xsd:double";
        } else {
            throw new UnsupportedOperationException( "Unable to deal with DR " + o.getClass() );
        }
    }


    private boolean isMultiple( String predicate ) {
        //TODO
        return predicate.equals( exprNS + "property" )
               || predicate.equals( exprNS + "element" )
               || predicate.equals( exprNS + "caseItem" )
               || predicate.equals( exprNS.replace( "ops-set", "ops" ) + "property" );
    }

    private void traversePropChain( PropertyExpression pro, Element parent, Document dox ) {
        List<String> propChain = new ArrayList<String>(  );
        do {
            String fqn = pro.getPropCode().get( 0 ).getCode().get( 0 ).toString();
            fqn = fqn.substring( fqn.indexOf( '#' ) + 1 );
            fqn = fqn.substring( fqn.indexOf( ':' ) + 1 );

            if ( pro.getSource() != null && ! pro.getSource().isEmpty() ) {
                pro = pro.getSource().get( 0 ) instanceof DomainPropertyExpression ? (DomainPropertyExpression) pro.getSource().get( 0 ) : null;
            } else {
                pro = null;
            }
            propChain.add( 0, fqn );
        } while ( pro != null );

        for ( String prop : propChain ) {
            List<String> candidateClasses = searchForDomains( prop );
            Element block = dox.createElement( "block" );
            if ( candidateClasses.size() == 1 ) {
                block.setAttribute( "type", candidateClasses.get( 0 ) + "/properties" );
            } else {
                block.setAttribute( "type", "http://asu.edu/sharpc2b/ops#DomainProperty" );
            }
            block.setAttribute( "inline", "false" );

            Element field = dox.createElement( "field" );
            field.setAttribute( "name", "DomainProperty" );
            field.setTextContent( "urn:hl7-org:vmr:r2#" + prop );
            block.appendChild( field );

            Element val = dox.createElement( "value" );
            val.setAttribute( "name", "DOT" );
            block.appendChild( val );

            parent.appendChild( block );
            parent = val;
        }
    }

    private List<String> searchForDomains( String prop ) {
        List<String> candidates = new ArrayList<String>( );
        //TODO needs improvement!
        for ( String klass : domainClasses.keySet() ) {
            if ( klass.contains( "Base" ) ) {
                continue;
            }
            Map<String, String> properties = domainProperties.get( klass );
            for ( String fpn : properties.keySet() ) {
                String pName = properties.get( fpn );
                if ( pName.equals( prop ) ) {
                    if ( this.requiredDomainClasses.containsKey( klass ) ) {
                        return Arrays.asList( domainClasses.get( klass ) );
                    }
                    candidates.add( domainClasses.get( klass ) );
                }
            }
        }
        return candidates;
    }


    private void visitExpression( TypedElement var, SharpExpression expr, Element parent, String tagName, Document dox ) {

        Class actualClass = mapClass( expr.getClass() );
        String type = extractType( actualClass );

        Element x = dox.createElement( tagName );
        x.setAttribute( "type", type );
        x.setAttribute( "inline", "false" );

        if ( expr instanceof VariableExpression ) {
            List vars = ((VariableExpression) expr ).getReferredVariable();
            String varName = ( (NamedElement) vars.get( 0 ) ).getName().get( 0 );
            Element field = dox.createElement( "field" );
            field.setAttribute( "name", "Variables" );
            field.setTextContent( varName );
            x.appendChild( field );
            parent.appendChild( x );
            return;
        } else if ( expr instanceof DomainClassExpression ) {
            Element klass = dox.createElement( tagName );
            String klassName = processKlassName( expr.getHasCode().get( 0 ).getCode().get( 0 ) );
            klass.setAttribute( "type", klassName );
            parent.appendChild( klass );
            return;
        } else if ( expr instanceof DomainPropertyExpression ) {
            traversePropChain( (PropertyExpression) expr, parent, dox );
            return;
        } else if ( expr instanceof PropertySetExpression ) {
            PropertySetExpression setter = (PropertySetExpression) expr;

            Element val = dox.createElement( "value" );
            val.setAttribute( "inline", "false" );
            val.setAttribute( "name", "http://asu.edu/sharpc2b/ops#value" );
            visitExpression( var, setter.getValue().get( 0 ), val, "block", dox );
            x.appendChild( val );

            Element prop = dox.createElement( "value" );
            prop.setAttribute( "inline", "false" );
            prop.setAttribute( "name", "http://asu.edu/sharpc2b/ops#property" );
            visitExpression( var, setter.getProperty().get( 0 ), prop, "block", dox );
            x.appendChild( prop );

            parent.appendChild( x );
            return;
        }

        if ( expr instanceof OperatorExpression ) {
            try {
                fillProperties( x, (OperatorExpression) expr, dox );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        parent.appendChild( x );
    }


    private String processKlassName( String klassName ) {
        if ( klassName.startsWith( "{" ) ) {
            String namespace = klassName.substring( 1, klassName.indexOf( "}" ) );
            String name = klassName.substring( klassName.indexOf( "}" ) + 1 );
            this.requiredDomainClasses.put( namespace + "#" + name, name );
            return name;
        } else if ( klassName.contains( "#" ) ) {
            String name = klassName.substring( klassName.indexOf( "#" ) + 1  );
            this.requiredDomainClasses.put( klassName, name );
            return name;
        } else {
            for ( String fqn : domainClasses.keySet() ) {
                String simpleName = domainClasses.get( fqn );
                if ( simpleName.equals( klassName ) ) {
                    this.requiredDomainClasses.put( fqn, simpleName );
                    return simpleName;
                }
            }
        }
        return klassName;
    }


    private String extractType( Class actualClass ) {
        RdfsClass klass = (RdfsClass) actualClass.getAnnotation( RdfsClass.class );
        return klass.value();
    }

    private Class mapClass( Class<?> aClass ) {
        if ( IteratorExpression.class.isAssignableFrom( aClass ) ) {
            return ClinicalRequestExpression.class;
        } else if (ExpressionRef.class.isAssignableFrom( aClass ) ) {
            return VariableExpression.class;
        } else {
            return aClass;
        }
    }






    private void visitCondition( SharpExpression sharpExpression, SharpExpression parent, Element exprRoot, Document dox ) {
        if ( sharpExpression instanceof AndExpression ) {
            AndExpression and = (AndExpression) sharpExpression;

            Element block = dox.createElement( "block" );
            block.setAttribute( "inline", "false" );
            block.setAttribute( "type", "logic_compare" );

            Element arity = dox.createElement( "mutation" );
            arity.setAttribute( "types", "" + and.getHasOperand().size() );
            block.appendChild( arity );

            Element name = dox.createElement( "field" );
            name.setAttribute( "name", "NAME" );
            block.appendChild( name );
            Element drop = dox.createElement( "field" );
            drop.setAttribute( "name", "dropdown" );
            drop.setTextContent( "ALL" );
            block.appendChild( drop );

            int j = 0;
            for ( SharpExpression operand : and.getHasOperand() ) {
                Element clause = dox.createElement( "value" );
                clause.setAttribute( "name", "CLAUSE" + (j++) );
                visitCondition( operand, and, clause, dox );
                block.appendChild( clause );
            }

            exprRoot.appendChild( block );
        } else if ( sharpExpression instanceof OrExpression ) {
            OrExpression or = (OrExpression) sharpExpression;

            Element block = dox.createElement( "block" );
            block.setAttribute( "inline", "false" );
            block.setAttribute( "type", "logic_compare" );

            Element arity = dox.createElement( "mutation" );
            arity.setAttribute( "types", "" + or.getHasOperand().size() );
            block.appendChild( arity );

            Element name = dox.createElement( "field" );
            name.setAttribute( "name", "NAME" );
            block.appendChild( name );
            Element drop = dox.createElement( "field" );
            drop.setAttribute( "name", "dropdown" );
            drop.setTextContent( "ONEPLUS" );
            block.appendChild( drop );

            int j = 0;
            for ( SharpExpression operand : or.getHasOperand() ) {
                Element clause = dox.createElement( "value" );
                clause.setAttribute( "name", "CLAUSE" + (j++) );
                visitCondition( operand, or, clause, dox );
                block.appendChild( clause );
            }

            exprRoot.appendChild( block );
        } else if ( sharpExpression instanceof NotExpression ) {
            NotExpression not = (NotExpression) sharpExpression;

            Element block = dox.createElement( "block" );
            block.setAttribute( "inline", "false" );
            block.setAttribute( "type", "logic_negate" );

            if ( ! not.getFirstOperand().isEmpty() ) {
                Element clause = dox.createElement( "value" );
                clause.setAttribute( "name", "NOT" );
                visitCondition( not.getFirstOperand().get( 0 ), not, clause, dox );
                block.appendChild( clause );
            }

            exprRoot.appendChild( block );
   } else if ( sharpExpression instanceof VariableExpression ) {
            VariableExpression variable = (VariableExpression) sharpExpression;
            RuleVariable ruleVariable = (RuleVariable) variable.getReferredVariable().get( 0 );

            Element block = dox.createElement( "block" );
            block.setAttribute( "inline", "false" );
            block.setAttribute( "type", isParentBoolean( parent ) ? "logic_boolean" : "logic_any" );

            Element var = dox.createElement( "field" );
            var.setAttribute( "name", "Clauses" );
            var.setTextContent( HeDArtifactData.idFromName( ruleVariable.getName().get( 0 ) ) );
            block.appendChild( var );

            exprRoot.appendChild( block );
        } else if ( sharpExpression instanceof IsEmptyExpression ) {
            IsEmptyExpression nexists = (IsEmptyExpression) sharpExpression;

            Element block = dox.createElement( "block" );
            block.setAttribute( "inline", "false" );
            block.setAttribute( "type", "logic_negate" );

            Element not = dox.createElement( "value" );
            not.setAttribute( "name", "NOT" );
            block.appendChild( not );

            Element exists = dox.createElement( "block" );
            exists.setAttribute( "inline", "false" );
            exists.setAttribute( "type", "logic_exists" );
            not.appendChild( exists );

            if ( ! nexists.getFirstOperand().isEmpty() ) {
                Element clause = dox.createElement( "value" );
                clause.setAttribute( "name", "EXISTS" );
                visitCondition( nexists.getFirstOperand().get( 0 ), nexists, clause, dox );
                exists.appendChild( clause );
            }

            exprRoot.appendChild( block );

        } else if ( sharpExpression instanceof IsNotEmptyExpression ) {
            IsNotEmptyExpression exists = (IsNotEmptyExpression) sharpExpression;

            Element block = dox.createElement( "block" );
            block.setAttribute( "inline", "false" );
            block.setAttribute( "type", "logic_exists" );

            if ( ! exists.getFirstOperand().isEmpty() ) {
                Element clause = dox.createElement( "value" );
                clause.setAttribute( "name", "EXISTS" );
                visitCondition( exists.getFirstOperand().get( 0 ), exists, clause, dox );
                block.appendChild( clause );
            }

            exprRoot.appendChild( block );
        } else {
            throw new UnsupportedOperationException( "Logic expression not supported at the UI level " + sharpExpression.getClass() );
        }
    }

    private boolean isParentBoolean( SharpExpression parent ) {
        return parent != null
               && ! ( parent instanceof IsNotEmptyExpression )
               && ! ( parent instanceof IsEmptyExpression );
    }


    protected void dump( Document dox, OutputStream stream ) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");
            StreamResult result = new StreamResult( stream );
            DOMSource source = new DOMSource( dox );
            transformer.transform( source, result );
        } catch ( TransformerException e ) {
            e.printStackTrace();
        }
    }
}

