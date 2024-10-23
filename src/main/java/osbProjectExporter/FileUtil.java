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


public class FileUtil {

    public static final Map<String, String> validExtensions;

    static {
        Map<String, String> extensions = new HashMap<>();
        extensions.put("BusinessService", "biz");
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
     * Copy the folder from source to target
     *
     * @param source String The source folder
     * @param target String The target folder
     */
    public static void copyFolder(String source, String target) throws IOException {
        Path sourcePath = new File(source).toPath();
        Path targetPath = new File(target).toPath();
        Files.walk(sourcePath)
                .forEach(sp -> {
                    Path tp = targetPath.resolve(sourcePath.relativize(sp));
                    try {
                        Files.copy(sp, tp, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy " + source + " to " + target, e);
                    }
                });
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
     * @param jarBinary byte[] The jar binary
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


    public static void parseFiles(String folder) throws ParserConfigurationException, IOException, SAXException {

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
                    file = parseFile(file);

                } else {
                    System.out.println("Extension " + fileExtension + " is not supported. Skipping " + file.getPath());
                }
            }
        }
    }


    public static File parseFile(File file) {

        // Parse the file as XML
        Document doc;
        try {
            doc = getXmlDocFromFile(file);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            System.out.println("Failed to parse the file as XML: " + file.getPath());

            return file;
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

        return file;
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
