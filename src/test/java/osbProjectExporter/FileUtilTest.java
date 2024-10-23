package osbProjectExporter;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class FileUtilTest {

    @Test
    public void testCopyFolder() throws IOException {
        String source = "src/test/resources/OSBExport_F000T00_Felles_20241023094828014";
        String target = "src/test/resources/OSBExport_F000T00_Felles_20241023094828014_parsed";
        FileUtil.copyFolder(source, target);

        // Assert the new folder exists
        assertTrue(FileUtil.folderExists(target));
    }

    @Test
    public void testParseFiles() {
    }

    @Test
    public void testGetFileExtension() {
        String path = "src/test/resources/OSBExport_F000T00_Felles_20241023094828014/F000T00_Felles/Business/F000T00_Skriv_tRampe.BusinessService";
        File folder = new File(path);
        assertEquals("BusinessService", FileUtil.getFileExtension(folder));
    }

    @Test
    public void testListFilesRecursively() {
        List<File> files = FileUtil.listFilesRecursively("src/test/resources/OSBExport_F000T00_Felles_20241023094828014");
        // Print the files
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
        }
        assertEquals(11, files.size());

    }

    @Test
    public void testChangeFileExtension() {
        File file = new File("src/test/resources/OSBExport_F000T00_Felles_20241023094828014/F000T00_Felles/Business/F000T00_Skriv_tRampe.BusinessService");
        String fileExtensionOld = FileUtil.getFileExtension(file);
        file = FileUtil.changeFileExtension(file, FileUtil.validExtensions.get(fileExtensionOld));
        String fileExtensionNew = FileUtil.getFileExtension(file);
        assertEquals("biz", fileExtensionNew);

        // Change back
        file = FileUtil.changeFileExtension(file, fileExtensionOld);
        fileExtensionNew = FileUtil.getFileExtension(file);
        assertEquals(fileExtensionOld, fileExtensionNew);
    }

    @Test
    public void testParseFile() throws ParserConfigurationException, IOException, SAXException {
        File file = new File("src/test/resources/OSBExport_F000T00_Felles_20241023094828014/F000T00_Felles/Mapping/common.XSLT");
        File fileNew = FileUtil.parseFile(file);
        assertTrue(fileNew != null);
    }
}