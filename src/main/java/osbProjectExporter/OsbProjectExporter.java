package osbProjectExporter;

import com.bea.wli.config.Ref;
import com.bea.wli.config.importexport.EncryptionScope;
import com.bea.wli.config.mbeans.ConfigMBean;
import com.bea.wli.sb.management.configuration.ALSBConfigurationMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;


/**
 * The utility exports the given OSB project sources and its dependencies from the given environment.
 */
public class OsbProjectExporter {

    public static final String USAGE = "Usage:\n" +
            "java -jar OsbProjectExporter.jar [url userName password projectName [exportDir]]\n" +
            "where\n" +
            "   url: Required. WLS Admin host and port to connect to over t3 protocol. Required. E.g. 't3://localhost:7001'.\n" +
            "   userName: User name to connect to WLS Admin server. Required.\n" +
            "   password: User password to connect to WLS Admin server. Required.\n" +
            "   projectName: An OSB project name to be exported. Required.\n" +
            "   exportDir: Path on the local machine to export to. Optional. Default: current directory.";


    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("The utility exports the given OSB project sources and its dependencies from the given environment.\n" +
                    USAGE);
            return;
        }

        Map<String, String> parsedArgs = parseArgs(args);

        try {
            exportProjectJarFromServer(parsedArgs);
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to export OSB project sources. " + e);
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
            System.out.println("[ERROR] Incorrect number of arguments.\n" +
                    USAGE);
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
        parsedArgs.put("url", args[0].trim());

        // Username
        if (args[1].isEmpty()) {
            System.out.println("[ERROR] The given username cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("userName", args[1].trim());

        // Password
        if (args[2].isEmpty()) {
            System.out.println("[ERROR] The given password cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("password", args[2].trim());

        // Project name
        String projectName = args[3].trim();
        if (projectName.isEmpty()) {
            System.out.println("[ERROR] The given project name cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("projectName", projectName);

        // Path
        if (args.length == 5) {
            // Check if the path exists
            String path = args[4].trim();
            if (!new File(path).exists()) {
                System.out.println("The given path does not exist: " + path);
                System.exit(1);
            }
            parsedArgs.put("exportDir", path);
        } else {
            // Create default path in the current directory
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String exportDir = System.getProperty("user.dir") + File.separator
                    + "OSBExport_" + projectName + "_" + timestamp;
            parsedArgs.put("exportDir", exportDir);
        }

        return parsedArgs;
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


    static class Globals {

        public static MBeanServerConnection connection;
        public static JMXConnector connector;
    }


    /**
     * Export the jar of the project's sources with dependencies from the server and unpack it to the given directory
     *
     * @param args Map<String, String> The arguments
     */
    public static void exportProjectJarFromServer(Map<String, String> args) throws Exception {

        // Get the byte array of the exported jar
        byte[] jarBinary = getJarBinary(args);

//        writeToFile(args, jarBinary);

        unpackJarBinary(jarBinary, args.get("exportDir"));
    }


    /**
     * Write the given byte array to a file
     *
     * @param args  Map<String, String> The arguments
     * @param input byte[] The byte array to write
     */
    private static void writeToFile(Map<String, String> args, byte[] input) throws IOException {

        String fullPath = args.get("exportDir") + File.separator + args.get("projectName") + ".jar";
        // Write the jar to the file system
        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            fos.write(input);
        } catch (IOException e) {
            throw new IOException("[ERROR] Failed writing the jar to the file system. " + e);
        }
    }


    /**
     * Unpack the given jar binary to the given directory
     *
     * @param jarBinary  byte[] The jar binary
     * @param exportDir String The directory to export to
     */
    public static void unpackJarBinary(byte[] jarBinary, String exportDir) throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(jarBinary))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                File file = new File(exportDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw new IOException("Failed to create directory " + file);
                    }
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = jarInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                jarInputStream.closeEntry();
            }
        }
    }


    /**
     * Get the project jar as a byte array
     *
     * @param args Map<String, String> The arguments
     * @return byte[] The project jar as a byte array
     */
    public static byte[] getJarBinary(Map<String, String> args) throws Exception {

        // Connect to the server
        initConnection(args.get("url"),
                args.get("userName"),
                args.get("password"),
                "weblogic.management.mbeanservers.domainruntime");

        // Get project references
        Set<Ref> resourceRefs = getResourceRefs(args.get("projectName"));

        // Get the project jar
        byte[] jarBinary;
        boolean includeDependencies = true;
        EncryptionScope encryptionScope = EncryptionScope.NoEncryption;
        ConfigMBean configMBean = getConfigMBean(Globals.connection);
        try {
            jarBinary = configMBean.export(resourceRefs, includeDependencies, encryptionScope, null);
        } catch (Exception e) {
            throw new Exception("[ERROR] Failed exporting the project " + args.get("projectName") + ". " + e);
        } finally {
            if (Globals.connector != null) {
                Globals.connector.close();
            }
        }

        return jarBinary;
    }


    /**
     * Get the project references
     *
     * @param projectName String The project name
     * @return Set<Ref> The project references
     */
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


    /**
     * The method returns ConfigMBean for a given MBeanServerConnection
     *
     * @param mBeanServerConnection MBeanServerConnection, server connection
     * @return ConfigMBean
     */
    private static ConfigMBean getConfigMBean(MBeanServerConnection mBeanServerConnection) throws Exception {
        try {
            return JMX.newMBeanProxy(
                    mBeanServerConnection,
                    ObjectName.getInstance(
                            "com.bea.wli.config:Name=Config.ServiceBus,Type=com.bea.wli.config.mbeans.ConfigMBean"),
                    ConfigMBean.class);
        } catch (Exception e) {
            System.out.println(new Timestamp(System.currentTimeMillis())
                    + " [ERROR] Could not get ConfigMBean");
            if (Globals.connector != null) {
                Globals.connector.close();
            }
            throw new Exception("[ERROR] Could not get ConfigMBean");
        }
    }


    /**
     * The method returns ALSBConfigurationMBean for a given server connection.
     *
     * @param mBeanServerConnection MBeanServerConnection, server connection
     * @return ALSBConfigurationMBean
     */
    private static ALSBConfigurationMBean getALSBConfigurationMBean(
            MBeanServerConnection mBeanServerConnection) throws Exception {
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

            throw new Exception("[ERROR] Could not get ALSBConfigurationMBean");
        }
    }
}