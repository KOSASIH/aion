package org.aion.cli;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.aion.mcf.account.Keystore;
import org.aion.base.util.Hex;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.mcf.config.Cfg;
import org.aion.zero.impl.config.CfgAion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import org.aion.zero.impl.cli.Cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class CliTest {

    private static final Cli cli = Mockito.spy(new Cli());

    /**
     * Sets up a spy Cli class that returns the String "password" when the cli.readPassword()
     * is called using any two params.
     */
    @Before
    public void setup() throws IOException {
        doReturn("password").when(cli).readPassword(any(), any());

        // Copies config folder recursively
        String BASE_PATH = Cfg.getBasePath();
        File src = new File(BASE_PATH + "/../modBoot/resource");
        File dst = new File(BASE_PATH + "/config");
        copyRecursively(src, dst);

        CfgAion.setConfFilePath(BASE_PATH + "/config/mainnet/config.xml");
        CfgAion.setGenesisFilePath(BASE_PATH + "/config/mainnet/genesis.json");
        Keystore.setKeystorePath(BASE_PATH + "/keystore");
    }

    @After
    public void shutdown() {
        // Deletes created folders recursively
        String BASE_PATH = Cfg.getBasePath();
        File path1 = new File(BASE_PATH + "/aaaaaaaa");
        File path2 = new File(BASE_PATH + "/abbbbbbb");
        File path3 = new File(BASE_PATH + "/abcccccc");
        File path4 = new File(BASE_PATH + "/keystore");
        File path5 = new File(BASE_PATH + "/config");
        if(path1.exists() || path2.exists() || path3.exists() || path4.exists() || path5.exists()) {
            deleteRecursively(path1);
            deleteRecursively(path2);
            deleteRecursively(path3);
            deleteRecursively(path4);
            deleteRecursively(path5);
        }

        CfgAion.setConfFilePath(BASE_PATH + "/config/mainnet/config.xml");
        CfgAion.setGenesisFilePath(BASE_PATH + "/config/mainnet/genesis.json");
        Keystore.setKeystorePath(BASE_PATH + "/keystore");
    }

    /**
     * Tests the -h argument does not fail.
     */
    @Test
    public void testHelp() {
        String args[] = {"-h"};
        assertEquals(0, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a create arguments do not fail.
     */
    @Test
    public void testCreateAccount() {
        String args[] = {"-a", "create"};
        assertEquals(0, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a list arguments do not fail.
     */
    @Test
    public void testListAccounts() {
        String args[] = {"-a", "list"};
        assertEquals(0, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a export arguments do not fail on a valid account.
     */
    @Test
    public void testExportPrivateKey() {
        String account = Keystore.create("password");

        String[] args = {"-a", "export", account};
        assertEquals(0, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a export arguments fail when the suupplied account is a proper substring of a
     * valid account.
     */
    @Test
    public void testExportSubstringOfAccount() {
        String account = Keystore.create("password");
        String substrAcc = account.substring(1);

        String[] args = {"-a", "export", substrAcc};
        assertEquals(1, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a import arguments do not fail on a fail import key.
     */
    @Test
    public void testImportPrivateKey() {
        ECKey key = ECKeyFac.inst().create();

        String[] args = {"-a", "import", Hex.toHexString(key.getPrivKeyBytes())};
        assertEquals(0, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a import arguments fail when a non-private key is supplied.
     */
    @Test
    public void testImportNonPrivateKey() {
        String account = Keystore.create("password");

        String[] args = {"-a", "import", account};
        assertEquals(1, cli.call(args, CfgAion.inst()));
    }

    /**
     * Tests the -a import arguments do not fail when a valid private key is supplied.
     */
    @Test
    public void testImportPrivateKey2() {
        ECKey key = ECKeyFac.inst().create();
        System.out.println("Original address    : " + Hex.toHexString(key.getAddress()));
        System.out.println("Original public key : " + Hex.toHexString(key.getPubKey()));
        System.out.println("Original private key: " + Hex.toHexString(key.getPrivKeyBytes()));

        String[] args = {"-a", "import", Hex.toHexString(key.getPrivKeyBytes())};
        assertEquals(0, cli.call(args, CfgAion.inst()));

        ECKey key2 = Keystore.getKey(Hex.toHexString(key.getAddress()), "password");
        System.out.println("Imported address    : " + Hex.toHexString(key2.getAddress()));
        System.out.println("Imported public key : " + Hex.toHexString(key2.getPubKey()));
        System.out.println("Imported private key: " + Hex.toHexString(key2.getPrivKeyBytes()));
    }

    /**
     * Tests the -a import arguments fail given an invalid private key.
     */
    @Test
    public void testImportPrivateKeyWrong() {
        String[] args = {"-a", "import", "hello"};
        assertEquals(1, cli.call(args, CfgAion.inst()));
    }

    /**
     * Test the -d | --datadir option to see if;
     * 1. Access the correct dbPath
     * 2. Defaults correctly to "database"
     */
    @Test
    public void testDatadir() {

        String BASE_PATH = Cfg.getBasePath();
        final String[][] networkArgs = new String[][] {
                { "-d" , "" },                              // Unspecified
                { "-d" , "{@.@}" },                         // Invalid (illegal characters)
                { "-d" , "test_db" , "test"},               // Invalid (number of arguments)
                { "-d" , "aaaaaaaa" },                      // Valid
                { "-d" , "abbbbbbb" },                      // Valid
        };

        Cli cli = new Cli();
        Cfg cfg = CfgAion.inst();

        String logOG = cfg.getLog().getLogPath();
        String dbOG = cfg.getDb().getPath();
        System.out.println(logOG + " " + dbOG);

        System.out.println("Invalid Datadir:");
        assertEquals(1, cli.call(networkArgs[0], cfg));
        assertEquals(1, cli.call(networkArgs[1], cfg));
        assertEquals(1, cli.call(networkArgs[2], cfg));

        cfg.getLog().setLogPath(logOG);
        cfg.getDb().setPath(dbOG);
        System.out.println(cfg.getLog().getLogPath() + " " + cfg.getDb().getPath());

        System.out.println("\nValid Datadir 1: " + networkArgs[3][1]);
        assertEquals(2, cli.call(networkArgs[3], cfg) );
        assertEquals("aaaaaaaa/database", cfg.getDb().getPath() );
        assertEquals("aaaaaaaa/log", cfg.getLog().getLogPath() );
        assertEquals(BASE_PATH + "/aaaaaaaa/keystore", Keystore.getKeystorePath());
        System.out.println("\n---------------------------- USED PATHS ----------------------------" +
                "\n> Logger path:   " + BASE_PATH + "/" + cfg.getLog().getLogPath() +
                "\n> Database path: " + BASE_PATH + "/" + cfg.getDb().getPath() +
                "\n> Keystore path: " + Keystore.getKeystorePath() +
                "\n> Config path:   " + CfgAion.getConfFilePath() +
                "\n> Genesis path:  " + CfgAion.getGenesisFilePath() +
                "\n--------------------------------------------------------------------\n\n");

        System.out.println("\nValid Datadir 2: " + networkArgs[4][1]);
        assertEquals(2, cli.call(networkArgs[4], cfg) );
        assertEquals("abbbbbbb/database", cfg.getDb().getPath() );
        assertEquals("abbbbbbb/log", cfg.getLog().getLogPath() );
        assertEquals(BASE_PATH + "/abbbbbbb/keystore", Keystore.getKeystorePath());
        System.out.println("\n---------------------------- USED PATHS ----------------------------" +
                "\n> Logger path:   " + BASE_PATH + "/" + cfg.getLog().getLogPath() +
                "\n> Database path: " + BASE_PATH + "/" + cfg.getDb().getPath() +
                "\n> Keystore path: " + Keystore.getKeystorePath() +
                "\n> Config path:   " + CfgAion.getConfFilePath() +
                "\n> Genesis path:  " + CfgAion.getGenesisFilePath() +
                "\n--------------------------------------------------------------------\n\n");

    }

    /**
     * Test the -n | --network option to see if;
     * 1. Access the correct CONF_FILE_PATH
     * 2. Access the correct GENESIS_FILE_PATH
     * 3. Defaults correctly to "mainnet"
     */
    @Test
    public void testNetwork() {

        String BASE_PATH = Cfg.getBasePath();
        final String[][] networkArgs = new String[][] {
                { "-n" , "" },                              // Unspecified
                { "-n" , "{@.@}" },                         // Invalid (illegal characters)
                { "-n" , "testnet", "test" },               // Invalid (number of arguments)
                { "-n" , "mainnet" },                       // Mainnet
                { "-n" , "conquest" },                      // Conquest
        };

        Cli cli = new Cli();
        Cfg cfg = CfgAion.inst();

        System.out.println("Invalid Networks:");
        assertEquals(1, cli.call(networkArgs[0], cfg) );
        assertEquals(1, cli.call(networkArgs[1], cfg) );
        assertEquals(1, cli.call(networkArgs[2], cfg) );

        System.out.println("\nValid Network 1: " + networkArgs[3][1]);
        assertEquals(2, cli.call(networkArgs[3], cfg) );
        assertEquals("mainnet", CfgAion.getNetwork() );
        assertEquals(BASE_PATH + "/config/mainnet/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/config/mainnet/genesis.json", CfgAion.getGenesisFilePath() );
        System.out.println("\n---------------------------- USED PATHS ----------------------------" +
                "\n> Logger path:   " + BASE_PATH + "/" + cfg.getLog().getLogPath() +
                "\n> Database path: " + BASE_PATH + "/" + cfg.getDb().getPath() +
                "\n> Keystore path: " + Keystore.getKeystorePath() +
                "\n> Config path:   " + CfgAion.getConfFilePath() +
                "\n> Genesis path:  " + CfgAion.getGenesisFilePath() +
                "\n--------------------------------------------------------------------\n\n");

        System.out.println("\nValid Network 2: " + networkArgs[4][1]);
        assertEquals(2, cli.call(networkArgs[4], cfg) );
        assertEquals("conquest", CfgAion.getNetwork() );
        assertEquals(BASE_PATH + "/config/conquest/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/config/conquest/genesis.json", CfgAion.getGenesisFilePath() );
        System.out.println("\n---------------------------- USED PATHS ----------------------------" +
                "\n> Logger path:   " + BASE_PATH + "/" + cfg.getLog().getLogPath() +
                "\n> Database path: " + BASE_PATH + "/" + cfg.getDb().getPath() +
                "\n> Keystore path: " + Keystore.getKeystorePath() +
                "\n> Config path:   " + CfgAion.getConfFilePath() +
                "\n> Genesis path:  " + CfgAion.getGenesisFilePath() +
                "\n--------------------------------------------------------------------\n\n");

    }

    @Test
    public void testMulti() {

        String BASE_PATH = Cfg.getBasePath();
        final String[][] multiArgs = new String[][] {
                { "-d" , "aaaaaaaa"     , "-n" , "conquest" },          // Existing dir / network
                { "-n" , "conquest"     , "-d" , "aaaaaaaa" },          // Exisitng network / dir
                { "-d" , "aaaaaaaa"     , "-n" , "mainnet"  },          // New network
                { "-d" , "abbbbbbb"     , "-n" , "conquest" },          // New dir
                { "-n" , "conquest"     , "-d" , "abcccccc" },          // New dir
                { "-d" , "{@.@}"        , "-n" , "conquest" },          // Invalid dir
                { "-n" , "conquest"     , "-d" , "{@.@}"    },          // Invalid dir
                { "-d" , "aaaaaaaa"     , "-n" , "{@.@}"    },          // Invalid network
                { "-n" , "{@.@}"        , "-d" , "aaaaaaaa" },          // Invalid network
        };

        Cli cli = new Cli();
        Cfg cfg = CfgAion.inst();

        assertEquals(2, cli.call(multiArgs[0], cfg));
        assertEquals("conquest", CfgAion.getNetwork() );
        assertEquals("aaaaaaaa/log", cfg.getLog().getLogPath());
        assertEquals("aaaaaaaa/database", cfg.getDb().getPath());
        assertEquals(BASE_PATH + "/aaaaaaaa/keystore", Keystore.getKeystorePath());
        assertEquals(BASE_PATH + "/aaaaaaaa/config/conquest/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/aaaaaaaa/config/conquest/genesis.json", CfgAion.getGenesisFilePath() );

        assertEquals(2, cli.call(multiArgs[1], cfg));
        assertEquals("conquest", CfgAion.getNetwork() );
        assertEquals("aaaaaaaa/log", cfg.getLog().getLogPath());
        assertEquals("aaaaaaaa/database", cfg.getDb().getPath());
        assertEquals(BASE_PATH + "/aaaaaaaa/keystore", Keystore.getKeystorePath());
        assertEquals(BASE_PATH + "/aaaaaaaa/config/conquest/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/aaaaaaaa/config/conquest/genesis.json", CfgAion.getGenesisFilePath() );

        assertEquals(2, cli.call(multiArgs[2], cfg));
        assertEquals("mainnet", CfgAion.getNetwork() );
        assertEquals("aaaaaaaa/log", cfg.getLog().getLogPath());
        assertEquals("aaaaaaaa/database", cfg.getDb().getPath());
        assertEquals(BASE_PATH + "/aaaaaaaa/keystore", Keystore.getKeystorePath());
        assertEquals(BASE_PATH + "/aaaaaaaa/config/mainnet/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/aaaaaaaa/config/mainnet/genesis.json", CfgAion.getGenesisFilePath() );

        assertEquals(2, cli.call(multiArgs[3], cfg));
        assertEquals("conquest", CfgAion.getNetwork() );
        assertEquals("abbbbbbb/log", cfg.getLog().getLogPath());
        assertEquals("abbbbbbb/database", cfg.getDb().getPath());
        assertEquals(BASE_PATH + "/abbbbbbb/keystore", Keystore.getKeystorePath());
        assertEquals(BASE_PATH + "/abbbbbbb/config/conquest/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/abbbbbbb/config/conquest/genesis.json", CfgAion.getGenesisFilePath() );

        assertEquals(2, cli.call(multiArgs[4], cfg));
        assertEquals("conquest", CfgAion.getNetwork() );
        assertEquals("abcccccc/log", cfg.getLog().getLogPath());
        assertEquals("abcccccc/database", cfg.getDb().getPath());
        assertEquals(BASE_PATH + "/abcccccc/keystore", Keystore.getKeystorePath());
        assertEquals(BASE_PATH + "/abcccccc/config/conquest/config.xml", CfgAion.getConfFilePath() );
        assertEquals(BASE_PATH + "/abcccccc/config/conquest/genesis.json", CfgAion.getGenesisFilePath() );

        // Invalid input
        assertEquals(1, cli.call(multiArgs[5], cfg));
        assertEquals(1, cli.call(multiArgs[6], cfg));
        assertEquals(1, cli.call(multiArgs[7], cfg));
        assertEquals(1, cli.call(multiArgs[8], cfg));
    }

    // Methods below taken from FileUtils class
    private static boolean copyRecursively(File src, File target)
    {
        if (src.isDirectory()) {
            return copyDirectoryContents(src, target);
        }
        else {
            try {
                Files.copy(src, target);
                return true;
            }
            catch (IOException e) {
                return false;
            }
        }
    }

    private static boolean copyDirectoryContents(File src, File target)
    {
        Preconditions.checkArgument(src.isDirectory(), "Source dir is not a directory: %s", src);

        // Don't delete symbolic link directories
        if (isSymbolicLink(src)) {
            return false;
        }

        target.mkdirs();
        Preconditions.checkArgument(target.isDirectory(), "Target dir is not a directory: %s", src);

        boolean success = true;
        for (File file : listFiles(src)) {
            success = copyRecursively(file, new File(target, file.getName())) && success;
        }
        return success;
    }

    private static boolean isSymbolicLink(File file)
    {
        try {
            File canonicalFile = file.getCanonicalFile();
            File absoluteFile = file.getAbsoluteFile();
            File parentFile = file.getParentFile();
            // a symbolic link has a different name between the canonical and absolute path
            return !canonicalFile.getName().equals(absoluteFile.getName()) ||
                    // or the canonical parent path is not the same as the file's parent path,
                    // provided the file has a parent path
                    parentFile != null && !parentFile.getCanonicalPath().equals(canonicalFile.getParent());
        }
        catch (IOException e) {
            // error on the side of caution
            return true;
        }
    }

    private static ImmutableList<File> listFiles(File dir)
    {
        File[] files = dir.listFiles();
        if (files == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(files);
    }

    private static boolean deleteRecursively(File file) {
        Path path = file.toPath();
        try {
            java.nio.file.Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    java.nio.file.Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                    return handleException(e);
                }

                private FileVisitResult handleException(final IOException e) {
                    // e.printStackTrace();
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
                    if (e != null)
                        return handleException(e);
                    java.nio.file.Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}