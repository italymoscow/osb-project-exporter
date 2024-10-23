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
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class OsbUtils {

    static class Globals {

        public static MBeanServerConnection connection;
        public static JMXConnector connector;
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
     * Get the project jar as a byte array
     *
     * @param args Map<String, String> The arguments
     * @return byte[] The project jar as a byte array
     */
    public static byte[] getJarBinary(Map<String, String> args) throws Exception {

        // Connect to the server
        System.out.println("Connecting to the server");
        initConnection(args.get("url"),
                args.get("userName"),
                args.get("password"),
                "weblogic.management.mbeanservers.domainruntime");

        // Get project references
        Set<Ref> resourceRefs = getResourceRefs(args.get("projectName"));

        // Get the project jar
        System.out.println("Exporting the jar for project " + args.get("projectName"));
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
            throw new RuntimeException("[ERROR] Failed getting the project references for project " +
                    projectName + ". " + e.getMessage());
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
