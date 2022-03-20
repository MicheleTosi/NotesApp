package it.mem.notesapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.mem.notesapp.database.NotesDatabase
import it.mem.notesapp.databinding.FragmentCreateNoteBinding
import it.mem.notesapp.entities.Notes
import it.mem.notesapp.util.NoteBottomSheetFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*

class CreateNoteFragment: BaseFragment(),EasyPermissions.PermissionCallbacks,EasyPermissions.RationaleCallbacks{

    private lateinit var binding: FragmentCreateNoteBinding
    var selectedColor = "#4e33ff"
    var currentDate:String? = null
    private var READ_STORAGE_PERM = 123
    private var REQUEST_CODE_IMAGE = 456
    private var selectedImagePath = ""
    private var webLink = ""
    private var noteId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteId = requireArguments().getInt("noteId",-1)

        /*if (savedInstanceState != null) {
            noteId=savedInstanceState.getInt("noteId", -1)
        }*/
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding= FragmentCreateNoteBinding.inflate(layoutInflater)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                CreateNoteFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (noteId != -1) {

            CoroutineScope(Dispatchers.Main).launch {
                context?.let {
                    val notes = NotesDatabase.getDatabase(it).noteDao().getSpecificNote(noteId)
                    binding.colorView.setBackgroundColor(Color.parseColor(notes.color))
                    binding.etNoteTitle.setText(notes.title)
                    binding.etNoteSubTitle.setText(notes.subTitle)
                    binding.etNoteDesc.setText(notes.noteText)
                    if (notes.imgPath != "") {
                        selectedImagePath = notes.imgPath!!
                        binding.imgNote.setImageBitmap(BitmapFactory.decodeFile(notes.imgPath))
                        binding.layoutImage.visibility = View.VISIBLE
                        binding.imgNote.visibility = View.VISIBLE
                        binding.imgDelete.visibility = View.VISIBLE
                    } else {
                        binding.layoutImage.visibility = View.GONE
                        binding.imgNote.visibility = View.GONE
                        binding.imgDelete.visibility = View.GONE
                    }

                    if (notes.webLink != "") {
                        webLink = notes.webLink!!
                        binding.tvWebLink.text = notes.webLink
                        binding.layoutWebUrl.visibility = View.VISIBLE
                        binding.etWebLink.setText(notes.webLink)
                        binding.imgUrlDelete.visibility = View.VISIBLE
                    } else {
                        binding.imgUrlDelete.visibility = View.GONE
                        binding.layoutWebUrl.visibility = View.GONE
                        binding.layoutWebUrl.visibility = View.GONE
                    }
                    if(notes.color!=""){
                        binding.colorView.setBackgroundColor(Color.parseColor(notes.color))
                        selectedColor= notes.color.toString()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                broadcastReceiver, IntentFilter("bottom_sheet_action")
        )

        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")

        currentDate = sdf.format(Date())

        binding.tvDateTime.text = currentDate

        binding.imgDone.setOnClickListener {
            if (noteId != -1) {
                updateNote()
            } else {
                saveNote()
            }
        }

        binding.imgBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.imgMore.setOnClickListener {


            val noteBottomSheetFragment = NoteBottomSheetFragment.newInstance(noteId)
            noteBottomSheetFragment.show(requireActivity().supportFragmentManager, "Note Bottom Sheet Fragment")
        }

        binding.imgDelete.setOnClickListener {
            selectedImagePath = ""
            binding.layoutImage.visibility = View.GONE

        }

        binding.btnOk.setOnClickListener {
            binding.btnOk.setOnClickListener {
                if (binding.etWebLink.text.toString().trim().isNotEmpty()) {
                    checkWebUrl()
                } else {
                    Toast.makeText(requireContext(), "Url is Required", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnCancel.setOnClickListener {
                if (noteId != -1) {
                    binding.tvWebLink.visibility = View.VISIBLE
                    binding.layoutWebUrl.visibility = View.GONE
                } else {
                    binding.layoutWebUrl.visibility = View.GONE
                }

            }

            binding.imgUrlDelete.setOnClickListener {
                webLink = ""
                binding.tvWebLink.visibility = View.GONE
                binding.imgUrlDelete.visibility = View.GONE
                binding.layoutWebUrl.visibility = View.GONE
            }

            binding.tvWebLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(binding.etWebLink.text.toString()))
                startActivity(intent)
            }

        }
    }


    private fun updateNote(){
        CoroutineScope(Dispatchers.Main).launch {

            context?.let {
                val notes = NotesDatabase.getDatabase(it).noteDao().getSpecificNote(noteId)

                notes.title = binding.etNoteTitle.text.toString()
                notes.subTitle = binding.etNoteSubTitle.text.toString()
                notes.noteText = binding.etNoteDesc.text.toString()
                notes.dateTime = currentDate
                notes.color = selectedColor
                notes.imgPath = selectedImagePath
                notes.webLink = webLink

                NotesDatabase.getDatabase(it).noteDao().updateNote(notes)
                binding.etNoteTitle.setText("")
                binding.etNoteSubTitle.setText("")
                binding.etNoteDesc.setText("")
                binding.layoutImage.visibility = View.GONE
                binding.imgNote.visibility = View.GONE
                binding.tvWebLink.visibility = View.GONE
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }
    private fun saveNote(){

        if (binding.etNoteTitle.text.isNullOrEmpty()){
            Toast.makeText(context,"Note Title is Required", Toast.LENGTH_SHORT).show()
        }
        else if (binding.etNoteSubTitle.text.isNullOrEmpty()){

            Toast.makeText(context,"Note Sub Title is Required", Toast.LENGTH_SHORT).show()
        }

        else if (binding.etNoteDesc.text.isNullOrEmpty()){

            Toast.makeText(context,"Note Description is Required", Toast.LENGTH_SHORT).show()
        }

        else{

            CoroutineScope(Dispatchers.Main).launch {
                val notes = Notes()
                notes.title = binding.etNoteTitle.text.toString()
                notes.subTitle = binding.etNoteSubTitle.text.toString()
                notes.noteText = binding.etNoteDesc.text.toString()
                notes.dateTime = currentDate
                notes.color = selectedColor
                notes.imgPath = selectedImagePath
                notes.webLink = webLink
                context?.let {
                    NotesDatabase.getDatabase(it).noteDao().insertNotes(notes)
                    binding.etNoteTitle.setText("")
                    binding.etNoteSubTitle.setText("")
                    binding.etNoteDesc.setText("")
                    binding.layoutImage.visibility = View.GONE
                    binding.imgNote.visibility = View.GONE
                    binding.tvWebLink.visibility = View.GONE
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

    }

    private fun deleteNote(){

        CoroutineScope(Dispatchers.Main).launch {
            context?.let {
                NotesDatabase.getDatabase(it).noteDao().deleteSpecificNote(noteId)
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    private fun checkWebUrl(){
        if (Patterns.WEB_URL.matcher(binding.etWebLink.text.toString()).matches()){
            binding.layoutWebUrl.visibility = View.GONE
            binding.etWebLink.isEnabled = false
            webLink = binding.etWebLink.text.toString()
            binding.tvWebLink.visibility = View.VISIBLE
            binding.tvWebLink.text = binding.etWebLink.text.toString()
        }else{
            Toast.makeText(requireContext(),"Url is not valid", Toast.LENGTH_SHORT).show()
        }
    }


    private val broadcastReceiver : BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {

            val actionColor = p1!!.getStringExtra("action")

            when(actionColor!!){

                "Blue" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }

                "Yellow" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Purple" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Green" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Orange" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Black" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }

                "Image" ->{
                    readStorageTask()
                    binding.layoutWebUrl.visibility = View.GONE
                }

                "WebUrl" ->{
                    binding.layoutWebUrl.visibility = View.VISIBLE
                }
                "DeleteNote" -> {
                    //delete note
                    deleteNote()
                }


                else -> {
                    binding.layoutImage.visibility = View.GONE
                    binding.imgNote.visibility = View.GONE
                    binding.layoutWebUrl.visibility = View.GONE
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }
            }
        }

    }

    override fun onDestroy() {

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        super.onDestroy()


    }

    private fun hasReadStoragePerm():Boolean{
        return EasyPermissions.hasPermissions(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
    }


    private fun readStorageTask(){
        if (hasReadStoragePerm()){


            pickImageFromGallery()
        }else{
            EasyPermissions.requestPermissions(
                    requireActivity(),
                    getString(R.string.storage_permission_text),
                    READ_STORAGE_PERM,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    var launchSomeActivity=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        if(result.resultCode==Activity.RESULT_OK){
            if (result.data != null){
                val selectedImageUrl = result.data!!.data
                if (selectedImageUrl != null){
                    try {
                        val inputStream = requireActivity().contentResolver.openInputStream(selectedImageUrl)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgNote.setImageBitmap(bitmap)
                        binding.imgNote.visibility = View.VISIBLE
                        binding.layoutImage.visibility = View.VISIBLE

                        selectedImagePath = getPathFromUri(selectedImageUrl)!!
                    }catch (e:Exception){
                        Toast.makeText(requireContext(),e.message, Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(requireActivity().packageManager) != null){
            launchSomeActivity.launch(intent)
        }
    }

    private fun getPathFromUri(contentUri: Uri): String? {
        val filePath:String?
        val cursor = requireActivity().contentResolver.query(contentUri,null,null,null,null)
        if (cursor == null){
            filePath = contentUri.path
        }else{
            cursor.moveToFirst()
            val index = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }
        return filePath
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == Activity.RESULT_OK){
            if (data != null){
                val selectedImageUrl = data.data
                if (selectedImageUrl != null){
                    try {
                        val inputStream = requireActivity().contentResolver.openInputStream(selectedImageUrl)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgNote.setImageBitmap(bitmap)
                        binding.imgNote.visibility = View.VISIBLE
                        binding.layoutImage.visibility = View.VISIBLE

                        selectedImagePath = getPathFromUri(selectedImageUrl)!!
                    }catch (e:Exception){
                        Toast.makeText(requireContext(),e.message, Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }



    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,requireActivity())
    }


    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(requireActivity(),perms)){
            AppSettingsDialog.Builder(requireActivity()).build().show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onRationaleDenied(requestCode: Int) {

    }

    override fun onRationaleAccepted(requestCode: Int) {

    }


}
