package com.example.liquormanagement;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class RecordListActivity extends AppCompatActivity {

    ListView mListView;
    ArrayList<Model> mList;
    RecordListAdapter mAdapter = null;

    ImageView imageViewIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("白酒列表");

        mListView = findViewById(R.id.listView);
        mList = new ArrayList<>();
        mAdapter = new RecordListAdapter(this, R.layout.row, mList);
        mListView.setAdapter(mAdapter);

        //从sqlite查询数据
        Cursor cursor = MainActivity.mSQLiteHelper.getData("SELECT * FROM RECORD");
        mList.clear();
        while (cursor.moveToNext()){
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            String descripiton = cursor.getString(2);
            byte[] image  = cursor.getBlob(3);
            //拼接后add to list
            mList.add(new Model(id, name, descripiton, image));
        }
        mAdapter.notifyDataSetChanged();
        if (mList.size()==0){
            //如果数据库对应的表中无数据，即listview为空时
            Toast.makeText(this, "无记录...", Toast.LENGTH_SHORT).show();
        }

        mListView.setOnItemLongClickListener((adapterView, view, position, l) -> {
            //长按显示更新或删除操作
            final CharSequence[] items = {"更新", "删除"};

            AlertDialog.Builder dialog = new AlertDialog.Builder(RecordListActivity.this);

            dialog.setTitle("请选择要进行的操作");
            dialog.setItems(items, (dialogInterface, i) -> {
                if (i == 0){
                    //更新操作
                    Cursor c = MainActivity.mSQLiteHelper.getData("SELECT id FROM RECORD");
                    ArrayList<Integer> arrID = new ArrayList<>();
                    while (c.moveToNext()){
                        arrID.add(c.getInt(0));
                    }
                    //show update dialog
                    showDialogUpdate(RecordListActivity.this, arrID.get(position));
                }
                if (i==1){
                    //删除操作
                    Cursor c = MainActivity.mSQLiteHelper.getData("SELECT id FROM RECORD");
                    ArrayList<Integer> arrID = new ArrayList<>();
                    while (c.moveToNext()){
                        arrID.add(c.getInt(0));
                    }
                    showDialogDelete(arrID.get(position));
                }
            });
            dialog.show();
            return true;
        });


    }

    private void showDialogDelete(final int idRecord) {
        AlertDialog.Builder dialogDelete = new AlertDialog.Builder(RecordListActivity.this);
        dialogDelete.setTitle("Warning!!");
        dialogDelete.setMessage("确定要删除吗?");
        dialogDelete.setPositiveButton("OK", (dialogInterface, i) -> {
            try {
                MainActivity.mSQLiteHelper.deleteData(idRecord);
                Toast.makeText(RecordListActivity.this, "Delete successfully", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e){
                Log.e("error", e.getMessage());
            }
            updateRecordList();
        });
        dialogDelete.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        dialogDelete.show();
    }

    private void showDialogUpdate(Activity activity, final int position){
        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.update_dialog);
        dialog.setTitle("Update");

        imageViewIcon = dialog.findViewById(R.id.imageViewRecord);
        final EditText edtName = dialog.findViewById(R.id.edtName);
        final EditText edtDescription = dialog.findViewById(R.id.edtDescription);
        Button btnUpdate = dialog.findViewById(R.id.btnUpdate);

        //set width of dialog
        int width = (int)(activity.getResources().getDisplayMetrics().widthPixels*0.95);
        //set hieght of dialog
        int height = (int)(activity.getResources().getDisplayMetrics().heightPixels*0.7);
        dialog.getWindow().setLayout(width,height);
        dialog.show();

        //在update_dialog视图中点击图片时
        imageViewIcon.setOnClickListener(view -> {
            //检查外部存储权限
            ActivityCompat.requestPermissions(
                    RecordListActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    888
            );
        });
        btnUpdate.setOnClickListener(view -> {
            try {
                MainActivity.mSQLiteHelper.updateData(
                        edtName.getText().toString().trim(),
                        edtDescription.getText().toString().trim(),
                        MainActivity.imageViewToByte(imageViewIcon),
                        position
                );
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "Update Successfull", Toast.LENGTH_SHORT).show();
            }
            catch (Exception error){
                Log.e("Update error", error.getMessage());
            }
            updateRecordList();
        });

    }

    private void updateRecordList() {
        //获取sqlite数据后更新
        Cursor cursor = MainActivity.mSQLiteHelper.getData("SELECT * FROM RECORD");
        mList.clear();
        while (cursor.moveToNext()){
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            String description = cursor.getString(2);
            byte[] image = cursor.getBlob(3);

            mList.add(new Model(id,name,description,image));
        }
        mAdapter.notifyDataSetChanged();
    }

//test
    public static byte[] imageViewToByte(ImageView image) {
        Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 888){
            if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //gallery intent
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, 888);
            }
            else {
                Toast.makeText(this, "Don't have permission to access file location", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 888 && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON) //enable image guidlines
                    .setAspectRatio(1,1)// image will be square
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result =CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                Uri resultUri = result.getUri();
                //set image choosed from gallery to image view
                imageViewIcon.setImageURI(resultUri);
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error = result.getError();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
