package com.example.drawableapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // TODO: Preciso mexer nas permissões

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->

        if(result.resultCode == RESULT_OK && result.data!=null){
            val imgBackground: ImageView = findViewById(R.id.imgBackground)

            imgBackground.setImageURI(result.data?.data)
        }
    }

// region Comment about how put one permission
//    private val btnFileResultLauncher : ActivityResultLauncher<String> =
//        registerForActivityResult(
//            ActivityResultContracts.RequestPermission()){
//                isGranted ->
//
//                if(isGranted){
//                    Toast.makeText(this, "Permission granted for external storage", Toast.LENGTH_SHORT).show()
//                }else{
//                    Toast.makeText(this, "Permission denied for external storage", Toast.LENGTH_SHORT).show()
//                }
//            }
// endregion

// region P. Function btnFileResultLauncher
    private val btnFileResultLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->

                permissions.entries.forEach{
                    val permissionName = it.key
                    val isGranted = it.value

                    if (isGranted ) {
                        Toast.makeText(
                            this@MainActivity,
                            "Permission granted now you can read the storage files.",
                            Toast.LENGTH_LONG
                        ).show()
                        val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    } else {

                        if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE)
                            Toast.makeText(
                                this@MainActivity,
                                "Oops you just denied the permission.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
        }
// endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drwView)

        val btnAddImage: ImageButton = findViewById(R.id.btnAddImage)

        drawingView?.setSizeForBrush(20f)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.layoutPaintColors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        val btnBrush: ImageButton = findViewById(R.id.btnBrush)

        btnBrush.setOnClickListener {
            brushSizeChooserDialog()
        }

        val btnReturn: ImageButton = findViewById(R.id.btnReturn)

        btnReturn.setOnClickListener {
            drawingView?.onClickUndo()
        }

        btnAddImage.setOnClickListener {
            requestStoragePermission()
        }

        val btnSave: ImageButton = findViewById(R.id.btnSave)

        btnSave.setOnClickListener {
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.frmLayout)
                    val myBitmap: Bitmap = getBitMapFromView(flDrawingView)
                    saveBitmapFile(myBitmap)
                }
            }
        }
        
    }

// region P. Function isReadStorageAllowed
    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

// endregion

// region P. Function requestStoragePermission()
    private fun requestStoragePermission(){
        if( ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){

            showRationalDialog("Drawing App", "Drawing App needs to Access Your External Storage")

        }else{
            btnFileResultLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
// endregion

// region P. Function showRationalDialog
    private fun showRationalDialog(tittle: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setTitle(tittle).setMessage(message).setPositiveButton("Cancel"){
            dialog, _-> dialog.dismiss()
        }

        builder.create().show()
    }
// endregion

// region P. Function brushSizeChooserDialog
    private fun brushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val btnSmall: ImageButton = brushDialog.findViewById(R.id.imgBrushSmall)
        val btnMedium: ImageButton = brushDialog.findViewById(R.id.imgBrushMedium)
        val btnHigh: ImageButton = brushDialog.findViewById(R.id.imgBrushHigh)

        btnSmall.setOnClickListener{
            drawingView?.setSizeForBrush(10f)
            brushDialog.dismiss()
        }

        btnMedium.setOnClickListener{
            drawingView?.setSizeForBrush(20f)
            brushDialog.dismiss()
        }

        btnHigh.setOnClickListener{
            drawingView?.setSizeForBrush(30f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }
// endregion

// region Function paintClicked()
    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imgButton = view as ImageButton
            val colorTag = imgButton.tag.toString()

            drawingView?.setColor(colorTag)

            imgButton!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }
// endregion

// region P. Function getBitMapFromView
    private fun getBitMapFromView(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height,
            Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap

    }
// endregion

// region P. S. Function saveBitmapFile
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                          + File.separator + "DrawableApp_" + System.currentTimeMillis() / 1000 + ".jpg")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()) {
                            shareImage(result)
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully: $result",
                                Toast.LENGTH_LONG
                            ).show()

                        }else{
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file",
                                Toast.LENGTH_LONG)
                                .show()
                        }
                    }

                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }
// endregion

// region P. Function showProgressDialog
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

// endregion

// region P. Function cancelProgressDialog
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
// endregion

// region P. Function shareImage
    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(), null){
            path, uri ->

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)

            shareIntent.type = "image/png"

            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

// endregion

 }
