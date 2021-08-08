package com.sungbin.cameracapture

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.sungbin.cameracapture.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val REQUEST_CAMERA_CAPTURE = 1

    private lateinit var curPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setPemission() // 권한 체크 with 테드 퍼미션

        binding.cameraBtn.setOnClickListener {
            takeCapture()
        }
    }

    private fun takeCapture() {                 // 카메라 앱 실행
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.sungbin.cameracapture.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File? {
        val timestamp: String = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_",".jpg", storageDir)
            .apply { curPhotoPath = absolutePath }
    }

    private fun setPemission() {
        val permission = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@MainActivity, "권한이 허용 되었습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Toast.makeText(this@MainActivity, "권한이 거부 되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        TedPermission.with(this)
            .setPermissionListener(permission)
            .setRationaleMessage("권한 허용이 필요합니다.")
            .setDeniedMessage("권한을 거부 하였습니다. [앱 설정] -> [권한] 항목에서 허용 해주세요.")
            .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA)
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAMERA_CAPTURE && resultCode == RESULT_OK) {
            val bitmap: Bitmap
            val file = File(curPhotoPath)
            Log.d("MainActvity", "ImagePath : ${curPhotoPath}")
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
            } else {
                val decode = ImageDecoder.createSource(this.contentResolver, Uri.fromFile(file))
                bitmap = ImageDecoder.decodeBitmap(decode)
            }
            binding.profileImage.setImageBitmap(bitmap)

            savePhoto(bitmap)
        }
    }

    private fun savePhoto(bitmap: Bitmap) {
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/"
        val timestamp: String = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val fileName = "${timestamp}.jpeg"
        val folder = File(folderPath)
        if(!folder.isDirectory){
            folder.mkdirs()
        }

        val out = FileOutputStream(folderPath + fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
}