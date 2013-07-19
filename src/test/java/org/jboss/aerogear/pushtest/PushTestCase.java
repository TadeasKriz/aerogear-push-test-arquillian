package org.jboss.aerogear.pushtest;

import com.google.common.base.Function;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Cookies;
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
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.android.AndroidDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
@RunAsClient
public class PushTestCase {

    @ArquillianResource
    @OperateOnDeployment("jbossas")
    URL url;

    static Cookies cookies;

    static String pushApplicationId;
    static String masterSecret;
    static String variantId;
    static String secret;

    @Deployment(name = "android", testable = false)
    @TargetsContainer("android")
    public static Archive<?> createAndroidDeployment() {
        return Deployments.testedApk();
    }

    @Deployment(name = "jbossas")
    @TargetsContainer("jbossas")
    public static Archive<?> createJbossasDeployment() {
        return Deployments.unifiedPushServer();
    }

    @Before
    public void trySetup() {
        if(pushApplicationId == null || variantId == null) {
            setup(url);
        }
    }

    public void setup(URL url) {
        Response response;

        response = given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body("{\"loginName\" : \"admin\", \"password\" : \"123\"}")
            .expect()
                .statusCode(200)
            .when()
                .post(url.toString() + "rest/auth/login");

        cookies = response.detailedCookies();

        response = given()
                .cookies(cookies)
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body("{\"name\" : \"AutomatedTestApp\", \"description\" : \"android-test-app\"}")
            .expect()
                .statusCode(201).body("id", is(not(nullValue()))) // TODO this is not enough! (and should be 201)
            .when()
                .post(url.toString() + "rest/applications");

        JsonPath jsonPath = JsonPath.from(response.asString());

        pushApplicationId = jsonPath.getString("pushApplicationID");
        masterSecret = jsonPath.get("masterSecret");

        response = given()
                .cookies(cookies)
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body("{\"googleKey\" : \"AIzaSyC0GBQqv2RVYdo2wONCT-IcNlufHt253VI\"}")
            .expect()
                .statusCode(201).body("id", is(not(nullValue())))
            .when()
                .post(url.toString() + "rest/applications/" + pushApplicationId + "/android");

        jsonPath = JsonPath.from(response.asString());

        variantId = jsonPath.getString("variantID");
        secret = jsonPath.getString("secret");
    }

    @Test
    @OperateOnDeployment("android")
    public void sendTestNotification(@Drone AndroidDriver driver) throws JSONException {
        WebElement editTextPushServer = driver.findElementById("editText_pushServer");
        editTextPushServer.sendKeys(url.toString() + "rest/registry/device");

        WebElement editTextVariantId = driver.findElementById("editText_variantId");
        editTextVariantId.sendKeys(variantId);

        WebElement editTextSenderId = driver.findElementById("editText_senderId");
        editTextSenderId.sendKeys("489342927486");

        WebElement editTextAlias = driver.findElementById("editText_alias");
        editTextAlias.sendKeys("no-alias");

        WebElement editTextSecret = driver.findElementById("editText_secret");
        editTextSecret.sendKeys(secret);

        WebElement textViewOutput = driver.findElementById("textView_output");
        String beforeRegisterText = textViewOutput.getText();

        WebElement buttonRegister = driver.findElementById("button_register");
        buttonRegister.click();

        new WebDriverWait(driver, 10).until(ExpectedConditions.textToBePresentInElement(By.id("button_register"), "Unregister"));

        int verificationKeysCount = 3;
        int verificationKeyLength = 8;
        int verificationValueLenght = 8;
        Map<String, String> verificationMap = new HashMap<String, String>();
        for(int i = 0; i < verificationKeysCount; i++) {
            String key = UUID.randomUUID().toString().substring(0, verificationKeyLength);
            String value = UUID.randomUUID().toString().substring(0, verificationValueLenght);

            verificationMap.put(key, value);
        }

        //String notificationBody = "{\"message\" : { \"key1\" : \"value1\", \"key2\" : \"value2\" }, \"staging\":\"development\", \"key\":\"value\",\"alert\":\"HELLO!\",\"sound\":\"default\",\"badge\":7,\"simple-push\":\"version=123\"}";

        String notificationBody = "{";

        for(String key : verificationMap.keySet()) {
            if(!notificationBody.equals("{")) {
                notificationBody += ", ";
            }
            notificationBody += "\"" + key + "\" : \"" + verificationMap.get(key) + "\"";
        }

        notificationBody += "}";

        String response = given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body(notificationBody)
                .auth().basic(pushApplicationId, masterSecret)
            .expect()
                .statusCode(200)
            .when()
                .post(url.toString() + "rest/sender/broadcast").asString();


        new WebDriverWait(driver, 10).until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElement(By.id("textView_output"), beforeRegisterText)));

        String text = textViewOutput.getText();

        JSONObject deliveredJson = new JSONObject(text);

        for(String key : verificationMap.keySet()) {
            assertTrue("Key \"" + key + "\" wasn't delivered!", deliveredJson.has(key));

            assertEquals("Key \"" + key + "\" delivered with wrong value!", verificationMap.get(key), deliveredJson.get(key));
        }


    }




}
