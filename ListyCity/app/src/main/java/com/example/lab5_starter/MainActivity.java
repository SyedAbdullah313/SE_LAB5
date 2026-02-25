package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;
    private FirebaseFirestore db;   // FIX: was FiresbaseFirestore (compile error)
    private CollectionReference citiesRef;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db= FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);
//
//        addDummyData();
        citiesRef.addSnapshotListener((QuerySnapshot value, FirebaseFirestoreException error) -> {

            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            if (value != null) {
                cityArrayList.clear();

                for (QueryDocumentSnapshot doc : value) {
                    String cityName = doc.getString("name");
                    String provinceName = doc.getString("province");

                    cityArrayList.add(new City(cityName, provinceName));
                }

                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });
        cityListView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            City cityToDelete = cityArrayAdapter.getItem(position);
            if (cityToDelete == null) return true;

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete City")
                    .setMessage("Delete " + cityToDelete.getName() + " (" + cityToDelete.getProvince() + ")?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        citiesRef.document(cityToDelete.getName())
                                .delete()
                                .addOnSuccessListener(unused ->
                                        Log.d("Firestore", "Deleted " + cityToDelete.getName()))
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Delete failed", e));
                        // NOTE: don’t manually remove from cityArrayList here.
                        // Your snapshotListener will update the ListView automatically.
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true; // long press handled
        });

    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName(); // ADDED: keep old doc id before changing it

        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // Updating the database using delete + addition
        if (oldName != null && !oldName.equals(title)) { // ADDED: if name changed, doc id changed
            citiesRef.document(oldName).delete();         // ADDED: remove old doc
        }
        DocumentReference docRef = citiesRef.document(city.getName()); // ADDED: write updated doc
        docRef.set(city);                                               // ADDED
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();
        DocumentReference docRef= citiesRef.document(city.getName());
        docRef.set(city);

    }

//    public void addDummyData(){
//        City m1 = new City("Edmonton", "AB");
//        City m2 = new City("Vancouver", "BC");
//        cityArrayList.add(m1);
//        cityArrayList.add(m2);
//        cityArrayAdapter.notifyDataSetChanged();
//    }
}