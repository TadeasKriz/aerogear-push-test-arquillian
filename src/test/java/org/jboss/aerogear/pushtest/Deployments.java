package org.jboss.aerogear.pushtest;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.impl.maven.logging.LogRepositoryListener;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Deployments {

    public static Archive testedApk() {
        return Maven.resolver().resolve("org.jboss.aerogear.pushtest:aerogear-test-android:apk:1.0-SNAPSHOT").withoutTransitivity().asSingle(JavaArchive.class);
    }

    public static WebArchive unifiedPushServer() {

        // FIXME this is ugly!
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ag-push.war").as(ZipImporter.class).importFrom(Maven.resolver().resolve("org.jboss.aerogear.connectivity:pushee:war:0.3.0-SNAPSHOT").withoutTransitivity().asSingleFile()).as(WebArchive.class);

        war.delete("/WEB-INF/classes/META-INF/persistence.xml");

        war.addAsResource("META-INF/persistence.xml");

        return war;
    }


}
