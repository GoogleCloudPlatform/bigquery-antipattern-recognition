package com.google.zetasql.toolkit.antipattern.util;


import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.zetasql.toolkit.antipattern.cmd.output.GCSFileOutputWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCSHelper {
  private static final Logger logger = LoggerFactory.getLogger(GCSHelper.class);

  public final static String GCS_DELIMITER = "/";
  public final static String GCS_PATH_PREFIX = "gs://";

  private String bucketName;

  private final static Storage storage = StorageOptions.newBuilder().build().getService();

  public static Boolean isGCSPath(String filePath) {
    return filePath.startsWith(GCS_PATH_PREFIX);
  }

  public ArrayList<String> getListOfFilesInGCSPath(String folderPath) {
    setBucketName(folderPath);
    String prefix = folderPath.replace(GCS_PATH_PREFIX+this.bucketName+GCS_DELIMITER, "") + GCS_DELIMITER;
    return getListOfFilesInGCSPrefix(bucketName, prefix);
  }

  private static ArrayList<String> getListOfFilesInGCSPrefix(String bucketName, String prefix) {
    ArrayList gcsFileList = new ArrayList();

    Page<Blob> blobs =
        storage.list(
            bucketName,
            Storage.BlobListOption.prefix(prefix),
            Storage.BlobListOption.currentDirectory());

    for (Blob blob : blobs.iterateAll()) {
      String blobName = blob.getName();
      if (!blobName.equals(prefix)) {
        gcsFileList.add(GCS_PATH_PREFIX + bucketName + GCS_DELIMITER + blobName);
      }
    }
    return gcsFileList;
  }

  public InputQuery getInputQueryFromGCSPath(String gcsPath) {
    if (bucketName == null) {
      setBucketName(gcsPath);
    }
    String filename = gcsPath.replace(GCS_PATH_PREFIX+this.bucketName+GCS_DELIMITER, "");
    Blob blob = storage.get(bucketName, filename);
    String fileContent = new String(blob.getContent());
    return new InputQuery(fileContent, gcsPath);
  }

  public void writeToGCS(String filePath, String fileContent) {
    try {
      setBucketName(filePath);
      String filename = filePath.replace(GCS_PATH_PREFIX+this.bucketName+GCS_DELIMITER, "");
      BlobId blobId = BlobId.of(bucketName, filename);
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
      Blob blob = storage.create(blobInfo, fileContent.getBytes(UTF_8));
    } catch (Exception e) {
      logger.error("Error when writing to GCD: " + filePath);
      logger.error(e.getMessage());
      logger.error(e.getStackTrace().toString());
    }

  }

  private void setBucketName(String gcsPath) {
    String trimFolderPathStr = gcsPath.replace(GCS_PATH_PREFIX, "");
    List<String> list = new ArrayList(Arrays.asList(trimFolderPathStr.split(GCS_DELIMITER)));
    this.bucketName = list.get(0);
  }

}
