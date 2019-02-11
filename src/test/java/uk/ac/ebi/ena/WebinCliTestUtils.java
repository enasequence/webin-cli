package uk.ac.ebi.ena;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;

import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class WebinCliTestUtils {

    public static File
    createTempDir()
    {
        try {
            File folder = File.createTempFile("test", "test");
            Assert.assertTrue(folder.delete());
            Assert.assertTrue(folder.mkdirs());
            return folder;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static WebinCliParameters
    createWebinCliParameters( Path manifestFile, Path inputDir ) {
        return createWebinCliParameters(manifestFile.toFile(), inputDir.toFile());
    }

    public static WebinCliParameters
    createWebinCliParameters( File manifestFile ) {
        return createWebinCliParameters(manifestFile, manifestFile.getParentFile());
    }

    public static WebinCliParameters
    createWebinCliParameters( File manifestFile, File inputDir ) {
        WebinCliParameters parameters = new WebinCliParameters();
        parameters.setManifestFile(manifestFile);
        parameters.setInputDir(inputDir);
        parameters.setOutputDir(WebinCliTestUtils.createTempDir());
        parameters.setUsername(System.getenv( "webin-cli-username" ));
        parameters.setPassword(System.getenv( "webin-cli-password" ));
        parameters.setTestMode(true);
        return parameters;
    }

    public static Path
    createDefaultTempFile(boolean compress)
    {
        return createTempFile(null, null, compress, "TEST");
    }

    public static Path
    createDefaultTempFile(String fileName, boolean compress)
    {
        return createTempFile(fileName, null, compress, "TEST");
    }

    public static Path
    createDefaultTempFile(String fileName, Path folder, boolean compress)
    {
        return createTempFile(fileName, folder, compress, "TEST");
    }

    public static Path
    createTempFile(String fileName)
    {
        return createTempFile(fileName, false, null);
    }

    public static Path
    createTempFile(boolean compress, String contents) {
        return createTempFile(null, null, compress, contents);
    }

    public static Path
    createTempFile(String fileName, boolean compress, String contents)
    {
        return createTempFile(fileName, null, compress, contents);
    }

    public static Path
    createTempFile(String fileName, Path folder, boolean compress, String contents)
    {
        try {
            Path path;
            if (folder != null && fileName != null)
                path = Files.createTempFile(folder, "TEST", fileName);
            else if (fileName != null)
                path = Files.createTempFile("TEST", fileName);
            else
                path = Files.createTempFile("TEST", "TEST");
            if (contents != null) {
                InputStream is = new ByteArrayInputStream(contents.getBytes());
                OutputStream os;
                IOUtils.copy(is, (os = compress ?
                        new GZIPOutputStream(Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC))
                        : Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC)));
                os.flush();
                os.close();
            }
            Assert.assertTrue(Files.exists(path));
            Assert.assertTrue(Files.isRegularFile(path));
            return path;
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Path
    createTempFileFromResource( String resource, Path folder, boolean compress, String...suffix ) throws IOException
    {
        try {
            URL url = WebinCliTestUtils.class.getClassLoader().getResource( resource );
            File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
            Path path = Files.createTempFile( folder, "COPY", file.getName() + ( suffix.length > 0 ? Stream.of( suffix ).collect( Collectors.joining( "" ) ) : "" ) );
            OutputStream os;
            Files.copy( file.toPath(), ( os = compress ? new GZIPOutputStream( Files.newOutputStream( path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC ) )
                    : Files.newOutputStream( path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC ) ) );
            os.flush();
            os.close();
            Assert.assertTrue( Files.exists( path ) );
            Assert.assertTrue( Files.isRegularFile( path ) );
            return path;
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String
    createName() {
        return String.format( "TEST-NAME %X", System.currentTimeMillis() );
    }

    public static String
    readFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static File
    getFile( String filePath ) {
        return new File( WebinCliTestUtils.class
                .getClassLoader()
                .getResource( filePath )
                .getFile());
    }

    public static Path
    getPath( String filePath ) {
        URL url =
                WebinCliTestUtils.class
                .getClassLoader()
                .getResource( filePath );
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    public static SubmissionBundle
    prepareSubmissionBundle(AbstractWebinCli<?> cli) {
        try {
            cli.getParameters().setManifestFile( File.createTempFile( "test", "test" ) );
            File submitDir = createTempDir();
            cli.setSubmitDir( submitDir );
            cli.prepareSubmissionBundle();
            SubmissionBundle submissionBundle = cli.getSubmissionBundle();
            Assert.assertTrue(Files.isSameFile(submissionBundle.getSubmitDirectory().toPath(), submitDir.toPath()));
            return submissionBundle;
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String
    readXmlFromSubmissionBundle(SubmissionBundle submissionBundle, SubmissionBundle.SubmissionXMLFileType xmlFileType) {
        SubmissionBundle.SubmissionXMLFile xmlFile = submissionBundle.getXMLFileList().stream()
                .filter(file -> file.getType().equals(xmlFileType) )
                .findFirst()
                .get();
        if (xmlFile == null) {
            return null;
        }
        return WebinCliTestUtils.readFile( xmlFile.getFile().toPath() );
    }


    public static void
    assertAnalysisXml(String xml, String expectedXml) {
        xml = xml.replaceAll("<\\?xml.*", "");
        xml = xml.replaceAll("\\r\\n?", "\n");
        xml = Arrays.stream(xml.split("\n"))
                .filter( line -> !line.trim().isEmpty() )
                .map( line -> line.replaceAll("^\\s+", ""))
                .map( line -> line.replaceAll("\\s+$", ""))
                .collect(Collectors.joining("\n"));
        xml = xml.replaceAll("<\\s+", "<");
        xml = xml.replaceAll("\\s+/>", "/>");
        xml = xml.replaceAll("\\s*alias=\"[^\"]+\"", "");
        expectedXml = expectedXml.replaceAll("<\\?xml.*", "");
        expectedXml = expectedXml.replaceAll("\\r\\n?", "\n");
        expectedXml = Arrays.stream(expectedXml.split("\n"))
                .filter( line -> !line.trim().isEmpty() )
                .map( line -> line.replaceAll("^\\s+", ""))
                .map( line -> line.replaceAll("\\s+$", ""))
                .collect(Collectors.joining("\n"));
        expectedXml = expectedXml.replaceAll("<\\s+", "<");
        expectedXml = expectedXml.replaceAll("\\s+/>", "/>");
        expectedXml = expectedXml.replaceAll("\\s*alias=\"[^\"]+\"", "");
        Assert.assertFalse(xml.isEmpty());
        Assert.assertFalse(expectedXml.isEmpty());
        Assert.assertEquals(expectedXml, xml);
    }


    public static String getWebinUsername() {
        String username = System.getenv( "webin-cli-username" );
        Assert.assertNotNull( "please set up environment variable: webin-cli-username", username);
        return username;
    }

    public static String getWebinPassword() {
        String password = System.getenv( "webin-cli-password" );
        Assert.assertNotNull( "please set up environment variable: webin-cli-password", password);
        return password;
    }
}
