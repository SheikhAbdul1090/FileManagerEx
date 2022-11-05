package com.example.filemanagerex;

import static com.example.filemanagerex.DbHelper.COLUMN_PATH;
import static com.example.filemanagerex.DbHelper.COLUMN_POSITION;
import static com.example.filemanagerex.DbHelper.TABLE_NAME;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ItemFilterListener;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback;
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback;
import com.mikepenz.fastadapter_extensions.utilities.DragDropUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileList extends AppCompatActivity implements onFileSelectedListener, ItemTouchCallback, ItemFilterListener<AbstractItem> {

    protected static String storageDirectoryPath;
    protected static boolean isSortByDragAndDropEnable;
    protected static boolean isShowHiddenFileEnable;
    private static boolean isGridViewEnable;
    private static boolean isSortDescendingEnable;
    private static boolean isSortAscendingEnable;
    private static boolean isSortByNameEnable;
    private static boolean isSortByDateEnable;
    private static boolean isSortBySizeEnable;
    private final List<PostPathData> pathDataList = new ArrayList<>();
    private Context context;
    private TextView tvNoFile, tvCancel, tvPaste;
    private RecyclerView recPath, recFile;
    private LinearLayout layoutPasteOption;
    private ProgressDialog progressDialog;
    private FastAdapter<AbstractItem> mfastAdapter;
    private ItemAdapter<AbstractItem> mitemAdapter;
    private ActionModeHelper<PostItemData> mActionModeHelper;
    private File currentFilePath;
    private List<File> fileList;
    private int fileHeaderPosition = 0;

    /**
     * Database helper object
     */
    private DbHelper mDbHelper;

    //drag & drop
    private SimpleDragCallback touchCallback;

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }

            String[] files = source.list();

            assert files != null;
            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(dest, file);

                copyFileUsingStream(srcFile, destFile);
            }
        } else {

            InputStream in = new FileInputStream(source);
            try {
                OutputStream out = new FileOutputStream(dest);
                try {
                    // Transfer bytes from in to out
                    byte[] buffer = new byte[512];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filelist);

        context = this;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("File Manager");

        AppCompatImageView imgPath = findViewById(R.id.imgPath);

        tvNoFile = findViewById(R.id.tvNoFile);
        recFile = findViewById(R.id.recFile);
        recPath = findViewById(R.id.recPath);
        layoutPasteOption = findViewById(R.id.layoutPasteOption);
        tvPaste = findViewById(R.id.tvPaste);
        tvCancel = findViewById(R.id.tvCancel);

        fileList = new ArrayList<>();

        mDbHelper = new DbHelper(context);
        mDbHelper.getWritableDatabase();


        SharedPreferences sharedPreferences = getSharedPreferences("Sort_And_View_Status", MODE_PRIVATE);

        isGridViewEnable = sharedPreferences.getBoolean("state_of_gridView_radio_button", false);
        isSortAscendingEnable = sharedPreferences.getBoolean("state_of_sortAscending_radio_button", false);
        isSortDescendingEnable = sharedPreferences.getBoolean("state_of_sortDescending_radio_button", false);
        isSortByNameEnable = sharedPreferences.getBoolean("state_of_sortByName_radio_button", false);
        isSortByDateEnable = sharedPreferences.getBoolean("state_of_sortByDate_radio_button", false);
        isSortBySizeEnable = sharedPreferences.getBoolean("state_of_sortBySize_radio_button", false);
        isSortByDragAndDropEnable = sharedPreferences.getBoolean("state_of_sortByDragAndDrop_radio_button", false);
        isShowHiddenFileEnable = sharedPreferences.getBoolean("state_of_showHiddenFile_checkBox", false);


        imgPath.setOnClickListener(v -> FileList.super.onBackPressed());

        storageDirectoryPath = getIntent().getStringExtra("path");
        if (storageDirectoryPath == null)
            return;

        File storage = new File(storageDirectoryPath);
        if (storage.getParent() == null)
            return;

        currentFilePath = storage;

        mitemAdapter = new ItemAdapter<>();
        mfastAdapter = FastAdapter.with(mitemAdapter);
        recFile.setAdapter(mfastAdapter);

        mfastAdapter.withSelectable(true);
        mfastAdapter.withMultiSelect(true);
        mfastAdapter.withSelectOnLongClick(true);

        mfastAdapter.withOnPreClickListener((v, adapter, item, position) -> {
            if (item instanceof PostItemData) {
                //we handle the default onClick behavior for the actionMode. This will return null if it didn't do anything and you can handle a normal onClick
                Boolean res = mActionModeHelper.onClick(item);
                return res != null ? res : false;
            }
            return true;
        });

        mfastAdapter.withOnClickListener((v, adapter, item, position) -> {
            if (item instanceof PostItemData) {
                if (mfastAdapter.getSelections().size() == 0) {
                    onFileClicked(((PostItemData) item).file);
                }
            }
            return false;
        });

        mitemAdapter.getItemFilter().withFilterPredicate(new IItemAdapter.Predicate<AbstractItem>() {
            @Override
            public boolean filter(AbstractItem item, @Nullable CharSequence constraint) {
                //return true if we should filter it out
                //return false to keep it
                if (item instanceof PostItemData && constraint != null)
                    return ((PostItemData) item).file.getName().toLowerCase(Locale.ROOT).contains(constraint.toString().toLowerCase(Locale.ROOT));
                else
                    return false;

            }
        });

        mitemAdapter.getItemFilter().withItemFilterListener(this);

        mfastAdapter.withOnPreLongClickListener((v, adapter, item, position) -> {
            if (!isSortByDragAndDropEnable) {
                if (item instanceof PostItemData) {
                    ActionMode actionMode = mActionModeHelper.onLongClick(FileList.this, position);
                    if (actionMode != null) {
                        if (getSupportActionBar() != null)
                            getSupportActionBar().hide();
                        //we want color our CAB
                        findViewById(R.id.action_mode_bar).setBackgroundColor(getResources().getColor(R.color.orange));
                    }

                    //if we have no actionMode we do not consume the event
                    return actionMode != null;
                }
            }
            return false;
        });

        mActionModeHelper = new ActionModeHelper(mfastAdapter, R.menu.menu, new ActionBarCallBack());

        getPath(storage);
        new DisplayFilesAndFolders().execute(storage);

        //restore selections (this has to be done after the items were added
        mfastAdapter.withSavedInstanceState(savedInstanceState);

        //set the back arrow in the toolbar
        if (getSupportActionBar() != null)
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        if (!isGridViewEnable)
            menu.findItem(R.id.menuListView).setChecked(true);
        else
            menu.findItem(R.id.menuGridView).setChecked(true);
        if (isSortAscendingEnable)
            menu.findItem(R.id.menuSortAscending).setChecked(true);
        else if (isSortDescendingEnable)
            menu.findItem(R.id.menuSortDescending).setChecked(true);
        if (isSortByNameEnable)
            menu.findItem(R.id.menuSortByName).setChecked(true);
        else if (isSortByDateEnable)
            menu.findItem(R.id.menuSortByDate).setChecked(true);
        else if (isSortBySizeEnable)
            menu.findItem(R.id.menuSortBySize).setChecked(true);
        else if (isSortByDragAndDropEnable)
            menu.findItem(R.id.menuSortByDragAndDrop).setChecked(true);
        if (isShowHiddenFileEnable)
            menu.findItem(R.id.menuShowHiddenFile).setChecked(true);

        return true;
    }

    public List<AbstractItem> findFiles(List<File> filesAndFolder, boolean isGridViewEnable) {

        List<AbstractItem> list = new ArrayList<>();

        if (isShowHiddenFileEnable) {
            for (File singleFile : filesAndFolder) {
                list.add(new PostItemData(singleFile, this, isGridViewEnable));
            }

        } else {
            for (File singleFile : filesAndFolder) {
                if (!singleFile.isHidden()) {
                    list.add(new PostItemData(singleFile, this, isGridViewEnable));
                }
            }

        }

        sortList(list);

        return list;

    }

    private void sortList(List<AbstractItem> list) {

        Collections.sort(list, (o1, o2) -> {
            if (o1 instanceof PostItemData && o2 instanceof PostItemData) {

                if (((PostItemData) o1).file.isDirectory() && !((PostItemData) o2).file.isDirectory())
                    // Directory before File
                    return -1;
                else if (!((PostItemData) o1).file.isDirectory() && ((PostItemData) o2).file.isDirectory())
                    // File after directory
                    return 1;
                else {

                    if (isSortByNameEnable) {

                        if (isSortAscendingEnable || !isSortDescendingEnable) {
                            return ((PostItemData) o1).file.getName().compareTo(((PostItemData) o2).file.getName());
                        } else {
                            return ((PostItemData) o2).file.getName().compareTo(((PostItemData) o1).file.getName());
                        }

                    } else if (isSortByDateEnable) {

                        Date d1LastModified = new Date(((PostItemData) o1).file.lastModified());
                        Date d2LastModified = new Date(((PostItemData) o2).file.lastModified());
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        String d1formattedDate = formatter.format(d1LastModified);
                        String d2formattedDate = formatter.format(d2LastModified);
                        if (isSortAscendingEnable || !isSortDescendingEnable) {
                            return d1formattedDate.compareTo(d2formattedDate);
                        } else {
                            return d2formattedDate.compareTo(d1formattedDate);
                        }

                    } else if (isSortBySizeEnable) {

                        Long firstFileOrFolderSize = getSizeOfFileORFolder(((PostItemData) o1).file);
                        Long secondFileOrFolderSize = getSizeOfFileORFolder(((PostItemData) o2).file);
                        if (isSortAscendingEnable || !isSortDescendingEnable) {
                            return firstFileOrFolderSize.compareTo(secondFileOrFolderSize);
                        } else {
                            return secondFileOrFolderSize.compareTo(firstFileOrFolderSize);
                        }

                    } else if (isSortByDragAndDropEnable) {

                        Long position1 = getPositionOfFile(((PostItemData) o1).file);
                        long position2 = getPositionOfFile(((PostItemData) o2).file);
                        if ((position1 != 0 && position2 != 0) || (position1 == 0 && position2 != 0))
                            return position1.compareTo(position2);
                        else
                            return 0;

                    } else {

                        return 0;

                    }
                }

            } else {

                return 0;
            }
        });

        addHeaderBeforeFolderAndFile(list);

    }

    private void addHeaderBeforeFolderAndFile(List<AbstractItem> list) {

        boolean isDirectoryAvailable = false;
        boolean isFileAvailable = false;
        int fileIndex = 0;


        for (AbstractItem singleFile : list) {
            if (singleFile instanceof PostItemData) {
                if (((PostItemData) singleFile).file.isDirectory()) {
                    isDirectoryAvailable = true;
                    break;
                }
            }
        }

        for (AbstractItem singleFile : list) {
            if (singleFile instanceof PostItemData) {
                fileIndex++;
                if (!((PostItemData) singleFile).file.isDirectory()) {
                    isFileAvailable = true;
                    break;
                }
            }
        }

        if (isDirectoryAvailable)
            list.add(0, new HeaderItem("Folder :  " + fileOrFolderCount(list, "Folder")));

        if (isFileAvailable) {
            if (!isDirectoryAvailable)
                list.add(0, new HeaderItem("File :  " + fileOrFolderCount(list, "File")));
            else {
                fileHeaderPosition = fileIndex;
                list.add(fileIndex, new HeaderItem("File :  " + fileOrFolderCount(list, "File")));
            }
        }
    }

    private long getPositionOfFile(File file) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        String SQL_SELECT_STORAGE_DATA = "select " + COLUMN_POSITION + " from " + TABLE_NAME + " where " + COLUMN_PATH + " = '" + file.getAbsolutePath() + "'";
        Cursor cr = database.rawQuery(SQL_SELECT_STORAGE_DATA, null);
        if (cr.moveToFirst()) {
            return Long.parseLong(cr.getString(0));
        } else {
            return 0L;
        }
    }

    private long getSizeOfFileORFolder(File file) {
        long size;
        if (file.isDirectory()) {
            size = 0;
            if (file.listFiles() != null) {
                for (File eachFile : file.listFiles()) {
                    size += getSizeOfFileORFolder(eachFile);
                }
            }
        } else {
            size = file.length();
        }
        return size;
    }

    private long fileOrFolderCount(List<AbstractItem> list, String type) {
        long count = 0;
        if (type.equals("Folder")) {
            for (AbstractItem singleFile : list) {
                if (singleFile instanceof PostItemData) {
                    if (((PostItemData) singleFile).file.isDirectory())
                        count++;
                }
            }
        } else if (type.equals("File")) {
            for (AbstractItem singleFile : list) {
                if (singleFile instanceof PostItemData) {
                    if (!((PostItemData) singleFile).file.isDirectory())
                        count++;
                }
            }
        }
        return count;
    }

    private void getPath(File storage) {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recPath.setLayoutManager(layoutManager);

        pathDataList.add(new PostPathData(storage));

        passToAdapter(pathDataList);

    }

    private void passToAdapter(List<PostPathData> pathData) {

        ItemAdapter<PostPathData> mitemAdapterPath = new ItemAdapter<>();
        mitemAdapterPath.setNewList(pathData);
        FastAdapter<PostPathData> mfastAdapterPath = FastAdapter.with(mitemAdapterPath);
        recPath.setAdapter(mfastAdapterPath);

        mfastAdapterPath.withSelectable(true);
        mfastAdapterPath.withOnClickListener((v, adapter, item, position) -> {

            if (position < pathDataList.size() - 1) {
                pathDataList.subList(position + 1, pathDataList.size()).clear();
                passToAdapter(pathDataList);
                currentFilePath = item.file;
                new DisplayFilesAndFolders().execute(currentFilePath);
            }

            return false;
        });
    }

    public void onFileClicked(File file) {
        if (file.isDirectory()) {
            getPath(file);
            currentFilePath = file;
            new DisplayFilesAndFolders().execute(file);
        } else {
            // open the file
            try {
                FileOpener.openFile(getApplicationContext(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onFileMenuClicked(File file, int position) {

        Dialog optionDialog = new Dialog(context);
        optionDialog.setContentView(R.layout.option_dialogue);
        optionDialog.setTitle("Select Options. ");

        LinearLayout layoutDetails = optionDialog.findViewById(R.id.layoutDetails);
        LinearLayout layoutRename = optionDialog.findViewById(R.id.layoutRename);
        LinearLayout layoutCopy = optionDialog.findViewById(R.id.layoutCopy);
        LinearLayout layoutMove = optionDialog.findViewById(R.id.layoutMove);
        LinearLayout layoutDelete = optionDialog.findViewById(R.id.layoutDelete);
        LinearLayout layoutHide = optionDialog.findViewById(R.id.layoutHide);
        LinearLayout layoutUnhide = optionDialog.findViewById(R.id.layoutUnhide);

        optionDialog.show();

        if (file.isHidden()) {
            layoutHide.setVisibility(View.GONE);
            layoutUnhide.setVisibility(View.VISIBLE);
        } else {
            layoutUnhide.setVisibility(View.GONE);
            layoutHide.setVisibility(View.VISIBLE);
        }

        layoutDetails.setOnClickListener(v -> {
            AlertDialog.Builder detailDialog = new AlertDialog.Builder(context);
            detailDialog.setTitle("Details:");
            final TextView details = new TextView(getApplicationContext());
            details.setTextColor(getResources().getColor(R.color.textColorBlackOrWhite));
            details.setPadding(60, 10, 10, 10);
            detailDialog.setView(details);
            Date lastModified = new Date(file.lastModified());
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String formattedDate = formatter.format(lastModified);
                details.setTextSize(20);
                details.setText("File Name:  " + file.getName() +
                        "\n" + "Size:  " + Formatter.formatShortFileSize(getApplicationContext(), file.length()) +
                        "\n" + "Path:  " + file.getAbsolutePath() +
                        "\n" + "Last Modified:  " + formattedDate);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            detailDialog.setCancelable(false);
            detailDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    optionDialog.cancel();
                }
            });

            AlertDialog alertDialog_details = detailDialog.create();
            alertDialog_details.show();
        });

        layoutRename.setOnClickListener(v -> {
            AlertDialog.Builder renameDialog = new AlertDialog.Builder(context);
            renameDialog.setTitle("Rename File: ");
            final EditText name = new EditText(getApplicationContext());
            name.requestFocus();
            name.setTextColor(getResources().getColor(R.color.textColorBlackOrWhite));
            renameDialog.setView(name);
            renameDialog.setCancelable(false);
            renameDialog.setPositiveButton("Ok", (dialog, which) -> {
                File destination;
                String newName = name.getEditableText().toString().trim();
                if (!file.isDirectory()) {
                    String extension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                    destination = new File(file.getAbsolutePath().replace(file.getName(), newName) + extension);
                } else {
                    destination = new File(file.getAbsolutePath().replace(file.getName(), newName));
                }
                File current = new File(file.getAbsolutePath());
                if (current.renameTo(destination)) {
                    if (file.getParent() != null) {
                        new DisplayFilesAndFolders().execute(new File(file.getParent()));
                    }

                    Toast.makeText(context, "Renamed!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Couldn't Rename!", Toast.LENGTH_SHORT).show();
                }
                optionDialog.cancel();
            });
            renameDialog.setNegativeButton("Cancel", (dialog, which) -> optionDialog.cancel());
            AlertDialog alertDialog_Rename = renameDialog.create();
            alertDialog_Rename.show();

        });

        layoutCopy.setOnClickListener(v -> {
            layoutPasteOption.setVisibility(View.VISIBLE);
            ArrayList<File> selectedFile = new ArrayList<>();
            selectedFile.add(file);
            pasteOrCancel(null, selectedFile, "Copy");
            optionDialog.dismiss();
        });

        layoutMove.setOnClickListener(v -> {
            layoutPasteOption.setVisibility(View.VISIBLE);
            ArrayList<File> selectedFile = new ArrayList<>();
            selectedFile.add(file);
            pasteOrCancel(null, selectedFile, "Move");
            optionDialog.dismiss();
        });

        layoutDelete.setOnClickListener(v -> {

            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(context);
            deleteDialog.setTitle("Delete File ?");
            deleteDialog.setCancelable(false);
            deleteDialog.setPositiveButton("Yes", (dialog, which) -> {

                File path = new File(Objects.requireNonNull(file.getParent()));
                deleteFiles(file);

                optionDialog.dismiss();
                new DisplayFilesAndFolders().execute(path);
                Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show();
            });

            deleteDialog.setNegativeButton("No", (dialog, which) -> optionDialog.dismiss());
            AlertDialog alertDialog_Delete = deleteDialog.create();
            alertDialog_Delete.show();


        });

        layoutHide.setOnClickListener(v -> {

            String newName = "." + file.getName();
            hideAndUnhideFileAndFolder(newName, file);
            optionDialog.dismiss();
        });

        layoutUnhide.setOnClickListener(v -> {
            String newName = file.getName().substring(1);
            hideAndUnhideFileAndFolder(newName, file);
            optionDialog.dismiss();
        });
    }

    private void hideAndUnhideFileAndFolder(String newName, File file) {
        File destination = new File(file.getAbsolutePath().replace(file.getName(), newName));

        File current = new File(file.getAbsolutePath());
        if (current.renameTo(destination)) {
            if (file.getParent() != null) {
                new DisplayFilesAndFolders().execute(new File(file.getParent()));
            }

        }
    }

    @Override
    public void onBackPressed() {
        pathDataList.remove(pathDataList.size() - 1);
        passToAdapter(pathDataList);

        if (currentFilePath.getAbsolutePath().equals(storageDirectoryPath)) {
            super.onBackPressed();
        } else {
            currentFilePath = currentFilePath.getParentFile();
            new DisplayFilesAndFolders().execute(currentFilePath);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //add the values which need to be saved from the adapter to the bundle
        outState = mfastAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //handle the click on the back arrow click
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menuSearch:
                final SearchView searchView = (SearchView) item.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        touchCallback.setIsDragEnabled(false);
                        mitemAdapter.filter(s);
                        return true;
                    }


                    @Override
                    public boolean onQueryTextChange(String s) {
                        mitemAdapter.filter(s);
                        return true;
                    }
                });
                return true;

            case R.id.menuCreateNewFolder:
                AlertDialog.Builder createFolderDialog = new AlertDialog.Builder(context);
                createFolderDialog.setTitle("New Folder: ");
                final EditText name = new EditText(getApplicationContext());
                name.requestFocus();
                name.setTextColor(getResources().getColor(R.color.white));
                name.setInputType(InputType.TYPE_CLASS_TEXT);
                createFolderDialog.setView(name);

                createFolderDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final File newFolder = new File(currentFilePath + "/" + name.getText());
                        if (!newFolder.exists()) {
                            newFolder.mkdir();
                            new DisplayFilesAndFolders().execute(currentFilePath);
                        } else {
                            Toast.makeText(getApplicationContext(), "Folder exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                createFolderDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                createFolderDialog.show();
                return true;

            case R.id.menuListView:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isGridViewEnable = false;
                    handleList(fileList);
                }
                return true;

            case R.id.menuGridView:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isGridViewEnable = true;
                    handleList(fileList);
                }
                return true;

            case R.id.menuSortAscending:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isSortAscendingEnable = true;
                    isSortDescendingEnable = false;
                    handleList(fileList);
                }
                return true;

            case R.id.menuSortDescending:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isSortAscendingEnable = false;
                    isSortDescendingEnable = true;
                    handleList(fileList);
                }
                return true;

            case R.id.menuSortByName:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isSortByNameEnable = true;
                    isSortByDateEnable = false;
                    isSortBySizeEnable = false;
                    isSortByDragAndDropEnable = false;

                    if (!mfastAdapter.isSelectable()) {

                        mfastAdapter.withSelectable(true);
                        mfastAdapter.withMultiSelect(true);
                        mfastAdapter.withSelectOnLongClick(true);
                        touchCallback.setIsDragEnabled(false);

                    }

                    handleList(fileList);
                }
                return true;

            case R.id.menuSortByDate:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isSortByNameEnable = false;
                    isSortByDateEnable = true;
                    isSortBySizeEnable = false;
                    isSortByDragAndDropEnable = false;

                    if (!mfastAdapter.isSelectable()) {

                        mfastAdapter.withSelectable(true);
                        mfastAdapter.withMultiSelect(true);
                        mfastAdapter.withSelectOnLongClick(true);
                        touchCallback.setIsDragEnabled(false);

                    }

                    handleList(fileList);
                }
                return true;

            case R.id.menuSortBySize:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isSortByNameEnable = false;
                    isSortByDateEnable = false;
                    isSortBySizeEnable = true;
                    isSortByDragAndDropEnable = false;

                    if (!mfastAdapter.isSelectable()) {

                        mfastAdapter.withSelectable(true);
                        mfastAdapter.withMultiSelect(true);
                        mfastAdapter.withSelectOnLongClick(true);
                        touchCallback.setIsDragEnabled(false);

                    }
                    new DisplayFilesAndFolders().execute(currentFilePath);
                    /*handleList(fileList);*/
                }
                return true;

            case R.id.menuSortByDragAndDrop:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isSortByNameEnable = false;
                    isSortByDateEnable = false;
                    isSortBySizeEnable = false;
                    isSortByDragAndDropEnable = true;

                    mfastAdapter.withSelectable(false);
                    mfastAdapter.withMultiSelect(false);
                    mfastAdapter.withSelectOnLongClick(false);
                    touchHelper();
                    touchCallback.setIsDragEnabled(true);

                    handleList(fileList);
                }
                return true;

            case R.id.menuShowHiddenFile:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    isShowHiddenFileEnable = true;
                } else {
                    item.setChecked(false);
                    isShowHiddenFileEnable = false;
                }
                new DisplayFilesAndFolders().execute(currentFilePath);
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void pasteOrCancel(ActionMode mode, ArrayList<File> selectedFilesPath, String action) {
        tvCancel.setOnClickListener(v -> {
            if (mode != null)
                mode.finish();
            layoutPasteOption.setVisibility(View.GONE);
        });
        tvPaste.setOnClickListener(v -> {
            layoutPasteOption.setVisibility(View.GONE);
            if (action.equals("Move")) {
                for (File eachFile : selectedFilesPath) {
                    moveFileAndFolder(eachFile);
                }
                new DisplayFilesAndFolders().execute(currentFilePath);
            } else {

                new CopyFiles().execute(selectedFilesPath);
            }
        });
    }

    private void moveFileAndFolder(File filePath) {
        File destPath = new File(currentFilePath + filePath.getAbsolutePath().substring(filePath.getAbsolutePath().lastIndexOf("/")));
        filePath.renameTo(destPath);
    }

    private void shareFiles() {

        ArrayList<String> fileName = new ArrayList<>();
        ArrayList<Uri> files = new ArrayList<>();

        for (AbstractItem file : mfastAdapter.getSelectedItems()) {
            if (file instanceof PostItemData) {
                fileName.add(((PostItemData) file).file.getName());
                Uri uri = FileProvider.getUriForFile(getApplicationContext(), context.getPackageName() + ".provider", ((PostItemData) file).file);
                files.add(uri);
            }
        }

        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND_MULTIPLE);
        share.setType("image/jpeg");
        share.putExtra(Intent.EXTRA_STREAM, files);
        startActivity(Intent.createChooser(share, "Share " + fileName));

    }

    private void deleteFiles(File file) {
        if (file.isDirectory()) {
            if (file.listFiles().length == 0) {
                file.delete();
            } else {
                File[] files = file.listFiles();
                assert files != null;
                for (File eachFile : files) {
                    deleteFiles(eachFile);
                }
                if (file.listFiles().length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }

    }

    void handleList(List<File> files) {
        fileList = files;
        if (isGridViewEnable) {
            setGridView();
        } else {
            setListView();
        }
    }

    void setGridView() {
        RecyclerView.LayoutManager layoutManager;
        layoutManager = new GridLayoutManager(context, 2);

        // Create a custom SpanSizeLookup where the first item spans(cover) both columns
        ((GridLayoutManager) layoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 || position == fileHeaderPosition ? 2 : 1;
            }
        });

        fileHeaderPosition = 0;
        recFile.setLayoutManager(layoutManager);

        if (fileList.size() == 0) {
            recFile.setVisibility(View.GONE);
            tvNoFile.setVisibility(View.VISIBLE);
        } else {
            mitemAdapter.setNewList(findFiles(fileList, true));
        }
    }

    void setListView() {
        RecyclerView.LayoutManager layoutManager;
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);

        fileHeaderPosition = 0;
        recFile.setLayoutManager(layoutManager);

        if (fileList.size() == 0) {
            recFile.setVisibility(View.GONE);
            tvNoFile.setVisibility(View.VISIBLE);
        } else {
            mitemAdapter.setNewList(findFiles(fileList, false));
        }
    }

    private void touchHelper() {
        /**
         * Drag & Drop
         */
        touchCallback = new SimpleDragCallback(SimpleDragCallback.ALL, FileList.this);
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchCallback); // Create ItemTouchHelper and pass with parameter the SimpleDragCallback
        touchHelper.attachToRecyclerView(recFile); // Attach ItemTouchHelper to RecyclerView
    }

    @Override
    public boolean itemTouchOnMove(int oldPosition, int newPosition) {
        if (mitemAdapter.getAdapterItem(oldPosition) instanceof PostItemData)

            if (fileHeaderPosition == 0)
                DragDropUtil.onMove(mitemAdapter, oldPosition, newPosition);
            else if ((((PostItemData) mitemAdapter.getAdapterItem(oldPosition)).file.isDirectory() && newPosition < fileHeaderPosition)
                    || (!((PostItemData) mitemAdapter.getAdapterItem(oldPosition)).file.isDirectory() && newPosition > fileHeaderPosition))
                DragDropUtil.onMove(mitemAdapter, oldPosition, newPosition);  // change position

        return true;
    }

    @Override
    public void itemTouchDropped(int oldPosition, int newPosition) {

        if (oldPosition != newPosition) {

            ArrayList<String> columnPathData = new ArrayList<>();
            SQLiteDatabase databaseRead = mDbHelper.getReadableDatabase();

            String SQL_SELECT_STORAGE_DATA = "select " + COLUMN_PATH + "  from " + TABLE_NAME;

            Cursor cr = databaseRead.rawQuery(SQL_SELECT_STORAGE_DATA, null);
            if (cr.moveToFirst()) {
                columnPathData.add(cr.getString(0));
                while (cr.moveToNext()) {
                    columnPathData.add(cr.getString(0));
                }
            }

            SQLiteDatabase databaseWrite = mDbHelper.getWritableDatabase();
            int index = 0;
            for (AbstractItem eachFile : mitemAdapter.getAdapterItems()) {

                if (eachFile instanceof PostItemData) {
                    if (columnPathData.contains(((PostItemData) eachFile).file.getAbsolutePath())) {
                        String SQL_UPDATE_STORAGE_DATA = "update " + TABLE_NAME + " set "
                                + COLUMN_POSITION + " = '" + index + "' where " + COLUMN_PATH + " = '" + ((PostItemData) eachFile).file.getAbsolutePath() + "'";

                        databaseWrite.execSQL(SQL_UPDATE_STORAGE_DATA);
                    } else {
                        String SQL_INSERT_STORAGE_DATA = "insert into " + TABLE_NAME + " ("
                                + COLUMN_PATH + ","
                                + COLUMN_POSITION + ")values('" + ((PostItemData) eachFile).file.getAbsolutePath() + "','" + index + "')";
                        databaseWrite.execSQL(SQL_INSERT_STORAGE_DATA);
                    }
                }
                index++;

            }
        }
    }

    @Override
    public void itemsFiltered(@Nullable CharSequence constraint, @Nullable List<AbstractItem> results) {
        if (mitemAdapter.getAdapterItemCount() == 0)
            Toast.makeText(context, "No File Found...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReset() {
    }

    @Override
    protected void onDestroy() {

        SharedPreferences sharedPreferences = getSharedPreferences("Sort_And_View_Status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.clear();

        if (isGridViewEnable)
            editor.putBoolean("state_of_gridView_radio_button", true);
        if (isSortAscendingEnable)
            editor.putBoolean("state_of_sortAscending_radio_button", true);
        else if (isSortDescendingEnable)
            editor.putBoolean("state_of_sortDescending_radio_button", true);
        if (isSortByNameEnable)
            editor.putBoolean("state_of_sortByName_radio_button", true);
        else if (isSortByDateEnable)
            editor.putBoolean("state_of_sortByDate_radio_button", true);
        else if (isSortBySizeEnable)
            editor.putBoolean("state_of_sortBySize_radio_button", true);
        else if (isSortByDragAndDropEnable)
            editor.putBoolean("state_of_sortByDragAndDrop_radio_button", true);
        if (isShowHiddenFileEnable)
            editor.putBoolean("state_of_showHiddenFile_checkBox", true);

        editor.apply();

        super.onDestroy();
    }

    /**
     * Our ActionBarCallBack to showcase the CAB
     */
    class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<File> selectedFilesPath = new ArrayList<>();

            switch (item.getItemId()) {
                case R.id.menu_copy:
                    layoutPasteOption.setVisibility(View.VISIBLE);
                    for (AbstractItem file : mfastAdapter.getSelectedItems()) {
                        if (file instanceof PostItemData)
                            selectedFilesPath.add(new File(((PostItemData) file).file.getAbsolutePath()));
                    }
                    pasteOrCancel(mode, selectedFilesPath, "Copy");
                    mode.finish();

                    return true;

                case R.id.menu_move:
                    layoutPasteOption.setVisibility(View.VISIBLE);
                    for (AbstractItem file : mfastAdapter.getSelectedItems()) {
                        if (file instanceof PostItemData)
                            selectedFilesPath.add(new File(((PostItemData) file).file.getAbsolutePath()));
                    }
                    pasteOrCancel(mode, selectedFilesPath, "Move");
                    mode.finish();
                    return true;

                case R.id.menu_delete:
                    AlertDialog.Builder deleteDialog = new AlertDialog.Builder(context);
                    deleteDialog.setTitle("Delete Files ?");
                    deleteDialog.setCancelable(false);
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            File path = null;
                            for (AbstractItem file : mfastAdapter.getSelectedItems()) {
                                if (file instanceof PostItemData) {
                                    path = new File(Objects.requireNonNull(((PostItemData) file).file.getParent()));
                                    deleteFiles(((PostItemData) file).file);
                                }
                            }

                            mode.finish();
                            new DisplayFilesAndFolders().execute(path);
                            Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    AlertDialog alertDialog_Delete = deleteDialog.create();
                    alertDialog_Delete.show();
                    return true;

                case R.id.menu_share:
                    shareFiles();
                    mode.finish();
                    return true;

                default:
                    return true;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (getSupportActionBar() != null)
                getSupportActionBar().show();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

    public class DisplayFilesAndFolders extends AsyncTask<File, Void, List<File>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Loading...");
            progressDialog.show();

            tvNoFile.setVisibility(View.GONE);
            recFile.setVisibility(View.VISIBLE);

        }

        @Override
        protected List<File> doInBackground(File... files) {

            List<File> listOfFiles = new ArrayList<>();
            File[] filesAndFolder = files[0].listFiles();
            if (filesAndFolder != null)
                Collections.addAll(listOfFiles, filesAndFolder);

            return listOfFiles;
        }

        @Override
        protected void onPostExecute(List<File> listOfFiles) {
            super.onPostExecute(listOfFiles);

            handleList(listOfFiles);
            progressDialog.dismiss();
        }
    }

    private class CopyFiles extends AsyncTask<ArrayList<File>, String, File> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Pasting Files...");

            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values[0].equals("1"))
                Toast.makeText(context, "The destination folder is a subfolder of the source folder.", Toast.LENGTH_SHORT).show();
            else if (values[0].equals("0"))
                Toast.makeText(context, "Already Exists.", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected File doInBackground(ArrayList<File>... arrayLists) {
            File destPath = null;
            for (File pathSrc : arrayLists[0]) {
                destPath = new File(currentFilePath + pathSrc.getAbsolutePath().substring(pathSrc.getAbsolutePath().lastIndexOf("/")));
                if (pathSrc.getAbsolutePath().equals(destPath.getAbsolutePath())) {
                    publishProgress("0");
                } else if (pathSrc.getAbsolutePath().equals(destPath.getParent())) {
                    publishProgress("1");
                } else {
                    try {
                        copyFileUsingStream(pathSrc, destPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (destPath != null && destPath.getParent() != null)
                return new File(destPath.getParent());
            else
                return null;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);

            progressDialog.dismiss();
            new DisplayFilesAndFolders().execute(file);
        }
    }
}