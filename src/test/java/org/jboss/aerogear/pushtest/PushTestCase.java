package org.jboss.aerogear.pushtest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.android.AndroidDriver;

import java.io.File;
import java.net.URL;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(Arquillian.class)
@RunAsClient
public class PushTestCase {



    String pushApplicationId;
    String variantId;

    @Deployment(name = "android", testable = false)
    @TargetsContainer("android")
    public static Archive<?> createAndroidDeployment() {
        return Maven.resolver().resolve("org.jboss.aerogear.pushtest:aerogear-test-android:apk:1.0-SNAPSHOT").withoutTransitivity().asSingle(JavaArchive.class);
    }

    @Deployment(name = "jbossas")
    @TargetsContainer("jbossas")
    public static Archive<?> createJbossasDeployment() {
        return Maven.resolver().resolve("org.aerogear.connectivity:pushee:war:0.1.0").withoutTransitivity().asSingle(WebArchive.class);
    }

    //@BeforeClass
    public void setup(URL url) {
        String response;
        response = given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body("{\"name\" : \"AutomatedTestApp\", \"description\" : \"android-test-app\"}")
            .expect()
                .statusCode(201).body("id", is(not(nullValue()))) // TODO this is not enough! (and should be 201)
            .when()
                .post(url.toString() + "rest/applications").asString();

        pushApplicationId = JsonPath.from(response).getString("pushApplicationID");

        response = given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body("{\"googleKey\" : \"AIzaSyC0GBQqv2RVYdo2wONCT-IcNlufHt253VI\"}")
            .expect()
                .statusCode(201).body("id", is(not(nullValue())))
            .when()
                .post(url.toString() + "rest/applications/" + pushApplicationId + "/android").asString();

        variantId = JsonPath.from(response).getString("variantID");

    }

    @Test
    @OperateOnDeployment("android")
    public void sendTestNotification(@Drone AndroidDriver driver, @ArquillianResource URL url) {
        setup(url);

        WebElement editTextPushServer = driver.findElementById("editText_pushServer");
        editTextPushServer.sendKeys(url.toString() + "rest/registry/device");

        WebElement editTextVariantId = driver.findElementById("editText_variantId");
        editTextVariantId.sendKeys(variantId);

        WebElement editTextSenderId = driver.findElementById("editText_senderId");
        editTextSenderId.sendKeys("489342927486");

        WebElement editTextAlias = driver.findElementById("editText_alias");
        editTextAlias.sendKeys("no-alias");

        WebElement buttonRegister = driver.findElementById("button_register");
        buttonRegister.click();


        String response = given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body("{\"key\":\"value\",\"alert\":\"HELLO!\", \"sound\":\"default\", \"badge\":7,\"simple-push\":\"version=123\"}")
            .expect()
                .statusCode(200)
            .when()
                .post(url.toString() + "rest/sender/broadcast/" + pushApplicationId).asString();



    }




}
