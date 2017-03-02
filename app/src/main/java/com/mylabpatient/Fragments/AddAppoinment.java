package com.mylabpatient.Fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mylabpatient.ConstantClasses.CustomeToast;
import com.mylabpatient.ConstantClasses.NetworkCheck;
import com.mylabpatient.DataSource.GenerateNewId;
import com.mylabpatient.DataSource.LabDetailsDataSource;
import com.mylabpatient.Appoinment;
import com.mylabpatient.HomeVisits;
import com.mylabpatient.R;
import com.mylabpatient.SharedPreference.PreferenceServices;
import com.mylabpatient.WebServices.Webservices;

import org.ksoap2.serialization.SoapObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddAppoinment extends Fragment implements View.OnClickListener {

    EditText edtContact,edtArea,edtAddress,edtRemark;
    TextView txtFilename;
    Spinner spiDate,spiTime,spiPreferdLab;
    Button btnBook,btnClear,btnBrowse;
    SweetAlertDialog progressDialog;
    CustomeToast customeToast=new CustomeToast();
    NetworkCheck networkCheck;
    Context mCtx;
    ArrayList<LabDetailsDataSource> arrayList_LabDetails=new ArrayList<>();
    ArrayList<String> arrayList_LabName=new ArrayList<>();
    ArrayList<String> arrayList_Dates=new ArrayList<>();
    public static final String TAG="AddAppoinmentFragment";
    Dialog dialog;
    static final int REQUEST_File_GET = 1;

    //for storing the file name and path
    String displayName,MaxAppoinmentId="",NextId="";

    //Send Data String Declaration
    String SendEmail,SendPatientContact,SendDate,SendTime,SendContact,SendArea,SendAddress,
            SendPreferLab,SendFileName,SendFilePath="",SendName,SendId,SendBookedFlag,SendLabCode,SendVisitId,SendRemark,SendAge;

    //these string for send masseges
    String SendContactNew,SendContactMsg,SendLabName,ShourtUrl,SendLabContact;

    public AddAppoinment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view= inflater.inflate(R.layout.fragment_add_appoinment, container, false);
        //initializing widget here
        init(view);

        //getting Dates for spinner
        SettingDate();

        //initializing the network check constant class
        networkCheck=new NetworkCheck(mCtx);

        //initializing the progress dialog
        progressDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.PROGRESS_TYPE);
        progressDialog.getProgressHelper().setBarColor(Color.parseColor("#EF6C00"));
        progressDialog.setTitleText("Loading");
        progressDialog.setCancelable(false);

        if(networkCheck.isConnectinToInternet()){
            //getting Lab Details
            new LabDetailsAsyn().execute();
            //getting MaxAppoinment Id
            new GetMaxHomevisitIdAsyn().execute();
        }else{
            customeToast.CustomeToastSetting(mCtx,"Internet Connection Required");
        }
        return view;
    }
    private void init(View view){


        edtContact=(EditText)view.findViewById(R.id.edtAppoinmentContact);
        edtArea=(EditText)view.findViewById(R.id.edtAppoinmentArea);
        edtAddress=(EditText)view.findViewById(R.id.edtAppoinmentAddress);
        edtRemark=(EditText)view.findViewById(R.id.edtAppoinmentRemark);

        spiDate=(Spinner)view.findViewById(R.id.spiAppoinmentDate);
        spiTime=(Spinner)view.findViewById(R.id.spiAppoinmentTime);
        spiPreferdLab=(Spinner)view.findViewById(R.id.spiAppoinmentPreferLab);

        btnBook=(Button)view.findViewById(R.id.btnAppoinmentBook);
        btnClear=(Button)view.findViewById(R.id.btnAppoinmentClear);
        btnBrowse=(Button)view.findViewById(R.id.btnAppoinmentPrescriptionFileName);

        txtFilename=(TextView)view.findViewById(R.id.txtAppoinmentPrescriptionName);

        btnBook.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnBrowse.setOnClickListener(this);
    }
    private void SettingDate(){

        //setting basic info
        edtContact.setText(Appoinment.map.get(PreferenceServices.Contact));
        edtArea.setText(Appoinment.map.get(PreferenceServices.Area));
        edtAddress.setText(Appoinment.map.get(PreferenceServices.Address));

        //adding the Date of next 15 days in array List
        arrayList_Dates.add("Select Date");
        SimpleDateFormat dateFormat= new SimpleDateFormat("dd/MM/yyyy");
        Calendar currentCal = Calendar.getInstance();
        for(int i=1;i<16;i++){
            currentCal.add(Calendar.DATE, 1);
            String incrementDate=dateFormat.format(currentCal.getTime());
            Log.e("StartDate",incrementDate);
            arrayList_Dates.add(incrementDate);
        }

        ArrayAdapter DatesAdapter=new ArrayAdapter(mCtx,android.R.layout.simple_list_item_1,arrayList_Dates);
        spiDate.setAdapter(DatesAdapter);
    }
    private void SendingData(){

        if(spiDate.getSelectedItemPosition() == 0){
            ((TextView)spiDate.getSelectedView()).setError("Select Date");
            customeToast.CustomeToastSetting(mCtx,"Select Date");
        }else if(spiTime.getSelectedItemPosition() == 0){
            ((TextView)spiTime.getSelectedView()).setError("Select Time");
            customeToast.CustomeToastSetting(mCtx,"Select Time");
        }else if(edtContact.getText().toString().length() == 0 && edtContact.getText().toString().equals("")){
            edtContact.setError("Enter Contact Number");
        }else if(edtArea.getText().toString().length() == 0 && edtArea.getText().toString().equals("")){
            edtArea.setError("Enter Area");
        }else if(edtAddress.getText().toString().length() == 0 && edtAddress.getText().toString().equals("")){
            edtAddress.setError("Enter Address");
        }else if(spiPreferdLab.getSelectedItemPosition() ==0){
            ((TextView)spiPreferdLab.getSelectedView()).setError("Select Prefered Lab");
            customeToast.CustomeToastSetting(mCtx,"Select Prefered Lab");
        }else{

            SendPatientContact=HomeVisits.map.get(PreferenceServices.Contact);
            SendEmail= HomeVisits.map.get(PreferenceServices.Email);
            SendDate=spiDate.getSelectedItem().toString();
            SendTime=spiTime.getSelectedItem().toString();
            SendContact=edtContact.getText().toString();
            SendArea=edtArea.getText().toString();
            SendAddress=edtAddress.getText().toString();
            SendPreferLab=spiPreferdLab.getSelectedItem().toString();
            SendRemark=edtRemark.getText().toString();
            SendFileName=displayName;
            //if it contatin any change then save else it contact black
            //SendFilePath;
            SendId=HomeVisits.map.get(PreferenceServices.PatientId);
            SendName=HomeVisits.map.get(PreferenceServices.PatientName);
            SendBookedFlag="true";
            SendAge=HomeVisits.map.get(PreferenceServices.Age);
            //here getting the spinner positin
            int index=spiPreferdLab.getSelectedItemPosition();
            LabDetailsDataSource labDetailsDataSource=arrayList_LabDetails.get(index-1);
            SendLabCode=labDetailsDataSource.getLabcode();
            Log.e("LabCode",SendLabCode);
            SendVisitId=NextId;

            //these value for sending massege
            SendContactNew=edtContact.getText().toString();
            SendContactMsg=HomeVisits.map.get(PreferenceServices.Contact);
            SendLabName=labDetailsDataSource.getLabname();
            SendLabContact=labDetailsDataSource.getLabContact();

            //if internet is available call the method else show the error msg
            if(networkCheck.isConnectinToInternet()){
                new InserNewAppoinmentAsyn().execute();
                new SendNewAppoinmentMsgAsyn().execute();
            }else{
                customeToast.CustomeToastSetting(mCtx,"Internet Connection Required");
            }
        }
    }

    private void showFileChooser() {
        
        dialog=new Dialog(mCtx);
        dialog.setContentView(R.layout.filechooserdialog);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        ImageView imageView=(ImageView)dialog.findViewById(R.id.filechooserclosedialog);
        Button btnpdf=(Button)dialog.findViewById(R.id.btn_pdf_dialog);
        Button btnjpg=(Button)dialog.findViewById(R.id.btn_jpg_dialog);

        final PackageManager packageManager = mCtx.getPackageManager();

        btnpdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

                //start the file chooser
                Intent intentPDF = new Intent(Intent.ACTION_GET_CONTENT);
                intentPDF.setType("application/pdf");
                intentPDF.addCategory(Intent.CATEGORY_OPENABLE);

                List activitiesPDF = packageManager.queryIntentActivities(intentPDF,
                        PackageManager.MATCH_DEFAULT_ONLY);
                boolean isIntentSafePDF = activitiesPDF.size() > 0;
                if (!isIntentSafePDF){

                    // Potentially direct the user to the Market with a Dialog
                    progressDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE);
                    progressDialog.getProgressHelper().setBarColor(Color.parseColor("#EF6C00"));
                    progressDialog.setTitleText("Oops...");
                    progressDialog.setContentText("You have No Files!");
                    progressDialog.show();

                }else{
                    startActivityForResult(intentPDF,REQUEST_File_GET);
                }

            }
        });

        btnjpg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //dismiss the dialog
                dialog.dismiss();
                //start the file chooser
                Intent intentJpg = new Intent(Intent.ACTION_GET_CONTENT);
                intentJpg.setType("image/jpeg");
                intentJpg.addCategory(Intent.CATEGORY_OPENABLE);

                List activitiesTxt = packageManager.queryIntentActivities(intentJpg,
                        PackageManager.MATCH_DEFAULT_ONLY);
                boolean isIntentSafeTxt = activitiesTxt.size() > 0;

                if (!isIntentSafeTxt){

                    // Potentially direct the user to the Market with a Dialog
                    progressDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE);
                    progressDialog.getProgressHelper().setBarColor(Color.parseColor("#EF6C00"));
                    progressDialog.setTitleText("Oops...");
                    progressDialog.setContentText("You have No Files!");
                    progressDialog.show();

                }else{
                    startActivityForResult(intentJpg,REQUEST_File_GET);
                }

            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_File_GET && resultCode == RESULT_OK){

            Uri uri = data.getData();
            String uriString = uri.toString();
            File myFile = new File(uriString);
            String path = myFile.getAbsolutePath();
            displayName = null;

            //for setting the Name of File on Text View
            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.getName();
            }
            //setting a file Name
            txtFilename.setText(displayName);
            // BufferedReader reader = null;
            try{
                //this is a path where is store after selection
                String Copyedpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PatientMylab";
                File dir = new File(Copyedpath);
                if(!dir.exists())
                    dir.mkdirs();
                //this are the path and name in which its store
                File file=new File(dir,displayName);

                file.createNewFile();
                copyFile(new File(path),file);

                Log.e("From PAth",String.valueOf(path));
                Log.e("To Path",String .valueOf(file));
                SendFilePath=String.valueOf(path);
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }
        }
    }

    //copy file from selected path to new file the name is MyLabPatient
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCtx=getActivity();
    }

    @Override
    public void onClick(View v) {
       
        switch (v.getId()){
            case R.id.btnAppoinmentBook:
                SendingData();
                break;

            case R.id.btnAppoinmentClear:
                customeToast.CustomeToastSetting(mCtx,"Clear");
                break;

            case R.id.btnAppoinmentPrescriptionFileName:
                showFileChooser();
                break;
        }
    }

    //method for generate next id
    private String getId(String mHomevisitid){
        String NextId="";
        if(mHomevisitid ==null && mHomevisitid.equals("0")){
            NextId="AA001";
        }else{
            NextId=new GenerateNewId().GenerateID(mHomevisitid);
        }
        Log.e("NextId",NextId);
        return NextId;
    }


    class LabDetailsAsyn extends AsyncTask<Void,Void,SoapObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try{
                if(progressDialog != null && !progressDialog.isShowing())
                    progressDialog.show();

            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }
        }

        @Override
        protected SoapObject doInBackground(Void... params) {

            Webservices webservices=new Webservices();
            SoapObject soapObject=webservices.LabDetails();
            return soapObject;
        }

        @Override
        protected void onPostExecute(SoapObject soapObject) {
            super.onPostExecute(soapObject);
            try{
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                if(soapObject == null){
                    Log.e(TAG,soapObject.toString());
                }else{
                    Log.e(TAG,soapObject.toString());

                    SoapObject soap = (SoapObject) soapObject.getProperty(0);
                    System.out.println("soap : : "+ soap.getPropertyCount() +"  "+ soap.toString() );
                    Log.e("Soap",soap.getPropertyCount() +"  "+ soap.toString() );

                    SoapObject res = (SoapObject) soap.getProperty(0);
                    System.out.println("res : : "+ res.getPropertyCount() +"  "+ res.toString() );
                    Log.e("res",res.getPropertyCount() +"  "+ res.toString() );

                    SoapObject res2 = (SoapObject) soap.getProperty(1);
                    System.out.println("res2 : : "+ res2.getPropertyCount() +"  "+ res2.toString() );
                    Log.e("res2",res2.getPropertyCount() +"  "+ res2.toString() );

                    SoapObject last = (SoapObject) res2.getProperty(0);
                    System.out.println("last : : "+ last.getPropertyCount() +"  "+ last.toString() );
                    Log.e("Soap",last.getPropertyCount() +"  "+ last.toString() );

                    int count=last.getPropertyCount();
                    Log.e("CancelVisit Count : ",String.valueOf(count));

                    arrayList_LabName.add("Select Lab");

                    for(int i=0; i < last.getPropertyCount(); i++) {
                        //when data was increase last property was increase that y getting in the for loops
                        SoapObject last2= (SoapObject) last.getProperty(i);

                        String Labname=last2.getProperty("LabName").toString();
                        String id=last2.getProperty("id").toString();
                        String Labcode=last2.getProperty("LabCode").toString();
                        String Labname1=last2.getProperty("LabName1").toString();
                        String LabAddress=last2.getProperty("LabAddress").toString();
                        String LabEmail=last2.getProperty("LabEmail").toString();
                        String LabContact=last2.getProperty("LabContact").toString();
                        String LabWebservicesUrl=last2.getProperty("LabWebserviceURL").toString();

                        arrayList_LabDetails.add(new LabDetailsDataSource(Labname,id,Labcode,Labname1,LabAddress,LabEmail,
                                LabContact,LabWebservicesUrl));

                        arrayList_LabName.add(Labname);

                    }

                    ArrayAdapter LabnameAdapter=new ArrayAdapter(mCtx,android.R.layout.simple_list_item_1,arrayList_LabName);
                    spiPreferdLab.setAdapter(LabnameAdapter);
                }
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }
        }
    }
    class GetMaxHomevisitIdAsyn extends AsyncTask<Void,Void,SoapObject>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try{
                if(progressDialog != null && !progressDialog.isShowing())
                    progressDialog.show();

            }catch (Exception e){
                //Log.e(TAG,e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        protected SoapObject doInBackground(Void... params) {

            Webservices webservices=new Webservices();
            SoapObject soapObject=webservices.MAX_APPOINMENT_ID();
            return soapObject;
        }

        @Override
        protected void onPostExecute(SoapObject soapObject) {
            super.onPostExecute(soapObject);
            try{
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                if(soapObject == null){
                    Log.e(TAG,soapObject.toString());
                }else{
                    Log.e(TAG,soapObject.toString());

                    SoapObject soap = (SoapObject) soapObject.getProperty(0);
                    System.out.println("soap : : "+ soap.getPropertyCount() +"  "+ soap.toString() );
                    Log.e("Soap",soap.getPropertyCount() +"  "+ soap.toString() );

                    SoapObject res = (SoapObject) soap.getProperty(0);
                    System.out.println("res : : "+ res.getPropertyCount() +"  "+ res.toString() );
                    Log.e("res",res.getPropertyCount() +"  "+ res.toString() );

                    SoapObject res2 = (SoapObject) soap.getProperty(1);
                    System.out.println("res2 : : "+ res2.getPropertyCount() +"  "+ res2.toString() );
                    Log.e("res2",res2.getPropertyCount() +"  "+ res2.toString() );

                    SoapObject last = (SoapObject) res2.getProperty(0);
                    System.out.println("last : : "+ last.getPropertyCount() +"  "+ last.toString() );
                    Log.e("Soap",last.getPropertyCount() +"  "+ last.toString() );

                    for(int i=0; i < last.getPropertyCount(); i++) {
                        //when data was increase last property was increase that y getting in the for loops
                        SoapObject last2= (SoapObject) last.getProperty(i);
                        MaxAppoinmentId=last2.getProperty("maxId").toString();
                    }
                    Log.e("MAXAppoinmentId",MaxAppoinmentId);
                    //calling the next home visit method
                    NextId=getId(MaxAppoinmentId);
                    Log.e("NextId",NextId);
                }
            }catch (Exception e){
                //Log.e(TAG,e.getMessage());
                e.printStackTrace();
            }
        }
    }
    class InserNewAppoinmentAsyn extends AsyncTask<Void,Void,SoapObject>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try{
                if(progressDialog != null && !progressDialog.isShowing())
                    progressDialog.show();

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        protected SoapObject doInBackground(Void... params) {
            Webservices webservices=new Webservices();
            SoapObject soapObject=webservices.NEW_APPOINMENT(SendPatientContact,SendEmail,SendDate,SendTime,SendContact,SendArea,
                    SendAddress,SendPreferLab,SendRemark,SendFileName,SendFilePath,SendName,SendId,SendBookedFlag,
                    SendAge,SendLabCode,SendVisitId);
            return soapObject;
        }

        @Override
        protected void onPostExecute(SoapObject soapObject) {
            super.onPostExecute(soapObject);
            try{
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                //showing the massege
                customeToast.CustomeToastSetting(mCtx,"Appoinment Book Succesfully");

                if(soapObject == null){
                    Log.e(TAG,soapObject.toString());
                }else{
                    Log.e(TAG,soapObject.toString());
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    class SendNewAppoinmentMsgAsyn extends AsyncTask<Void,Void,SoapObject>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try{
                if(progressDialog != null && !progressDialog.isShowing())
                    progressDialog.show();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        protected SoapObject doInBackground(Void... params) {

            Webservices webservices=new Webservices();
            SoapObject soapObject=webservices.NEW_APPOINMENT_SENDING_MASSAGE(SendContactNew,SendContactMsg,
                    SendLabName,ShourtUrl,SendLabContact);
            return soapObject;
        }

        @Override
        protected void onPostExecute(SoapObject soapObject) {
            super.onPostExecute(soapObject);
            try{
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                if(soapObject == null){
                    Log.e(TAG,soapObject.toString());
                }else{
                    Log.e(TAG,soapObject.toString());
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
