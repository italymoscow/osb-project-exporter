package osbProjectExporter;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class FileUtilTest {

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
    public void testParseFile() {
        File file = new File("src/test/resources/OSBExport_F000T00_Felles_20241023094828014/F000T00_Felles/Mapping/common.XSLT");
        FileUtil.parseFile(file);
    }
}