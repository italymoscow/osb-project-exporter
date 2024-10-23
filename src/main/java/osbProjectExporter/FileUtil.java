package osbProjectExporter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;


public class FileUtil {

    public static final Map<String, String> validExtensions;

    static {
        Map<String, String> extensions = new HashMap<>();
        extensions.put("BusinessService", "bix");
        extensions.put("ProxyService", "proxy");
        extensions.put("Pipeline", "pipeline");
        extensions.put("XML", "xml");
        extensions.put("XMLSchema", "xsd");
        extensions.put("Xquery", "xqy");
        extensions.put("XSLT", "xsl");
        extensions.put("MFL", "mfl");
        extensions.put("WADL", "wadl");
        extensions.put("WSDL", "wsdl");
        extensions.put("ServiceAccount", "sa");
        extensions.put("Archive", "jar");
        extensions.put("JCA", "jca");
        validExtensions = Collections.unmodifiableMap(extensions);
    }


    /**
     * Check if the given folder exists
     *
     * @param path String The folder path
     * @return boolean True if the folder exists, false otherwise
     */
    public static boolean folderExists(String path) {
        File folder = new File(path);
        return folder.exists() && folder.isDirectory();
    }


    /**
     * Move the folder from source to target
     *
     * @param source String The source folder
     * @param target String The target folder
     */
    public static void moveFolderContents(String source, String target) throws IOException {
        System.out.println("Moving " + source + " to " + target);
        Path sourcePath = new File(source).toPath();
        Path targetPath = new File(target).toPath();

        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.skip(1) // Skip the root directory itself
                    .forEach(sp -> {
                        Path tp = targetPath.resolve(sourcePath.relativize(sp));
                        try {
                            if (Files.exists(sp)) {
                                if (Files.isDirectory(sp)) {
                                    Files.createDirectories(tp);
                                } else {
                                    Files.move(sp, tp, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } else {
                                System.out.println("Skipping non-existent file: " + sp);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to move " + sp + " to " + tp, e);
                        }
                    });
        }

        // Recursively delete the source directory
        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete " + path, e);
                        }
                    });
        }
    }


    /**
     * Change the file extension of the given file
     *
     * @param file         File The file to change the extension
     * @param newExtension String The new extension
     * @return File The file with the new extension
     */
    public static File changeFileExtension(File file, String newExtension) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String newFileName = (dotIndex == -1) ? fileName + "." + newExtension : fileName.substring(0, dotIndex) + "." + newExtension;
        File newFile = new File(file.getParent(), newFileName);
        if (file.renameTo(newFile)) {
            return newFile;
        } else {
            throw new RuntimeException("Failed to change file extension for " + file.getAbsolutePath());
        }
    }


    /**
     * Get the file extension of the given file
     *
     * @param file File The file
     * @return String The file extension
     */
    public static String getFileExtension(File file) {
        // Get file extension
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }
        return extension;
    }


    /**
     * Get the list of files in the directory recursively
     *
     * @param directory String The directory
     * @return List<File> The list of files
     */
    public static List<File> listFilesRecursively(String directory) {
        File folder = new File(directory);
        List<File> fileList = new ArrayList<>();
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        fileList.addAll(listFilesRecursively(file.getAbsolutePath()));
                    } else {
                        fileList.add(file);
                    }
                }
            }
        }

        return fileList;
    }


    /**
     * Write the given byte array to a file
     *
     * @param args  Map<String, String> The arguments
     * @param input byte[] The byte array to write
     */
    private static void writeBytesToFile(Map<String, String> args, byte[] input) throws IOException {

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
     * @param jarBytes byte[] The jar binary
     * @param destDir  String The directory to export to
     */
    public static void unpackJar(byte[] jarBytes, String destDir) throws IOException {
        System.out.println("Unpacking the jar to " + destDir);
        try (JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                File file = new File(destDir, entry.getName());
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
     * Process the files in the given folder. The method will delete unnecessary files, rename the files with valid extensions,
     * and parse the files with CDATA content.
     *
     * @param folder String The folder to parse
     */
    public static void processFilesInFolder(String folder) {
        System.out.println("Parsing the files in " + folder);
        // Get the list of files in the directory recursively
        List<File> listOfFiles = listFilesRecursively(folder);

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileExtension = getFileExtension(file);

                // Delete the file if unnecessary
                if (file.getName().equals("ExportInfo") || fileExtension.equals("LocationData")) {
                    if (!file.delete()) {
                        System.out.println("Failed to delete the file: " + file.getPath());
                    }
                    continue;
                }

                if (validExtensions.containsKey(fileExtension)) {
                    // Rename the file
                    file = changeFileExtension(file, validExtensions.get(fileExtension));

                    // Parse the file
                    parseFile(file);
                } else {
                    System.out.println("Extension " + fileExtension + " is not supported. Skipping " + file.getPath());
                }
            }
        }
    }


    /**
     * The method tries to parse the file as an XML.
     * If the content of the first child is CDATA, write the content of the CDATA to the file
     *
     * @param file File The file to parse
     */
    public static void parseFile(File file) {
        // Parse the file as XML
        Document doc;
        try {
            doc = getXmlDocFromFile(file);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            return;
        }

        // Check if the contents of the first child is CDATA
        Element root = doc.getDocumentElement();
        Node child = root.getFirstChild().getNextSibling().getFirstChild();
        if (child != null && child.getNodeType() == Node.CDATA_SECTION_NODE) {
            // Get the content of the CDATA
            String cdata = child.getNodeValue();
            // Write the content of the CDATA to the file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(cdata.getBytes());
            } catch (IOException e) {
                System.out.println("Failed to write the content of the CDATA to the file: " + file.getPath());
            }
        }
    }


    /**
     * Get the XML document from the given file
     *
     * @param file File The file to get the XML document from
     * @return Document The XML document
     */
    public static Document getXmlDocFromFile(File file) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();

        return doc;
    }
}
