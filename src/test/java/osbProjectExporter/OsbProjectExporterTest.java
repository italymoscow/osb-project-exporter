package osbProjectExporter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static osbProjectExporter.OsbProjectExporter.exportProjectJarFromServer;


public class OsbProjectExporterTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void testParseArgsOK() {
        String[] args = new String[]{"localhost:7001",
                "weblogic",
                "password",
                "project",
                "C:\\Users"};
        Map<String, String> parsedArgs = OsbProjectExporter.parseArgs(args);
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
        Map<String, String> parsedArgs = OsbProjectExporter.parseArgs(args);
    }

    @Test
    public void testParseArgsWrongUrl() {
        exit.expectSystemExitWithStatus(1);
        String[] args = new String[]{"localhost:7001",
                "weblogic",
                "password",
                "project",
                "C:\\Users"};
        Map<String, String> parsedArgs = OsbProjectExporter.parseArgs(args);
    }

    @Test
    public void testParseArgsWrongUrlNoPort() {
        exit.expectSystemExitWithStatus(1);
        String[] args = new String[]{"localhost",
                "weblogic",
                "password",
                "project",
                "C:\\Users"};
        Map<String, String> parsedArgs = OsbProjectExporter.parseArgs(args);
    }

    @Test
    public void testExportProjectJarFromServer() throws Exception {
        String[] args = new String[]{"t3://localhost:7101",
                "weblogic",
                "welcome1",
                "F000T00_Felles"};
        Map<String, String> parsedArgs = OsbProjectExporter.parseArgs(args);
        exportProjectJarFromServer(parsedArgs);
    }
}