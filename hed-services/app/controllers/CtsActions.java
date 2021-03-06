package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.cts2.framework.core.client.Cts2RestClient;
import edu.mayo.cts2.framework.core.json.JsonConverter;
import edu.mayo.cts2.framework.core.xml.Cts2Marshaller;
import edu.mayo.cts2.framework.core.xml.DelegatingMarshaller;
import edu.mayo.cts2.framework.model.codesystem.CodeSystemCatalogEntryDirectory;
import edu.mayo.cts2.framework.model.valueset.ValueSetCatalogEntryDirectory;
import models.ModelHome;
import play.api.libs.json.JsPath;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static controllers.SharpController.setHeaderCORS;

//import edu.mayo.*;

/**
 * User: rk Date: 8/19/13 Package: controllers
 */
public class CtsActions
        extends play.mvc.Controller
{

//    static String ctsURL = "http://informatics.mayo.edu/cts2/rest/";
    static String ctsURL = "http://localhost:8080/cts2framework/";

    public static Result helloWorld ()
    {
        Cts2Marshaller marshaller = new DelegatingMarshaller();
        Cts2RestClient client = new Cts2RestClient( marshaller, true );
        ValueSetCatalogEntryDirectory result = client
                .getCts2Resource( "codesystems",
                                  ValueSetCatalogEntryDirectory.class );

        System.out.println( result );

        setHeaderCORS();
        return ok( result.toString() );
    }

    public static Result searchInCodeSystem (final String cs,
                                             final String matchvalue)
            throws IOException
    {
//        System.out.println( "\n" + "Unmarshalling and printing from json" + "\n" );

        String uri = ctsURL + "codesystem/" + cs + "/resolution" +
                "?" + "matchvalue=" + matchvalue +
                "&format=json";
        URL url;
        HttpURLConnection connection;
        url = new URL( uri );
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty( "Accept", "text/json" );
        if (connection.getResponseCode() != 200)
        {
            throw new RuntimeException(
                    "Failed : The HTTP error code is : " + connection.getResponseCode() );
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader( (connection.getInputStream()) ) );
        String output;
        StringBuilder builder = new StringBuilder();
        while ((output = br.readLine()) != null)
        {
            builder.append( output );
        }
        JsonConverter converter = new JsonConverter();
        ValueSetCatalogEntryDirectory valuesetcat = converter
                .fromJson( builder.toString(), ValueSetCatalogEntryDirectory.class );

        String jsonText = converter.toJson( valuesetcat );
        setHeaderCORS();
        return ok( jsonText );
    }

    public static Result getCodeSystems() {
        JsonConverter converter = new JsonConverter();
        List<String> codeSystems = ModelHome.getCodeSystems();

        JsonNode jsonOut = Json.toJson( codeSystems );
        System.out.println( "Sending " + jsonOut );
        setHeaderCORS();
        return ok( jsonOut );
    }


    public static Result unMarshallandPrintFromJson ()
            throws IOException
    {
//        System.out.println( "\nUnmarshalling and printing from json\n" );
        String uri = ctsURL + "codesystems?matchvalue=Sequence&format=json";
        URL url;
        HttpURLConnection connection;
        url = new URL( uri );
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty( "Accept", "text/json" );
        if (connection.getResponseCode() != 200)
        {
            throw new RuntimeException(
                    "Failed : The HTTP error code is : " + connection.getResponseCode() );
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader( (connection.getInputStream()) ) );
        String output;
        StringBuilder builder = new StringBuilder();
        while ((output = br.readLine()) != null)
        {
            builder.append( output );
        }
        JsonConverter converter = new JsonConverter();
        CodeSystemCatalogEntryDirectory valuesetcat = converter
                .fromJson( builder.toString(), CodeSystemCatalogEntryDirectory.class );



        String jsonText = converter.toJson( valuesetcat );
        setHeaderCORS();
        return ok( jsonText );
    }

    private static Result getRestFromHref (String uri)
            throws IOException
    {
        URL url;
        uri = uri.concat( "/resolution" );
        if (!uri.endsWith( "?format=json" ))
        {
            uri = uri.concat( "?format=json" );
        }
        HttpURLConnection connection;
        url = new URL( uri );
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty( "Accept", "text/json" );
        if (connection.getResponseCode() != 200)
        {
            throw new RuntimeException(
                    "Failed : The HTTP error code is : " + connection.getResponseCode() );
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader( (connection.getInputStream()) ) );
        String output;
        StringBuilder buffer = new StringBuilder();
        while ((output = br.readLine()) != null)
        {
            buffer.append( output );
        }
        JsonConverter converter = new JsonConverter();
        StringBuilder builder = new StringBuilder();
        ValueSetCatalogEntryDirectory valuesetcat = converter
                .fromJson( builder.toString(), ValueSetCatalogEntryDirectory.class );

        String jsonText = converter.toJson( valuesetcat );
        setHeaderCORS();
        return ok( jsonText );
    }

    public static Result forwardToCts (String path)
            throws IOException
    {
        System.out.println( "forwardToCts: path = " + path );

        String formatIn = request().getQueryString( "format" );

        final String base = ctsURL;
        URL url;
        String queryString = createJsonQueryString( request().queryString() );

        String fwdPath = path + queryString;

        HttpURLConnection connection;
        url = new URL( base + fwdPath );

        System.out.println( "forwardToCts: cts2 URL = " + url );
        System.out.println( "forwardToCts: cts2 URL.query = " + url.getQuery() );

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty( "Accept", "text/json" );
        if ( connection.getResponseCode() != 200 ) {
            throw new RuntimeException(
                    "Failed : The HTTP error code is : " + connection.getResponseCode() );
        }

        InputStream response = connection.getInputStream();
        byte[] data = new byte[ response.available() ];
        response.read( data );

        setHeaderCORS();
        return ok( data );
    }

    private static String createJsonQueryString (final Map<String, String[]> queries)
    {
        final StringBuilder sb = new StringBuilder();

        boolean empty = true;
        boolean hasFormatJson = false;

        for (Map.Entry<String, String[]> entry : queries.entrySet())
        {
            for (String value : entry.getValue())
            {
                sb.append( empty ? "?" : "&" );
                sb.append( entry.getKey() + "=" + value.replace( " ", "%20" ) );

                if ("format".equals( entry.getKey() ) && "json".equals( value )) {
                    hasFormatJson = true;
                }
                empty = false;
            }
        }
        if (!hasFormatJson)
        {
            sb.append( (empty ? "?" : "&") + "format=json" );
        }
        return sb.toString();
    }

}
