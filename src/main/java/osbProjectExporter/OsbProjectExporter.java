package osbProjectExporter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


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
            System.out.println("[ERROR] Failed to export OSB project sources for " + parsedArgs.get("projectName") + e);
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
        String url = args[0].trim();
        if (url.isEmpty()) {
            System.out.println("[ERROR] The given URL cannot be empty. E.g. t3://localhost:7001");
            System.exit(1);
        }
        if (!url.startsWith("t3://")) {
            System.out.println("[ERROR] The given URL must start with 't3://'. E.g. t3://localhost:7001");
            System.exit(1);
        }
        if (!url.substring(6).contains(":")) {
            System.out.println("[ERROR] The given URL must contain a port number. E.g. t3://localhost:7001");
            System.exit(1);
        }
        parsedArgs.put("url", url);

        // Username
        String userName = args[1].trim();
        if (userName.isEmpty()) {
            System.out.println("[ERROR] The given username cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("userName", userName);

        // Password
        String password = args[2].trim();
        if (password.isEmpty()) {
            System.out.println("[ERROR] The given password cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("password", password);

        // Project name
        String projectName = args[3].trim();
        if (projectName.isEmpty()) {
            System.out.println("[ERROR] The given project name cannot be empty.");
            System.exit(1);
        }
        parsedArgs.put("projectName", projectName);

        // Path
        if (args.length == 5) {
            String path = args[4].trim();
            parsedArgs.put("exportDir", path);
        } else {
            // Create default path in the current directory
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String exportDir = System.getProperty("user.dir") + File.separator
                    + "OSBExport" + File.separator
                    + "OSBExport_" + projectName + "_" + timestamp;
            parsedArgs.put("exportDir", exportDir);
        }

        return parsedArgs;
    }


    /**
     * Export the jar of the project's sources with dependencies from the server and unpack it to the given directory
     *
     * @param args Map<String, String> The arguments
     */
    public static void exportProjectJarFromServer(Map<String, String> args) throws Exception {

        // Get the byte array of the exported jar
        byte[] jarBinary = OsbUtils.getJarBinary(args);

        // Write the unpacked jar to the file system
        String tmpDir = args.get("exportDir") + File.separator + "tmp";
        FileUtil.unpackJar(jarBinary, tmpDir);

        // Copy the folder
//        FileUtil.copyFolder(args.get("exportDir"), args.get("exportDir") + "_parsed");

        // Parse the files
        FileUtil.processFilesInFolder(tmpDir);

        // Move the parsed files to the export directory
        FileUtil.moveFolderContents(tmpDir, args.get("exportDir"));
    }
}