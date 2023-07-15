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

import java.io.FileNotFoundException;
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

public class EncryptActivity extends AppCompatActivity {

    // Constants for file names, keys, and initialization vectors

    private static final String FILE_NAME_ENC = "image_enc";
    private static final String FILE_NAME_DEC = "image_dec.jpg";
    private static final String key = "PDY80oOtPHNYz1FG7";
    private static final String specString = "yoe6Nd84MOZCzbbO";
    // Variables to store initial picker information

    private static String inputPDFUri = "";
    private static String initialpickernamePDFIn = "";
    private static String initialpickernamePDFOut = "";
    private static String initialsize = "";
    private static int initialsizeformat = 0;

    // Utility class to format file sizes

    public static class HumanizeUtils {
        public static String formatAsFileSize(long size) {
            if (size == 0) {
                return "0 B";
            }
            int precision = (int) Math.floor(Math.log(size) / Math.log(1024));
            double formattedSize = size / Math.pow(1024, precision);
            String[] suffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
            return String.format("%.2f %s", formattedSize, suffixes[precision]);
        }
    }
    // Constants for encryption keys and salt

    private static final String secretKey = "tK5UTui+DPh8lIlBxya5XVsmeDCoUl6vHhdIESMB6sQ=";
    private static final String salt = "QWlGNHNhMTJTQWZ2bGhpV3U="; // base64 decode => AiF4sa12SAfvlhiWu
    private static final String iv = "bVQzNFNhRkQ1Njc4UUFaWA=="; // base64 decode => mT34SaFD5678QAZX
    private static final int cur_buffer_size = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt);
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
                        Toast.makeText(EncryptActivity.this, "You should accept permission", Toast.LENGTH_SHORT).show();
                    }
                })
                .check();
        // Get the root directory path

        String root = Environment.getExternalStorageDirectory().toString();
        // Get references to the buttons in the layout

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button buttonOpenFilePDFDec = findViewById(R.id.btnOpenFilePDFDec);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button buttonSaveFilePDFEnc = findViewById(R.id.btnBeginEncryptPDF);
        // Set click listeners for the buttons

        buttonOpenFilePDFDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open a PDF file for decryption

                openPDFFileDec(Uri.parse(Environment.getExternalStorageDirectory().toString()));
            }
        });
        buttonSaveFilePDFEnc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the encrypted PDF file

                savePDFFileEnc(Uri.parse(Environment.getExternalStorageDirectory().toString()));
            }
        });


    }
    // Open a PDF file for decryption

    private void openPDFFileDec(Uri pickerInitialUri) {
        Intent intentOpenPDFFileDec = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intentOpenPDFFileDec.addCategory(Intent.CATEGORY_OPENABLE);
        intentOpenPDFFileDec.setType("application/pdf");
        intentOpenPDFFileDec.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intentOpenPDFFileDec, 31);
    }
    // Save the encrypted PDF file

    private void savePDFFileEnc(Uri pickerInitialUri) {
        Intent intentSavePDFFileEnc = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intentSavePDFFileEnc.addCategory(Intent.CATEGORY_OPENABLE);
        intentSavePDFFileEnc.setType("*/*");
        intentSavePDFFileEnc.putExtra(Intent.EXTRA_TITLE, initialpickernamePDFIn + "_enc");
        intentSavePDFFileEnc.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intentSavePDFFileEnc, 32);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 31 && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri31 = resultData.getData();
                // Extract metadata from the selected PDF file
                dumpImageMetaData(uri31);
                inputPDFUri = uri31.toString();

                // Enable the encryption button and display the file name and size
                Button buttonEncryptSavePDFstate = findViewById(R.id.btnBeginEncryptPDF);
                TextView filename = findViewById(R.id.filenamePDFTextViewEnc);
                TextView size = findViewById(R.id.filesizePDFTextViewEnc);
                buttonEncryptSavePDFstate.setEnabled(true);
                filename.setText(initialpickernamePDFIn);
                String sizeformat = HumanizeUtils.formatAsFileSize(initialsizeformat);
                size.setText(sizeformat);
            }
        } else if (requestCode == 32 && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri32 = resultData.getData();
                try {
                    // Encrypt the selected PDF file and save it

                    encryptPDFToFile(
                            key,
                            specString,
                            Uri.parse(inputPDFUri),
                            uri32
                    );

                    // Display a toast message indicating successful encryption
                    Toast.makeText(this, "Encrypted!", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // Encrypt the PDF file and save it

    private void encryptPDFToFile(String keyStr, String spec, Uri uri31, Uri uri32) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, InvalidAlgorithmParameterException, FileNotFoundException, NoSuchPaddingException {
        // Open input and output streams for the source and destination files
        InputStream input = getContentResolver().openInputStream(uri31);
        OutputStream output = getContentResolver().openOutputStream(uri32);
        try {
            // Create an initialization vector (IV) parameter specification
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.decode(iv, Base64.DEFAULT));

            // Generate a secret key from the provided key material
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec pbeKeySpec = new PBEKeySpec(secretKey.toCharArray(), Base64.decode(salt, Base64.DEFAULT), 10000, 256);
            SecretKeySpec tmp = new SecretKeySpec(factory.generateSecret(pbeKeySpec).getEncoded(), "AES");

            // Create an AES cipher instance with CBC mode and PKCS7 padding
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, tmp, ivParameterSpec);

            // Create a CipherOutputStream to perform encryption and write to the output stream
            output = new CipherOutputStream(output, cipher);
            byte[] buffer = new byte[cur_buffer_size];
            int bytesRead;
            // Read data from the input stream, encrypt it, and write to the output stream
            while ((bytesRead = input.read(buffer)) > 0)
                output.write(buffer, 0, bytesRead);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } finally {
            // Close the output stream to flush and release resources
            if (output != null) {
                output.flush();
                output.close();
            }
        }
    }

    // Extract metadata from the selected PDF file

    @SuppressLint("Range")
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

}

