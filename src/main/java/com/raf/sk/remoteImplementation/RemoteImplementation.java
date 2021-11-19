package com.raf.sk.remoteImplementation;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.raf.sk.specification.builders.DirectoryBuilder;
import com.raf.sk.specification.builders.FileBuilder;
import com.raf.sk.specification.builders.INodeBuilder;
import com.raf.sk.specification.exceptions.IODriverException;
import com.raf.sk.specification.io.IODriver;
import com.raf.sk.specification.io.IOManager;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

public class RemoteImplementation implements IODriver {

    private static final String INODE_SEPARATOR = "/";

    static {
        IOManager.setIODriver(new RemoteImplementation());
    }

    private String srcPath;

    private Drive driveService;

    public RemoteImplementation() {
        try {
            this.driveService = GoogleDriveApi.getDriveService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void makeDirectory(String s) {
        File fileMetadata = new File();
        fileMetadata.setName(s);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = null;
        try {
            file = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId());
    }

    @Override
    public void makeFile(String s) {
        File fileMetadata = new File();
        Path path = Path.of(s);
        fileMetadata.setName(path.getFileName().toString());
        java.io.File filePath = new java.io.File(s);
        FileContent mediaContent = new FileContent(fileMetadata.getFileExtension(), filePath);
        File file = null;
        try {
            file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                    .setName(path.getFileName().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File Name: " + file.getName());
    }


    @Override
    public void deleteDirectory(String s) {
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                if (file.getName().equals(s)) {
                    try {
                        driveService.files().delete(file.getId()).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void deleteFile(String s) {
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                if (file.getName().equals(s)) {
                    try {
                        driveService.files().delete(file.getId()).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void moveDirectory(String s, String s1) {

    }

    @Override
    public void moveFile(String s, String s1) {
        String fileId = null;
        String folderId = null;
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                if (file.getName().equals(s)) {
                    fileId = file.getId();
                    System.out.println("FILE NAME: " + file.getName());
                }
            }
        }

        for (File file : files) {
            if (file.getName().equals(s1)) {
                folderId = file.getId();
                System.out.println("FOLDER NAME: " + file.getName());
            }
        }

// Retrieve the existing parents to remove
        File file2 = null;
        try {
            file2 = driveService.files().get(fileId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file2.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
// Move the file to the new folder
        try {
            file2 = driveService.files().update(fileId, null)
                    .setAddParents(folderId)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadDirectory(String s, String s1) {

    }

    @Override
    public FileBuilder uploadFile(String s, String s1) {
        return null;
    }

    @Override
    public void downloadFile(String s, String s1) {

        FileList result = null;
        try {
            result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                if (file.getName().equals(s)) {
                    String fileId = file.getId();
                    OutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    @Override
    public String readConfig(String s) {
        return null;
    }

    @Override
    public void writeConfig(String s, String s1) {

    }

    // #TODO ovo mora da se implementira
    @Override
    public @NotNull DirectoryBuilder initStorage() {
        return null;
    }


    /**
     *
     *
     *
     *
     *
     *
     *
     * #TODO preporučujem ti da koristiš ove metode ispod, samo da ih prilagodiš za Google Drive
     *
     *
     *
     *
     *
     *
     *
     *
     */

    /**
     * Vraća sistemski fajl separator.
     *
     * @return Sistemski fajl separator.
     */
    private String getSeparator() {
        return System.getProperty("file.separator");
    }

    /**
     * Pretvara path koji je dala aplikacija u apsolutni path za korisničko okruženje. Može se pozivati SAMO nakon
     * inicijalnog čitanja direktorijuma.
     *
     * @param appPath Path iz aplikacije.
     * @return Krajnji path.
     */
    private String resolvePath(String appPath) {
        if (srcPath == null)
            throw new IODriverException(
                    "Programming error: you cannot call resolvePath() before reading the config!"
            );

        String sep = getSeparator();
        if (!srcPath.endsWith(sep)) {
            srcPath = srcPath + sep;
        }

        if (appPath.startsWith(INODE_SEPARATOR))
            appPath = appPath.substring(1);

        if (sep.equals("\\")) sep = "\\\\";
        appPath = appPath.replaceAll(INODE_SEPARATOR, sep);
        return srcPath + appPath;
    }

    /**
     * Prolazi kroz direktorijum rekurzivno i pravi DirectoryBuilder.
     *
     * @param parent DirectoryBuilder koji se gradi.
     * @param file   Korenski File objekat.
     * @return Vraća korensko DirectoryBuilder stablo.
     */
    private INodeBuilder traverse(DirectoryBuilder parent, java.io.File file) {
        for (java.io.File f : file.listFiles()) {
            if (f.isFile()) {
                parent.addChild(new FileBuilder(
                        parent,
                        f.getName(),
                        f.length()
                ));
            } else {
                DirectoryBuilder db = new DirectoryBuilder(
                        parent,
                        f.getName()
                );
                parent.addChild(db);
                traverse(db, f);
            }
        }
        return parent;
    }
}
