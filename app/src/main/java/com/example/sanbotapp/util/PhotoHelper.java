package com.example.sanbotapp.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper para gestionar la captura y selección de fotos.
 * Elimina la duplicidad de código entre WelcomeActivity y AjustesActivity.
 */
public class PhotoHelper {

    public interface PhotoCallback {
        void onPhotoReady(String ruta);
        void onPermissionDenied();
    }

    private final Activity activity;
    private final PhotoCallback callback;
    private Uri currentPhotoUri;

    public PhotoHelper(Activity activity, PhotoCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, AppConstants.CAMERA_PERMISSION_REQUEST);
        } else {
            iniciarCamara();
        }
    }

    public void checkStoragePermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, AppConstants.STORAGE_PERMISSION_REQUEST);
            } else {
                lanzarSelectorGaleria();
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AppConstants.STORAGE_PERMISSION_REQUEST);
            } else {
                lanzarSelectorGaleria();
            }
        }
    }

    private void iniciarCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = crearArchivoFoto();
            } catch (IOException ex) {
                Toast.makeText(activity, "Error creando archivo", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(activity, "com.example.sanbotapp.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                activity.startActivityForResult(intent, AppConstants.REQUEST_CAMERA);
            }
        }
    }

    private void lanzarSelectorGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, AppConstants.REQUEST_GALLERY);
    }

    public String handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return null;

        if (requestCode == AppConstants.REQUEST_CAMERA) {
            if (currentPhotoUri != null) {
                String ruta = activity.getExternalFilesDir(null) + "/avatar_usuario.jpg";
                callback.onPhotoReady(ruta);
                return ruta;
            }
        } else if (requestCode == AppConstants.REQUEST_GALLERY && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                try {
                    String ruta = copiarFotoAAlmacenamiento(selectedImage);
                    callback.onPhotoReady(ruta);
                    return ruta;
                } catch (IOException e) {
                    Toast.makeText(activity, "Error al copiar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return null;
    }

    public void handlePermissionsResult(int requestCode, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == AppConstants.CAMERA_PERMISSION_REQUEST) {
                iniciarCamara();
            } else if (requestCode == AppConstants.STORAGE_PERMISSION_REQUEST) {
                lanzarSelectorGaleria();
            }
        } else {
            callback.onPermissionDenied();
        }
    }

    private File crearArchivoFoto() throws IOException {
        File storageDir = activity.getExternalFilesDir(null);
        return new File(storageDir, "avatar_usuario.jpg");
    }

    private String copiarFotoAAlmacenamiento(Uri uri) throws IOException {
        InputStream is = activity.getContentResolver().openInputStream(uri);
        File storageDir = activity.getExternalFilesDir(null);
        File photoFile = new File(storageDir, "avatar_usuario.jpg");
        FileOutputStream fos = new FileOutputStream(photoFile);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        is.close();
        return photoFile.getAbsolutePath();
    }

    public static void mostrarFotoDesdeRuta(String ruta, ImageView iv) {
        if (ruta != null && !ruta.isEmpty()) {
            File imgFile = new File(ruta);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                iv.setImageBitmap(myBitmap);
            }
        }
    }
}
