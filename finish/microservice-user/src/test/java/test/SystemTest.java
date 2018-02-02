package test;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SystemTest {

    @Test
    public void testGetProperties() throws Exception {
        // tag::systemProperties[]
        String baseUrl =
            "https://"
                + System.getProperty("liberty.test.hostname")
                + ":"
                + System.getProperty("liberty.test.ssl.port");
        // end::systemProperties[]

        //  Add user.
        String loginAuthHeader =
            "Bearer "
                + new JWTVerifier()
                    .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
        User user1 = new User("01", "Isaac", "Newton", "inewton", "inewtonWishListLink", "mypassword");
        String url = baseUrl + "/users/";
        Response response = processRequest(url, "POST", user1.getJson(), loginAuthHeader);
        assertEquals(
            "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
            Status.OK.getStatusCode(),
            response.getStatus());

        // Retrieving JWT from POST response
        String authHeader = response.getHeaderString("Authorization");
        new JWTVerifier().validateJWT(authHeader);

        // Get system properties by using JWT token
        String propUrl = baseUrl + "/system/properties";
        Response propResponse = processRequest(propUrl, "GET", null, authHeader);

        assertEquals(
            "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
            Status.OK.getStatusCode(),
            propResponse.getStatus());

        JsonObject responseJson = toJsonObj(propResponse.readEntity(String.class));

        assertEquals("The system property for the local and remote JVM should match",
                     System.getProperty("os.name"),
                     responseJson.getString("os.name"));
        System.out.println(responseJson.getString("os.name"));
        System.out.println(propUrl);

    }


    @Test
    public void testCreateUserInvalidJWT() throws Exception {
        // tag::systemProperties[]
        String baseUrl =
            "https://"
                + System.getProperty("liberty.test.hostname")
                + ":"
                + System.getProperty("liberty.test.ssl.port");
        // end::systemProperties[]

    // Add user.  Use a JWT that is not in the login group.
      String loginAuthHeader =
          "Bearer "
              + new JWTVerifier()
                  .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("users")));
      User user1 = new User("02", "Isaac", "Newton", "inewton", "inewtonWishListLink",
  "mypassword");
      String url = baseUrl + "/users/";

      // This request should fail since the JWT is in the wrong group.
      Response response = processRequest(url, "POST", user1.getJson(), loginAuthHeader);
      assertEquals(
          "HTTP response code should have been " + Status.UNAUTHORIZED.getStatusCode() + ".",
          Status.UNAUTHORIZED.getStatusCode(),
          response.getStatus());


    }

    @Test
    public void testGetPropertiesWithJWT() throws Exception {
        // tag::systemProperties[]
        String baseUrl =
            "https://"
                + System.getProperty("liberty.test.hostname")
                + ":"
                + System.getProperty("liberty.test.ssl.port");
        // end::systemProperties[]

      String authHeader =
          "Bearer "
              + new JWTVerifier()
                  .createJWT("Newton");

      // Get system properties by using JWT token
      String propUrl = baseUrl + "/system/properties";
      Response propResponse = processRequest(propUrl, "GET", null, authHeader);

      assertEquals(
          "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
          Status.OK.getStatusCode(),
          propResponse.getStatus());

      JsonObject responseJson = toJsonObj(propResponse.readEntity(String.class));

      assertEquals("The system property for the local and remote JVM should match",
                   System.getProperty("os.name"),
                   responseJson.getString("os.name"));
      System.out.println(responseJson.getString("os.name"));
      System.out.println(propUrl);


    }

    public Response processRequest(String url, String method, String payload, String authHeader) {
      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(url);
      Builder builder = target.request();
      builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      if (authHeader != null) {
        builder.header(HttpHeaders.AUTHORIZATION, authHeader);
      }
      return (payload != null)
          ? builder.build(method, Entity.json(payload)).invoke()
          : builder.build(method).invoke();
    }

    public JsonObject toJsonObj(String json) {
        JsonReader jReader = Json.createReader(new StringReader(json));
          return jReader.readObject();
    }

}
