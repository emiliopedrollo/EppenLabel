package br.nom.pedrollo.emilio.eppenlabel

import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import br.nom.pedrollo.emilio.eppenlabel.DataContract.DataDbHelper
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName
    private var barcodeView: DecoratedBarcodeView? = null
    private var beepManager: BeepManager? = null
    private var lastText: String? = null
    private var isShowingPopUp: Boolean = false
    private var DbHelper: DataDbHelper? = null
    private var Database: SQLiteDatabase? = null
    private var isEditingExistingEntry: Boolean = false

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            // Prevent duplicate scans
            if (isShowingPopUp) {
                return
            }

            lastText = result.text

            val data = getDataFromCode(result.text)

            beepManager!!.playBeepSoundAndVibrate()

            if (data !== null) {
                (findViewById(R.id.info_viewer_code) as TextView).text = data.code
                (findViewById(R.id.info_viewer_text) as TextView).text = data.text
                showViewerFrame()
            } else {
                (findViewById(R.id.info_editor_code) as TextView).text = result.text
                (findViewById(R.id.info_editor_text) as TextView).text = ""
                showEditorFrame()
            }

//            barcodeView!!.setStatusText(result.text)

            //Added preview of scanned barcode
            val imageView = findViewById(R.id.barcodePreview) as ImageView
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW))
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        barcodeView = findViewById(R.id.barcode_scanner) as DecoratedBarcodeView
        barcodeView!!.decodeContinuous(callback)

        beepManager = BeepManager(this)

        DbHelper = DataDbHelper(this)
        Database = DbHelper!!.writableDatabase
    }

    override fun onDestroy() {
        DbHelper?.close()
        super.onDestroy()
    }

    fun getDataFromCode(code: String): DataContract.Data? {
        val projection = arrayOf(
                DataContract.Data._ID,
                DataContract.Data.COLUMN_NAME_CODE,
                DataContract.Data.COLUMN_NAME_TEXT)

        val selection: String = DataContract.Data.COLUMN_NAME_CODE + " = ?"
        val args = arrayOf(code)

        val cursor = Database!!.query(
                DataContract.Data.TABLE_NAME,
                projection,
                selection,
                args,
                null,
                null,
                null
        )

        var data: DataContract.Data? = null

        if (cursor.count > 0) {
            cursor.moveToFirst()
            val text = cursor.getString(cursor.getColumnIndexOrThrow(DataContract.Data.COLUMN_NAME_TEXT))
            data = DataContract.Data(code, text)
        }

        cursor.close()
        return data

    }

    fun setDataToCode(data: DataContract.Data) {
        val values = ContentValues()
        values.put(DataContract.Data.COLUMN_NAME_CODE, data.code)
        values.put(DataContract.Data.COLUMN_NAME_TEXT, data.text)

        Database!!.replace(DataContract.Data.TABLE_NAME, null, values)
    }

    class InfoFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val view = inflater!!.inflate(R.layout.info_fragment, container, false)

//            var code_view = view.findViewById(R.id.info_viewer_code) as TextView
//            code_view.setText("101")
//
//            var text_view = view.findViewById(R.id.info_viewer_code) as TextView
//            text_view.setText("TESTE")

            return view
        }
    }

    class EditFragment : Fragment() {
        private var toast: String? = null

//        override fun onActivityCreated(savedInstanceState: Bundle?) {
//            super.onActivityCreated(savedInstanceState)
//
//            displayToast()
//        }

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val view = inflater!!.inflate(R.layout.edit_fragment, container, false)

            return view
        }

        private fun displayToast() {
            if (activity != null && toast != null) {
                Toast.makeText(activity, toast, Toast.LENGTH_LONG).show()
                toast = null
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    toast = "Cancelled from fragment"
                } else {
                    toast = "Scanned from fragment: " + result.contents
                }

                // At this point we may or may not have a reference to the activity
                displayToast()
            }
        }
    }

    override fun onBackPressed() {
        if (isShowingPopUp) {
            if (isEditingExistingEntry) {
                rollbackEdition()
            } else {
                hideBothFrames()
            }
        } else {
            AlertDialog.Builder(this)
                    .setTitle("Sair?")
                    .setMessage("Você realmente deseja sair?")
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                        super.onBackPressed()
                    }).create().show()
        }
    }


    override fun onResume() {
        super.onResume()

        barcodeView!!.resume()
    }

    override fun onPause() {
        super.onPause()

        barcodeView!!.pause()
    }


    private fun hideBothFrames() {
        val frameEdit = findViewById(R.id.fragment_info_editor)
        val frameView = findViewById(R.id.fragment_info_viewer)
        frameEdit.visibility = View.GONE
        frameView.visibility = View.GONE
        isShowingPopUp = false
    }

    private fun showViewerFrame() {
        val frameEdit = findViewById(R.id.fragment_info_editor)
        val frameView = findViewById(R.id.fragment_info_viewer)
        frameEdit.visibility = View.GONE
        frameView.visibility = View.VISIBLE
        isShowingPopUp = true
    }

    private fun showEditorFrame() {
        val frameEdit = findViewById(R.id.fragment_info_editor)
        val frameView = findViewById(R.id.fragment_info_viewer)
        frameEdit.visibility = View.VISIBLE
        frameView.visibility = View.GONE
        isShowingPopUp = true
    }

    private fun rollbackEdition() {
        isEditingExistingEntry = false
        showViewerFrame()
    }

    fun edit(view: View) {
        val code = (findViewById(R.id.info_viewer_code) as TextView).text.toString()
        val text = (findViewById(R.id.info_viewer_text) as TextView).text.toString()

        (findViewById(R.id.info_editor_code) as TextView).text = code
        (findViewById(R.id.info_editor_text) as TextView).text = text

        isEditingExistingEntry = true

        showEditorFrame()
    }

    fun save(view: View) {
        val code = (findViewById(R.id.info_editor_code) as TextView).text.toString()
        val text = (findViewById(R.id.info_editor_text) as TextView).text.toString()

        (findViewById(R.id.info_viewer_code) as TextView).text = code
        (findViewById(R.id.info_viewer_text) as TextView).text = text

        val data = DataContract.Data(code, text)
        setDataToCode(data)

        showViewerFrame()
    }

    fun pause(view: View) {
        barcodeView!!.pause()
    }

    fun resume(view: View) {
        barcodeView!!.resume()
    }

    fun triggerScan(view: View) {
        barcodeView!!.decodeSingle(callback)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return barcodeView!!.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}
