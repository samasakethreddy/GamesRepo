package com.Games.deployment.service;

import com.Games.deployment.entity.GameMetadata;
import com.Games.deployment.repository.GameRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GameService {
    private final AmazonS3 s3Client;
    private final GameRepository gameRepository;
    private final String bucketName;

    @Autowired
    public GameService(AmazonS3 s3Client, GameRepository gameRepository, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.gameRepository = gameRepository;
        this.bucketName = bucketName;
    }

    public void processAndUploadGame(MultipartFile gameFile, String gameName, MultipartFile titleImage, List<String> tags) throws IOException {
        String gameFolderUrl = uploadAndExtractGame(gameFile, gameName);
        String titleImageUrl = uploadFile(titleImage, "games/" + gameName + "/title");
        GameMetadata gameMetadata = GameMetadata.builder()
                .gameName(gameName)
                .folderUrl(gameFolderUrl)
                .titleImageUrl(titleImageUrl)
                .tags(tags)
                .build();
        gameRepository.save(gameMetadata);
    }

    private String uploadAndExtractGame(MultipartFile gameFile, String gameName) throws IOException {
        File tempZipFile = File.createTempFile("game", ".zip");
        gameFile.transferTo(tempZipFile);

        File extractDir = new File(tempZipFile.getParent(), gameName);
        extractDir.mkdirs();
        unzipFile(tempZipFile, extractDir);

        String folderUrl = uploadDirectoryToS3(extractDir, "games/" + gameName);
        tempZipFile.delete();
        deleteDirectory(extractDir);

        return folderUrl;
    }

    private String uploadFile(MultipartFile file, String s3Path) throws IOException {
        String fileName = s3Path + "/" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        s3Client.putObject(bucketName, fileName, file.getInputStream(), metadata);
        return s3Client.getUrl(bucketName, fileName).toString();
    }

    private String uploadDirectoryToS3(File dir, String s3Path) {
        for (File file : dir.listFiles()) {
            String key = s3Path + "/" + file.getName();

            if (file.isDirectory()) {
                // Recursively upload subdirectories
                uploadDirectoryToS3(file, key);
            } else {
                // Upload file to S3
                s3Client.putObject(bucketName, key, file);
            }
        }

        // Return the base folder URL
        return s3Client.getUrl(bucketName, s3Path).toString();
    }


    public void unzipFile(File zipFile, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();  // Ensure the output directory exists
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(outputDir, entry.getName());

                // Prevent Zip Slip Vulnerability
                String canonicalPath = newFile.getCanonicalPath();
                if (!canonicalPath.startsWith(outputDir.getCanonicalPath() + File.separator)) {
                    throw new IOException("Zip entry is trying to escape the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    if (entry.getSize() == 0) {
                        System.out.println("Skipping empty file: " + entry.getName());
                        continue;  // Ignore zero-byte files
                    }

                    newFile.getParentFile().mkdirs();  // Ensure parent directories exist
                    try (FileOutputStream fos = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }


    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}

