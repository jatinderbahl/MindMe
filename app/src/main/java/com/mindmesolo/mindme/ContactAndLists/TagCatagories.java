package com.mindmesolo.mindme.ContactAndLists;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mindmesolo.mindme.OrganizationModel;
import com.mindmesolo.mindme.R;
import com.mindmesolo.mindme.helper.Contacts;
import com.mindmesolo.mindme.helper.CustomHurlStack;
import com.mindmesolo.mindme.helper.DataHelper;
import com.mindmesolo.mindme.helper.SqliteDataBaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by User1 on 8/22/2016.
 */
public class TagCatagories extends Fragment {

    private static final String TAG = "TagCatagories";

    ListView list;

    String StoredContactid;

    SqliteDataBaseHelper sqliteDataBaseHelper;

    MyCustomAdapter dataAdapter = null;

    DataHelper dataHelper = new DataHelper();

    ApiCallsInterface mCallback;

    boolean _areFragmentLoaded = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.list, container, false);

        list = (ListView) rootView.findViewById(R.id.listView);

        Bundle extras = getActivity().getIntent().getExtras();

        StoredContactid = extras.getString("contactid");
        setupListView();
        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void setupListView() {
        ArrayList<Contacts> ContactsList = new ArrayList<Contacts>();

        sqliteDataBaseHelper = new SqliteDataBaseHelper(getContext());

        ArrayList<String> AllNames = sqliteDataBaseHelper.getAllTagNames();

        Collections.sort(AllNames, String.CASE_INSENSITIVE_ORDER);

        for (String Name : AllNames) {

            String ID = sqliteDataBaseHelper.getTagIdFromName(Name);

            ArrayList<String> AllContacts = dataHelper.stringToArrayList(sqliteDataBaseHelper.getAllContactsFromTag(ID));

            ContactsList.add(new Contacts(String.valueOf(AllContacts.size()), Name, AllContacts, ID, getContactInList(AllContacts)));

        }

        dataAdapter = new MyCustomAdapter(getContext(), R.layout.layout_contact_row, ContactsList);

        list.setAdapter(dataAdapter);

    }

    private boolean getContactInList(ArrayList<String> ListContacts) {
        if (ListContacts.contains(StoredContactid)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupListView();
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (ApiCallsInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement IFragmentToActivity");
        }
    }

    private void AddContactInTags(final Contacts contacts, int RequestMethod) {

        JSONArray ContactArray = new JSONArray();

        ContactArray.put(StoredContactid);

        final String jsonStr = ContactArray.toString();

        String URl = OrganizationModel.getApiBaseUrl() + getOrgId() + "/tags/" + contacts.getId() + "/contacts";


        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(RequestMethod, URl,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONArray mJSONArray = response.getJSONArray("contactIds");

                            contacts.setCode(String.valueOf(mJSONArray.length()));

                            long time = System.currentTimeMillis();

                            sqliteDataBaseHelper.changeContactUpdatedON(StoredContactid, String.valueOf(time));

                            sqliteDataBaseHelper.updateTagContactsAndLength(contacts.getId(), mJSONArray.toString(), (mJSONArray.length()));

                            dataAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, error.toString());
                        String json = null;
                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null) {
                            switch (response.statusCode) {
                                case 400:
                                case 405:
                                    json = new String(response.data);
                                    json = dataHelper.trimMessage(json, "message");
                                    if (json != null) dataHelper.displayMessage(json);
                                    break;
                            }

                        }


                    }
                }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return jsonStr == null ? null : jsonStr.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                            jsonStr, "utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Basic " + GetApiAccess());
                return headers;
            }
        };
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(
                9000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonArrayRequest);
    }

    private void DeleteContactInTags(final Contacts contacts, int RequestMethod) {

        JSONArray ContactArray = new JSONArray();

        ContactArray.put(StoredContactid);

        final String jsonStr = ContactArray.toString();

        String URl = OrganizationModel.getApiBaseUrl() + getOrgId() + "/tags/" + contacts.getId() + "/contacts";

        CustomHurlStack customHurlStack = new CustomHurlStack();

        RequestQueue requestQueue = Volley.newRequestQueue(getContext(), customHurlStack);

        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(RequestMethod, URl,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            JSONArray mJSONArray = response.getJSONArray("contactIds");

                            contacts.setCode(String.valueOf(mJSONArray.length()));

                            long time = System.currentTimeMillis();

                            sqliteDataBaseHelper.changeContactUpdatedON(StoredContactid, String.valueOf(time));

                            sqliteDataBaseHelper.updateTagContactsAndLength(contacts.getId(), mJSONArray.toString(), (mJSONArray.length()));

                            dataAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, error.toString());
                        String json = null;
                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null) {
                            switch (response.statusCode) {
                                case 400:
                                case 405:
                                    json = new String(response.data);
                                    json = dataHelper.trimMessage(json, "message");
                                    if (json != null) dataHelper.displayMessage(json);
                                    break;
                            }
                        }
                    }
                }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return jsonStr == null ? null : jsonStr.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                            jsonStr, "utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Basic " + GetApiAccess());
                return headers;
            }
        };
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(
                9000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonArrayRequest);
    }

    private String getOrgId() {
        SharedPreferences pref = getContext().getSharedPreferences("Organization", Context.MODE_PRIVATE);
        String StoredOrgid = pref.getString("OrgId", null);
        return StoredOrgid;
    }

    public String GetApiAccess() {
        SharedPreferences pref1 = getContext().getSharedPreferences("UserLogin", Context.MODE_PRIVATE);
        String email = pref1.getString("Email", null);
        String password = pref1.getString("Password", null);
        String token = email + ":" + password;
        byte[] data = null;
        try {
            data = token.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        final String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        final String finalToken = base64.replace("\n", "");
        return finalToken;
    }

    private void updateContactFromList(Contacts contacts) {

        ArrayList<String> TempArray = new ArrayList<String>();

        TempArray.addAll(contacts.getContacts());

        if (contacts.isSelected()) {

            TempArray.add(StoredContactid);

        } else {
            TempArray.remove(StoredContactid);
        }

        JSONArray mJSONArray = new JSONArray();

        for (String item : TempArray) {

            mJSONArray.put(item.toString());

        }
        contacts.setCode(String.valueOf(TempArray.size()));

        contacts.setContacts(TempArray);

        long time = System.currentTimeMillis();

        sqliteDataBaseHelper.changeContactUpdatedON(StoredContactid, String.valueOf(time));
        sqliteDataBaseHelper.updateTagContactsAndLength(contacts.getId(), mJSONArray.toString(), (mJSONArray.length()));

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("contactIds", mJSONArray);
            jsonObject.put("code", contacts.getName());
            jsonObject.put("id", contacts.getId());
            jsonObject.put("name", contacts.getName());
            jsonObject.put("status", "ACTIVE");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mCallback.AddContact_In_List_Interest_Tags(jsonObject, "tags");
        dataAdapter.notifyDataSetChanged();
    }

    //---------------------CUSTOM ADAPTER FOR LIST VIEW ----------//
    private class MyCustomAdapter extends ArrayAdapter<Contacts> {

        private ArrayList<Contacts> ListD;

        public MyCustomAdapter(Context context, int textViewResourceId, ArrayList<Contacts> List) {

            super(context, textViewResourceId, List);

            this.ListD = new ArrayList<Contacts>();

            this.ListD.addAll(List);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            MyCustomAdapter.ViewHolder holder = null;

            if (convertView == null) {

                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);

                convertView = vi.inflate(R.layout.catagorieslayout, null);

                holder = new MyCustomAdapter.ViewHolder();

                holder.name = (TextView) convertView.findViewById(R.id.txtName);

                holder.len = (TextView) convertView.findViewById(R.id.textNumber);

                holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);

                convertView.setTag(holder);

                holder.checkBox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;

                        Contacts contacts = (Contacts) cb.getTag();

                        int POST = 1;

                        int DELETE = 3;

                        if (cb.isChecked()) {
                            //Request method POST = 1;
                            AddContactInTags(contacts, POST);
                            contacts.setSelected(true);
                        } else {
                            DeleteContactInTags(contacts, DELETE);
                            contacts.setSelected(false);
                        }
                    }
                });
            } else {
                holder = (MyCustomAdapter.ViewHolder) convertView.getTag();
            }

            Contacts listsDB = ListD.get(position);

            holder.name.setText(listsDB.getName());

            holder.len.setText(listsDB.getCode());

            holder.checkBox.setChecked(listsDB.isSelected());

            holder.checkBox.setTag(listsDB);

            return convertView;
        }

        private class ViewHolder {
            TextView name, len;
            CheckBox checkBox;
        }
    }

}
