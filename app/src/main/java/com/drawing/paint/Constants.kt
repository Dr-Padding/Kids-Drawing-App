package com.drawing.paint

object Constants {
    const val tag = "BottomSheetDialog"
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
    const val STORAGE_PERMISSION_CODE = 1
    const val STORAGE_REQUEST_CODE = 2
    const val REQUEST_CODE_PERMISSIONS = 123
    val CAMERA_PERMISSION = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    val UPLOAD_PERMISSION = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
}