package com.example.PdfEncryptionAndDecrytion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptActivity extends AppCompatActivity {

    // Constants for file names, keys, and initialization vectors

    private static final String FILE_NAME_ENC = "image_enc";
    private static final String FILE_NAME_DEC = "image_dec.jpg";
    private static final String key = "PDY80oOtPHNYz1FG7";
    private static final String specString = "yoe6Nd84MOZCzbbO";
    private static String inputPDFUri = "";
    private static String outputPDFUri = "";
    private static String initialpickernamePDFIn = "";
    private static String initialpickernamePDFOut = "";
    private static String initialsize = "";
    private static int initialsizeformat = 0;
    // Utility class to format file sizes

    public static class HumanizeUtils {
        public static String formatAsFileSize(File file) {
            return String.valueOf(file.length());
        }

        public static String formatAsFileSize(int size) {
            return formatAsFileSize((long) size);
        }

        public static String formatAsFileSize(long size) {
            double value = size;
            String[] suffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
            int suffixIndex = 0;
            while (value > 1024 && suffixIndex < suffixes.length - 1) {
                value /= 1024;
                suffixIndex++;
            }
            return String.format("%.2f %s", value, suffixes[suffixIndex]);
        }
    }
    // Constants for encryption keys and salt

    private static final String secretKey = "tK5UTui+DPh8lIlBxya5XVsmeDCoUl6vHhdIESMB6sQ=";
    private static final String salt = "QWlGNHNhMTJTQWZ2bGhpV3U="; // base64 decode => AiF4sa12SAfvlhiWu
    private static final String iv = "bVQzNFNhRkQ1Njc4UUFaWA=="; // base64 decode => mT34SaFD5678QAZX
    private static final int cur_buffer_size = 1024;

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt);
        // Request necessary permissions using Dexter library

        Dexter.withActivity(this)
                .withPermissions(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        // Permissions granted or denied, handle accordingly

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        Toast.makeText(DecryptActivity.this, "You should accept permission", Toast.LENGTH_SHORT).show();
                    }
                })
                .check();
        // Get the root directory path

        String root = Environment.getExternalStorageDirectory().toString();
        // Get references to the buttons in the layout

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button buttonOpenFilePDFEnc = findViewById(R.id.btnOpenFilePDFEncrypted);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button buttonSaveFilePDFDec = findViewById(R.id.btnBeginDecryptPDF);
        // Set click listeners for the buttons

        buttonOpenFilePDFEnc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open an encrypted PDF file

                openPDFFileEnc(Uri.parse(root));
            }
        });
        buttonSaveFilePDFDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the decrypted PDF file

                savePDFFileDec(Uri.parse(root));
            }
        });
    }
    // Open an encrypted PDF file

    private void openPDFFileEnc(Uri pickerInitialUri) {
        Intent intentOpenPDFFileEnc = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intentOpenPDFFileEnc.addCategory(Intent.CATEGORY_OPENABLE);
        intentOpenPDFFileEnc.setType("*/*");
        intentOpenPDFFileEnc.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intentOpenPDFFileEnc, 33);
    }
    // Save the decrypted PDF file

    private void savePDFFileDec(Uri pickerInitialUri) {
        Intent intentSavePDFFileDec = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intentSavePDFFileDec.addCategory(Intent.CATEGORY_OPENABLE);
        intentSavePDFFileDec.setType("application/pdf");
        intentSavePDFFileDec.putExtra(Intent.EXTRA_TITLE, initialpickernamePDFIn + ".pdf");
        intentSavePDFFileDec.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intentSavePDFFileDec, 34);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 33 && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri33 = resultData.getData();
                // Extract metadata from the selected PDF file
                dumpImageMetaData(uri33);
                inputPDFUri = uri33.toString();

                // Enable the decryption button and display the file name and size
                Button buttonDecryptSavePDFstate = findViewById(R.id.btnBeginDecryptPDF);
                TextView filename = findViewById(R.id.filenamePDFTextViewDec);
                TextView size = findViewById(R.id.filesizePDFTextViewDec);
                buttonDecryptSavePDFstate.setEnabled(true);
                filename.setText(initialpickernamePDFIn);
                String sizeformat = HumanizeUtils.formatAsFileSize(initialsizeformat);
                size.setText(sizeformat);
            }
        } else if (requestCode == 34 && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri34 = resultData.getData();
                try {
                    // Decrypt the selected PDF file
                    decryptPDFToFile(
                            key,
                            specString,
                            Uri.parse(inputPDFUri),
                            uri34
                    );

                    // Display a toast message indicating successful decryption
                    Toast.makeText(this, "Decrypted!", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @SuppressLint("Range")
    // Extract metadata from the selected PDF file
    private void dumpImageMetaData(Uri uri) {
        // Query the content resolver to retrieve metadata of the selected PDF file
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    // Retrieve the display name of the PDF file
                    String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    Log.i("TAG", "Display Name: " + displayName);

                    // Extract the file name without the extension
                    String resultsbstr = displayName.substring(0, displayName.length() - 4);
                    initialpickernamePDFIn = resultsbstr;
                    Log.i("TAG", "Display Name Disunat: " + resultsbstr);

                    // Retrieve the size of the PDF file
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    String size = cursor.isNull(sizeIndex) ? "Unknown" : cursor.getString(sizeIndex);
                    Log.i("TAG", "Size: " + size);

                    // Store the size in the initialsize variable and format it as an integer
                    initialsize = size;
                    initialsizeformat = Integer.parseInt(size);
                }
            } finally {
                // Close the cursor to release resources
                cursor.close();
            }
        }
    }

    // Decrypt the PDF file to a new file
    @SuppressLint("Range")
    private void decryptPDFToFile(String keyStr, String spec, Uri uri33, Uri uri34) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, InvalidAlgorithmParameterException {
        // Open an input stream to read the encrypted PDF file
        InputStream input = getContentResolver().openInputStream(uri33);
        // Open an output stream to write the decrypted PDF file
        OutputStream output = getContentResolver().openOutputStream(uri34);
        try {
            // Initialize the IV parameter specification using the decoded IV string
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.decode(iv, Base64.DEFAULT));

            // Generate the secret key using the PBKDF2 key derivation algorithm
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec pbeKeySpec = new PBEKeySpec(secretKey.toCharArray(), Base64.decode(salt, Base64.DEFAULT), 10000, 256);
            SecretKeySpec tmp = new SecretKeySpec(factory.generateSecret(pbeKeySpec).getEncoded(), "AES");

            // Create a cipher object for decryption using AES/CBC/PKCS7Padding algorithm
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            // Initialize the cipher object with the secret key and IV
            cipher.init(Cipher.DECRYPT_MODE, tmp, ivParameterSpec);

            // Create a cipher output stream to write the decrypted data
            output = new CipherOutputStream(output, cipher);

            // Read data from the input stream, decrypt it, and write to the output stream
            byte[] buffer = new byte[cur_buffer_size];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) > 0)
                output.write(buffer, 0, bytesRead);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                // Flush and close the output stream
                output.flush();
                output.close();
            }
        }
    }


}
