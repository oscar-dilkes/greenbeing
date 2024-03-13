package org.greenbeing.data;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Paths;

// Uploads image to S3 bucket
public class S3ImageUploader {
  private static final String BUCKET_NAME = "";
  private static final String ACCESS_KEY_ID = "";
  private static final String SECRET_ACCESS_KEY = "";
  private static final Region REGION = Region.EU_WEST_2;

  public static String imageUploader(String key, String filePath) {
    // Set up AWS credentials and S3 client
    AwsBasicCredentials awsCreds = AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
    S3Client s3 = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .region(REGION)
            .build();

    // Upload image
    PutObjectResponse response = s3.putObject(PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build(),
            Paths.get(filePath));

    if (response.sdkHttpResponse().isSuccessful()) {

      // Construct the URL of the uploaded image
      return String.format("https://%s.s3.%s.amazonaws.com/%s", BUCKET_NAME, REGION.toString(), key);
    } else {
      // Return nothing if unsuccessful
      return "";
    }
  }
}