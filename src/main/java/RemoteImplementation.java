import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.io.ByteArrayDataOutput;
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
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class RemoteImplementation implements IODriver {

    Drive driveService = GoogleDriveApi.getDriveService();

    private static final String INODE_SEPARATOR = "/";

    static {
        try {
            IOManager.setIODriver(new RemoteImplementation());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String srcPath;

    public RemoteImplementation() throws IOException {
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
        String srcFolderId = "";
        String trgFolderId = "";
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                if(file.getName().equals(s)) {
                    srcFolderId = file.getId();
                    System.out.printf("Found folder: %s (%s)\n",
                            file.getName(), file.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file2 : result.getFiles()) {
                if(file2.getName().equals(s1)) {
                    trgFolderId = file2.getId();
                    System.out.printf("Found folder: %s (%s)\n",
                            file2.getName(), file2.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        File file3 = null;
        try {
            file3 = driveService.files().get(srcFolderId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file3.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
// Move the file to the new folder
        try {
            file3 = driveService.files().update(srcFolderId, null)
                    .setAddParents(trgFolderId)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void moveFile(String s, String s1) {
        String fileId = "";
        String folderId = "";
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                if(file.getName().equals(s)) {
                    fileId = file.getId();
                    System.out.printf("Found file: %s (%s)\n",
                            file.getName(), file.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file2 : result.getFiles()) {
                if(file2.getName().equals(s1)) {
                    folderId = file2.getId();
                    System.out.printf("Found file: %s (%s)\n",
                            file2.getName(), file2.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        File file3 = null;
        try {
            file3 = driveService.files().get(fileId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file3.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
// Move the file to the new folder
        try {
            file3 = driveService.files().update(fileId, null)
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

    private String createMimeType(String s){
        String extension = "";

        int i = s.lastIndexOf('.');
        int p = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));

        if (i > p) {
            extension = s.substring(i+1);
        }

        if(extension.equals("txt")){
            return "text/plain";
        }else if(extension.equals("html")){
            return "text/html";
        }else if(extension.equals("jpeg")){
            return "image/jpeg";
        }else if(extension.equals("png")){
            return "image/png";
        }else if(extension.equals("json")){
            return "application/vnd.google-apps.script+json";
        }else{
            return null;
        }
    }

    @Override
    public FileBuilder uploadFile(String s, String s1) {
        Path p = Path.of(s1);
        String type = createMimeType(p.getFileName().toString());
        System.out.println("TYPE: " + type);
        if(type.equals("application/vnd.google-apps.script+json")){
            File fileMetadata = new File();
            fileMetadata.setName(p.getFileName().toString());
            fileMetadata.setMimeType("application/vnd.google-apps.script+json");
            java.io.File filePath = new java.io.File(s1);
            FileContent mediaContent = new FileContent("text/plain", filePath);
            File file = null;
            try {
                file = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
                return new FileBuilder(new DirectoryBuilder(), file.getName(), file.getSize());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File ID: " + file.getId());
        }else {
            File fileMetadata = new File();
            fileMetadata.setName(p.getFileName().toString());
            java.io.File filePath = new java.io.File(s1);
            FileContent mediaContent = new FileContent(type, filePath);
            File file = null;
            try {
                file = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
                return new FileBuilder(new DirectoryBuilder(), file.getName(), file.getSize());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File ID: " + file.getId());
        }
        return null;
    }

    @Override
    public void downloadFile(String s, String s1) {
        String fileId = "";
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                if(file.getName().equals(s)) {
                    fileId = file.getId();
                    OutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        driveService.files().get(fileId)
                                .executeMediaAndDownloadTo(outputStream);
                        System.out.println(outputStream.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.printf("Found file: %s (%s)\n",
                            file.getName(), file.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

    }

    @Override
    public String readConfig(String s) {
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                if(file.getName().equals(s)) {
                    OutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        driveService.files().get(file.getId())
                                .executeMediaAndDownloadTo(outputStream);
                        System.out.println(outputStream.toString());
                        return outputStream.toString();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.printf("Found file: %s (%s)\n",
                            file.getName(), file.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return null;
    }

    @Override
    public void writeConfig(String s, String s1) {
        String fileId = "";
        String folderId = "";
        try {
            Files.createFile(Path.of("config.json"));
            PrintWriter pw = new PrintWriter("config.json");
            pw.append(s);
            pw.close();
            File fileMetadata = new File();
            fileMetadata.setName("config.json");
            java.io.File filePath = new java.io.File("config.json");
            FileContent mediaContent = new FileContent("text/plain", filePath);
            File file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + file.getId());
            Files.delete(Path.of("config.json"));

            //MOVE FILE
            fileId = file.getId();
            String pageToken = null;
            do {
                FileList result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file2 : result.getFiles()) {
                    if(file2.getName().equals(s1)){
                        folderId = file2.getId();
                    }
                    System.out.printf("Found file: %s (%s)\n",
                            file2.getName(), file2.getId());
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);


        } catch (IOException e) {
            e.printStackTrace();
        }
        File file3 = null;
        try {
            file3 = driveService.files().get(fileId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file3.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
// Move the file to the new folder
        try {
            file3 = driveService.files().update(fileId, null)
                    .setAddParents(folderId)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public @NotNull DirectoryBuilder initStorage() {
        File fileMetadata = new File();
        fileMetadata.setName(srcPath);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File root = null;
        try {
            root = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root.setMimeType("application/vnd.google-apps.folder");
        System.out.println(root.getMimeType());
        if(root.getMimeType() == "application/vnd.google-apps.folder"){
            DirectoryBuilder db = new DirectoryBuilder(null, DirectoryBuilder.ROOT_DIRECTORY);
            try {
                return (DirectoryBuilder) traverse(db, root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new IODriverException(
                    "Cannot initiate root on file!"
            );
        }
        return null;
    }


    private INodeBuilder traverse(DirectoryBuilder parent, File file) throws IOException {
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file2 : result.getFiles()) {
                if(file.getParents() == null){
                    break;
                }
                    if (file.getParents().equals(file.getParents()) && file.getParents()!=null) {
                        parent.addChild(new FileBuilder(
                                parent,
                                file2.getName(),
                                file2.getSize()

                        ));
                }else{
                    DirectoryBuilder db = new DirectoryBuilder(
                            parent,
                            file2.getName()
                    );
                    parent.addChild(db);
                    traverse(db, file2);
                }

            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return parent;
    }



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

}
