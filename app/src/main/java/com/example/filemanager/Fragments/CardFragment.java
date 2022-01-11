package com.example.filemanager.Fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filemanager.FileAdapter;
import com.example.filemanager.FileOpener;
import com.example.filemanager.OnFileSelectedListener;
import com.example.filemanager.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardFragment extends Fragment implements OnFileSelectedListener {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<File> fileList;
    private ImageView img_back;
    private TextView tv_pathHolder;
//    file directory
    File storage;

    View view;
    String data;
    String[] items= {"Details", "Rename","Delete","Share"};

    String secondaryStorage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_card, container, false);

        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.img_back);


    File[] externalCacheDirs = getContext().getExternalCacheDirs();
    for(File file: externalCacheDirs){
        if (Environment.isExternalStorageRemovable(file)){
           secondaryStorage = file.getPath().split("/Android")[0];
           break;
        }
    }


        storage = new File(secondaryStorage);

        //open folder

        //if user open internal storage at first time, there is no any data in the path variable
        try{
            data= getArguments().getString("path");
            File file = new File(data);
            storage= file;
        }catch (Exception e){
            e.printStackTrace();

        }

        tv_pathHolder.setText(storage.getAbsolutePath());

        //        ask runtime permission
        runtimePermission();

        return view;
    }

    private void runtimePermission() {
        Dexter.withContext(getContext()).withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
            //method to show the files
            displayFiles();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
//need permission token
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    //find the file from the expected directory
    public ArrayList<File> findFiles(File file){
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        //search each file
        for(File singleFile :files){
            //check if file is directory or not
            //work with non-hidden directories
            if(singleFile.isDirectory() && !singleFile.isHidden()){
                arrayList.add(singleFile);
            }
        }
        //check if the file contain particular format or not
        for(File singleFile :files){
            if(singleFile.getName().toLowerCase().endsWith(".jpeg") || singleFile.getName().toLowerCase().endsWith(".jpg")
            || singleFile.getName().toLowerCase().endsWith(".png") || singleFile.getName().toLowerCase().endsWith(".mp3")
            || singleFile.getName().toLowerCase().endsWith(".wav") || singleFile.getName().toLowerCase().endsWith(".mp4")
            || singleFile.getName().toLowerCase().endsWith(".pdf") || singleFile.getName().toLowerCase().endsWith(".doc")
            || singleFile.getName().toLowerCase().endsWith(".apk")){

                arrayList.add(singleFile);
            }
        }
        return  arrayList;
    }
//create method outside of the onCreate View
    private void displayFiles() {
        recyclerView = view.findViewById((R.id.recycler_internal));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
        //initialize fileList
        fileList = new ArrayList<>();
        fileList.addAll(findFiles(storage));

        fileAdapter = new FileAdapter(getContext(), fileList, this);
        recyclerView.setAdapter(fileAdapter);
    }


    @Override
    public void onFileClicked(File file) {
        if(file.isDirectory()){
            Bundle bundle = new Bundle();
                bundle.putString("path", file.getAbsolutePath());
                CardFragment internalFragment = new CardFragment();
                internalFragment.setArguments(bundle);
                getFragmentManager().beginTransaction().replace(R.id.fragment_container, internalFragment).addToBackStack(null).commit();

        }
        else{
            try{
                //call to file opener class
                FileOpener.openFile(getContext(), file);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onFileLongClicked(File file, int position) {
        //to rename, delete,etc
        //create new dialog box
        final Dialog optionDialog = new Dialog(getContext());
        optionDialog.setContentView(R.layout.option_dialog);
        optionDialog.setTitle("Select Options.");
        ListView options = (ListView) optionDialog.findViewById(R.id.List);

        CustomAdapter customAdapter = new CustomAdapter();
        //set custom adapter to listview
        options.setAdapter(customAdapter);
        optionDialog.show();

        //create onClickListener
        options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItem = adapterView.getItemAtPosition(i).toString();

                //use switch case to check options
                switch (selectedItem){
                    case "Details":
                        AlertDialog.Builder detailDialog = new AlertDialog.Builder(getContext());
                        detailDialog.setTitle("Details");
                        final TextView details = new TextView(getContext());
                        detailDialog.setView(details);
                        Date lastModified = new Date(file.lastModified());
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/mm/yyyy HH:mm:ss");
                        String formattedDate = formatter.format(lastModified);

                        details.setText("File Name"+ file.getName() + "\n" +
                                "Size" + Formatter.formatShortFileSize(getContext(), file.length()) + "\n"+
                                "Path" + file.getAbsolutePath() + "\n" +
                                "Last Modified" + formattedDate);
                        //set up dialog
                        detailDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                optionDialog.cancel();
                            }
                        });
                        AlertDialog alertDialog_details = detailDialog.create();
                        alertDialog_details.show();
                        break;

                    case "Rename":
                        AlertDialog.Builder renameDialog = new AlertDialog.Builder(getContext());
                        renameDialog.setTitle("Rename File:");
                        final EditText name = new EditText(getContext());
                        renameDialog.setView(name);

                        renameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //get new name from the user
                                String new_name = name.getEditableText().toString();
                                //get file extention
                                String extention =  file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                            //get current file from recycler view
                                File current = new File(file.getAbsolutePath());
                                //create destination file

                                File destination = new File(file.getAbsolutePath().replace(file.getName(),new_name) + extention);

                                //every time we can't rename files. sometimes existing directories has similar name

                                    if(current.renameTo(destination)){
                                        //need to update recycler view
                                        fileList.set(position, destination);
                                        fileAdapter.notifyItemChanged(position);
                                        Toast.makeText(getContext(),"Renamed", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        //unable to rename the file
                                        Toast.makeText(getContext(), "Couldn't Rename!", Toast.LENGTH_SHORT).show();
                                    }


                            }
                        });

                        renameDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                optionDialog.cancel();
                            }
                        });
                        AlertDialog alertDialog_rename = renameDialog.create();
                        alertDialog_rename.show();

                        break;


                        //delete option
                    case "Delete":
                        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getContext());
                        deleteDialog.setTitle("Delete" + file.getName() + "?");
                        deleteDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                               file.delete();
                               //call to file adapter
                                fileList.remove(position);
                                fileAdapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Delete",Toast.LENGTH_SHORT).show();
                            }
                        });

                        deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                optionDialog.cancel();
                            }
                        });
                        AlertDialog alertDialog_delete = deleteDialog.create();
                        alertDialog_delete.show();
                        break;

                    case "Share":
                        //suggest appropriate app to share the file
                        String fileName = file.getName();
                        Intent share = new Intent();
                        share.setAction(Intent.ACTION_SEND);
                        share.setType("image/jpeg");
                        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        startActivity(Intent.createChooser(share, "Share" + fileName));
                        break;
                }
            }
        });

    }
    //custom adapter
    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
//            return 0; length of the items
            return items.length;
        }

        @Override
        public Object getItem(int i) {
//            return null; position
            return items[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            //populate view with the particular title and icon
            View myView = getLayoutInflater().inflate(R.layout.option_layout, null);
            //create textview and image view objects
            TextView txtOptions = myView.findViewById(R.id.txtOption);
            ImageView imgOptions = myView.findViewById(R.id.imgOption);
            txtOptions.setText(items[i]);

            if (items[i].equals("Details")) {
                imgOptions.setImageResource(R.drawable.ic_details);
            } else if (items[i].equals("Rename")) {
                imgOptions.setImageResource(R.drawable.ic_rename);
            } else if (items[i].equals("Delete")) {
                imgOptions.setImageResource(R.drawable.ic_delete);
            } else if (items[i].equals("Share")) {
                imgOptions.setImageResource(R.drawable.ic_share);
            }

//            return null;
                return myView;
        }


    }
}
