package osbProjectExporter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static osbProjectExporter.Main.exportProjectJarFromServer;


public class MainTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void testParseArgsOK() {
        String[] args = new String[]{"localhost:7001",
                "weblogic",
                "password",
                "project",
                "C:\\Users"};
        Map<String, String> parsedArgs = Main.parseArgs(args);
        assertEquals("localhost:7001", parsedArgs.get("url"));
        assertEquals("weblogic", parsedArgs.get("userName"));
        assertEquals("password", parsedArgs.get("password"));
        assertEquals("project", parsedArgs.get("project"));
        assertEquals("C:\\Users", parsedArgs.get("path"));
    }

    @Test
    public void testParseArgsWrongNumberOfParameters() {
        exit.expectSystemExitWithStatus(1);
        String[] args = new String[]{"localhost:7001"};
        Map<String, String> parsedArgs = Main.parseArgs(args);
    }

    @Test
    public void testParseArgsWrongUrl() {
        exit.expectSystemExitWithStatus(1);
        String[] args = new String[]{"localhost:7001",
                "weblogic",
                "password",
                "project",
                "C:\\Users"};
        Map<String, String> parsedArgs = Main.parseArgs(args);
    }

    @Test
    public void testParseArgsWrongUrlNoPort() {
        exit.expectSystemExitWithStatus(1);
        String[] args = new String[]{"localhost",
                "weblogic",
                "password",
                "project",
                "C:\\Users"};
        Map<String, String> parsedArgs = Main.parseArgs(args);
    }

    @Test
    public void testExportProjectJarFromServer() throws Exception {
        String[] args = new String[]{"t3://ngsbboq1t.joh.no:7101",
                "vimosh",
                "vimosh22",
                "F000T00_Felles"};
        Map<String, String> parsedArgs = Main.parseArgs(args);
        String a = exportProjectJarFromServer(parsedArgs);
        System.out.println("Full path: " + a);
    }
}