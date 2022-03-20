package it.mem.notesapp.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import it.mem.notesapp.R
import it.mem.notesapp.databinding.ItemRvNotesBinding
import it.mem.notesapp.entities.Notes

class NotesAdapter() :
    RecyclerView.Adapter<NotesAdapter.NotesViewHolder>() {

    private lateinit var binding: ItemRvNotesBinding
    private var listener:OnItemClickListener? = null
    var arrList = ArrayList<Notes>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val layoutInflater=LayoutInflater.from(parent.context)
        binding= ItemRvNotesBinding.inflate(layoutInflater, parent, false)
        return NotesViewHolder(binding, parent.context)
    }

    override fun getItemCount(): Int {
        return arrList.size
    }

    fun setData(arrNotesList: List<Notes>){
        arrList = arrNotesList as ArrayList<Notes>
    }

    fun setOnClickListener(listener1: OnItemClickListener){
        listener = listener1
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {

        holder.tvTitle.text = arrList[position].title
        holder.tvDesc.text = arrList[position].noteText
        holder.tvDateTime.text = arrList[position].dateTime

        if (arrList[position].color != null){
            holder.cardView.setCardBackgroundColor(Color.parseColor(arrList[position].color))
        }else{
            holder.cardView.setCardBackgroundColor(Color.parseColor(R.color.ColorWhite.toString()))
        }

        if (arrList[position].imgPath != null){
            holder.imgNote.setImageBitmap(BitmapFactory.decodeFile(arrList[position].imgPath))
            holder.imgNote.visibility = View.VISIBLE
        }else{
            holder.imgNote.visibility = View.GONE
        }

        if (arrList[position].webLink != ""){
            holder.tvWebLink.text = arrList[position].webLink
            holder.tvWebLink.visibility = View.VISIBLE
        }else{
            holder.tvWebLink.visibility = View.GONE
        }

        holder.cardView.setOnClickListener {
            listener!!.onClicked(arrList[position].id!!)
        }


    }

    class NotesViewHolder(binding: ItemRvNotesBinding, val context: Context) : RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener{


        val tvTitle= binding.tvTitle
        val tvDesc=binding.tvDesc
        val cardView=binding.cardView
        val tvWebLink=binding.tvWebLink
        val tvDateTime= binding.tvDateTime
        val imgNote=binding.imgNote

        init {
            cardView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.setHeaderTitle(context.getString(R.string.selectOption))
            menu?.add(this.adapterPosition, 121, 0, context.getString(R.string.delete_note))
            menu?.add(this.adapterPosition, 122, 1,context.getString(R.string.cancel))
        }

    }

    fun removeItem(position:Int) : Int?{
        val noteId = arrList[position].id
        arrList.removeAt(position)
        notifyDataSetChanged()
        return noteId
    }


    interface OnItemClickListener{
        fun onClicked(noteId:Int)
    }

}
