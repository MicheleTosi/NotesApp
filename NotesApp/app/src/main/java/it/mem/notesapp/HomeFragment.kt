package it.mem.notesapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import it.mem.notesapp.adapter.NotesAdapter
import it.mem.notesapp.database.NotesDatabase
import it.mem.notesapp.databinding.FragmentHomeBinding
import it.mem.notesapp.entities.Notes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class HomeFragment: BaseFragment(){
    private lateinit var binding:FragmentHomeBinding
    var arrNotes=ArrayList<Notes>()
    var notesAdapter: NotesAdapter = NotesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding= FragmentHomeBinding.inflate(layoutInflater)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager=StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        CoroutineScope(Dispatchers.Main).launch {
            val notes = context?.let { NotesDatabase.getDatabase(it).noteDao().getAllNotes() }
            if (notes != null) {
                notesAdapter.setData(notes)
            }
            arrNotes = notes as ArrayList<Notes>
            binding.recyclerView.adapter = notesAdapter
        }

        notesAdapter.setOnClickListener(onClicked)

        binding.fabBtnCreateNote.setOnClickListener {
            replaceFragment(CreateNoteFragment.newInstance(),false)
        }

        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            binding.searchView.isSelected = hasFocus
            binding.searchView.isIconified = !hasFocus
        }

        binding.searchView.setOnQueryTextListener( object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {

                val tempArr = ArrayList<Notes>()

                for (arr in arrNotes){
                    if (arr.title!!.toLowerCase(Locale.getDefault()).contains(p0.toString().toLowerCase(Locale.ROOT))) {
                        tempArr.add(arr)
                    }
                }

                notesAdapter.setData(tempArr)
                notesAdapter.notifyDataSetChanged()
                return true
            }

        })



        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                HomeFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }

    private val onClicked = object : NotesAdapter.OnItemClickListener{
        override fun onClicked(noteId: Int) {


            val fragment :Fragment
            val bundle = Bundle()
            bundle.putInt("noteId",noteId)
            fragment = CreateNoteFragment.newInstance()
            fragment.arguments = bundle

            replaceFragment(fragment,false)
        }

    }


    fun replaceFragment(fragment:Fragment, istransition:Boolean){
        val fragmentTransition = requireActivity().supportFragmentManager.beginTransaction()

        if (istransition){
            fragmentTransition.setCustomAnimations(android.R.anim.slide_out_right,android.R.anim.slide_in_left)
        }
        fragmentTransition.replace(R.id.frame_layout,fragment).addToBackStack(fragment.javaClass.simpleName).commit()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            121 -> {
                val noteId=notesAdapter.removeItem(item.groupId)

                CoroutineScope(Dispatchers.IO).launch {
                    context?.let {
                        if (noteId != null) {
                            NotesDatabase.getDatabase(it).noteDao().deleteSpecificNote(noteId)
                        }
                    }
                }

                Toast.makeText(requireContext(), getString(R.string.itemDeleted), Toast.LENGTH_LONG).show()
                true
            }

            122 -> {
                true
            }
            else-> super.onContextItemSelected(item)
        }
    }
}