package osbProjectExporter;

import com.bea.wli.config.Ref;
import com.bea.wli.config.importexport.EncryptionScope;
import com.bea.wli.sb.management.configuration.ALSBConfigurationMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


public class Main {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("The utility exports the given OSB project and its dependencies from the given environment.\n" +
                    "[INFO] Usage:\n" +
                    "java -jar OsbProjectExporter.jar [host:port] [userName] [password] [projectName] [path]\n\n" +
                    "   [host:port]: WLS Admin host and port to connect to. t3 protocol will be used. \n" +
                    "   [userName]: User name to connect to WLS Admin server.\n" +
                    "   [password]: User password to connect to WLS Admin server.\n" +
                    "   [projectName]: An OSB project name to be exported.\n" +
                    "   [path]: Path on the local machine to export to. Default: current directory.");
            return;
        }

        Map<String, String> parsedArgs = parseArgs(args);

        try {
            String fullPath = exportProjectJarFromServer(parsedArgs);
            System.out.println("[INFO] The project " + parsedArgs.get("projectName") + " has been exported to " + fullPath);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed exporting the project " + parsedArgs.get("projectName") + ". " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Globals.connector.close();
            } catch (IOException e) {
                System.out.println("[ERROR] Failed closing the connection. " + e.getMessage());
            }
        }

    }

    /**
     * Parse the given arguments
     *
     * @param args String[] The arguments to parse
     * @return Map<String, String> A map with the parsed arguments
     */
    public static Map<String, String> parseArgs(String[] args) {
        if (args.length < 4) {
            System.out.println("[ERROR] Incorrect number of arguments. Correct usage:\n" +
                    "java -jar OsbProjectExporter.jar [host:port] [userName] [password] [projectName] {exportDir}\n\n" +
                    "   [host:port]: Required. WLS Admin host and port to connect to. t3 protocol will be used. \n" +
                    "   [userName]: Required. User name to connect to WLS Admin server.\n" +
                    "   [password]: Required. User password to connect to WLS Admin server.\n" +
                    "   [projectName]: Required. An OSB project name to be exported.\n" +
                    "   [exportDir]: Optional. Path on the local machine to export to. Default: current directory.");
            System.exit(1);
        }

        Map<String, String> parsedArgs = new HashMap<>();

        // URL
        if (args[0].isEmpty()) {
            System.out.println("[ERROR] The given URL cannot be empty. E.g. t3://localhost:7001");
            System.exit(1);
        }
        if (!args[0].startsWith("t3://")) {
            System.out.println("[ERROR] The given URL must start with 't3://'. E.g. t3://localhost:7001");
            System.exit(1);
        }
        if (!args[0].substring(6).contains(":")) {
            System.out.println("[ERROR] The given URL must contain a port number. E.g. t3://localhost:7001");
            System.exit(1);
        }
        parsedArgs.put("url", args[0]);

        // Username
        if (args[1].isEmpty()) {
            System.out.println("[ERROR] The given username cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("userName", args[1]);

        // Password
        if (args[2].isEmpty()) {
            System.out.println("[ERROR] The given password cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("password", args[2]);

        // Project name
        if (args[3].isEmpty()) {
            System.out.println("[ERROR] The given project name cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("projectName", args[3]);

        // Path
        if (args.length == 5) {
            // Check if the path exists
            String path = args[4];
            if (!new File(path).exists()) {
                System.out.println("The given path does not exist: " + path);
                System.exit(1);
            }
            parsedArgs.put("exportDir", args[4]);
        } else {
            // Default path
            parsedArgs.put("exportDir", System.getProperty("user.dir"));
        }

        return parsedArgs;
    }

    /**
     * Used to create a hashtable with properties for connecting to SOA servers
     *
     * @param url,      String, t3 URL of the Admin server
     * @param userName, String, WLS user part of security group Administrators
     * @param password, String, WLS user password
     * @return Hashtable with jndi properties.
     */
    public static Hashtable getConnectionDetails(String url, String userName, String password) {
        Hashtable jndiProps = new Hashtable();
        jndiProps.put(Context.PROVIDER_URL, url);
        jndiProps.put(Context.INITIAL_CONTEXT_FACTORY,
                "weblogic.jndi.WLInitialContextFactory");
        jndiProps.put(Context.SECURITY_PRINCIPAL, userName);
        jndiProps.put(Context.SECURITY_CREDENTIALS, password);
        jndiProps.put("dedicated.connection", "true");
        jndiProps.put("jmx.remote.x.request.waiting.timeout", 30000L);

        return jndiProps;
    }

    /**
     * Initialize connection to the Runtime MBean Server
     *
     * @param url,          String, t3 URL of the Admin server
     * @param username,     String, WLS user part of security group Administrators
     * @param password,     String, WLS user password
     * @param mbServerType, MBeanServer type, e.g. "weblogic.management.mbeanservers.domainruntime" or
     *                      "weblogic.management.mbeanservers.runtime"
     */
    public static void initConnection(String url, String username, String password, String mbServerType)
            throws Exception {
        String jndiRoot = "/jndi/";
        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:" + url + jndiRoot + mbServerType);
        HashMap<String, Object> connectionMap = new HashMap<>();
        connectionMap.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
        connectionMap.put(Context.SECURITY_PRINCIPAL, username);
        connectionMap.put(Context.SECURITY_CREDENTIALS, password);
        connectionMap.put("jmx.remote.x.request.waiting.timeout", 30000L);
        try {
            Globals.connector = JMXConnectorFactory.connect(serviceURL, connectionMap);
            Globals.connection = Globals.connector.getMBeanServerConnection();
        } catch (Exception e) {
            System.out.println("[ERROR] Failed connecting to " + url + ". " + e);
            throw new Exception("[ERROR] Failed to connect to " + url + ". " + e);
        }
    }

    /**
     * The method returns ALSBConfigurationMBean for a given server connection.
     *
     * @param mBeanServerConnection MBeanServerConnection, server connection
     * @return ALSBConfigurationMBean
     */
    private static ALSBConfigurationMBean getALSBConfigurationMBean(
            MBeanServerConnection mBeanServerConnection) throws IOException {
        try {

            return JMX.newMBeanProxy(
                    mBeanServerConnection,
                    ObjectName.getInstance("com.bea:Name=" + ALSBConfigurationMBean.NAME +
                            ",Type=" + ALSBConfigurationMBean.TYPE),
                    ALSBConfigurationMBean.class);
        } catch (Exception e) {
            System.out.println(new Timestamp(System.currentTimeMillis())
                    + " [ERROR] Could not get ALSBConfigurationMBean");
            if (Globals.connector != null) {
                Globals.connector.close();
            }

            return null;
        }
    }

    static class Globals {

        public static final ObjectName service;
        public static MBeanServerConnection connection;
        public static JMXConnector connector;

        static {
            try {
                service = new ObjectName("com.bea:Name=DomainRuntimeService," +
                        "Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
            } catch (MalformedObjectNameException e) {
                throw new AssertionError(e.getMessage());
            }
        }
    }

    /**
     * Export the given project from the server as a jar archive
     *
     * @param args Map<String, String> The arguments
     * @return String The full path of the exported project jar archive
     */
    public static String exportProjectJarFromServer(Map<String, String> args) throws Exception {

        String fullPath = args.get("exportDir") + File.separator + args.get("projectName") + ".jar";

        // Connect to the server
        initConnection(args.get("url"), args.get("userName"), args.get("password"), "weblogic.management.mbeanservers.runtime");

        // Get project references
        Set<Ref> resourceRefs = getResourceRefs(args.get("projectName"));

        Boolean includeDependencies = true;
        EncryptionScope encryptionScope = EncryptionScope.NoEncryption;
        byte[] exportedJar;
        try {
            ObjectName sbc = new ObjectName("com.bea.wli.config:Name=Config.ServiceBus,Type=com.bea.wli.config.mbeans.ConfigMBean");
            exportedJar = (byte[]) Globals.connection.invoke(sbc,
                    "export",
                    new Object[]{resourceRefs, includeDependencies, encryptionScope, null},
                    new String[]{"java.util.Set", "java.lang.Boolean", "com.bea.wli.config.importexport.EncryptionScope", null});
        } catch (Exception e) {
            return fullPath;
        }

        // Write exportedJar to a file
        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            fos.write(exportedJar);
        }

        return fullPath;
    }


    public static Set<Ref> getResourceRefs(String projectName) throws Exception {

        ALSBConfigurationMBean alsbCore = getALSBConfigurationMBean(Globals.connection);
        Set<Ref> projectRefs;
        try {
            projectRefs = alsbCore.getRefs(new Ref("Project", Ref.DOMAIN, projectName));
        } catch (Exception e) {
            throw new Exception("");
        }

        return projectRefs;
    }

}