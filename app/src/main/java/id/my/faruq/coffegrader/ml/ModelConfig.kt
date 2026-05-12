package id.my.faruq.coffegrader.ml

/**
 * Konfigurasi model YOLO11n-seg best_float32.tflite.
 * Ubah konstanta di sini jika model diganti — tidak perlu menyentuh Engine/Decoder.
 */
object ModelConfig {

    /** Nama file di folder assets/ */
    const val MODEL_FILE_NAME = "best_float32.tflite"

    /** Ukuran input model (lebar = tinggi = 640 px) */
    const val INPUT_SIZE = 640

    /** Normalisasi: pixel / 255f */
    const val PIXEL_NORM = 255f

    /**
     * Output 0: [1, 57, 8400]
     * 57 = 4 (bbox xywh) + 21 (class scores) + 32 (mask coefficients)
     */
    const val NUM_PREDICTIONS = 8400
    const val BBOX_DIMS      = 4
    const val MASK_COEFF_DIM = 32

    /**
     * Output 1: [1, 160, 160, 32] — proto masks
     * Tidak dipakai untuk grading, namun disediakan untuk ekstensi segmentasi.
     */
    const val PROTO_SIZE = 160

    /** Threshold confidence deteksi */
    const val CONFIDENCE_THRESHOLD = 0.5f

    /** Threshold IoU untuk Non-Maximum Suppression */
    const val IOU_THRESHOLD = 0.45f
}
