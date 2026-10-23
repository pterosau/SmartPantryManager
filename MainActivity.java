package com.example.smartpantry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/** Pantry List screen (home). Shows every ingredient from the database. */
public class MainActivity extends AppCompatActivity implements PantryAdapter.Listener {

    public static final String PREFS = "settings";
    public static final String PREF_EXPIRY_ALERTS = "expiry_alerts";

    private DatabaseHelper db;
    private PantryAdapter adapter;
    private TextView tvEmpty, tvBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("My Pantry");

        db = new DatabaseHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvBanner = findViewById(R.id.tvBanner);

        RecyclerView rv = findViewById(R.id.rvPantry);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(new ArrayList<>(), this);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        // Explicit Intent: open the Add/Edit screen in "add" mode
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddEditIngredientActivity.class)));
    }

    /** Reload every time we return to this screen so add/edit/delete are reflected immediately. */
    @Override
    protected void onResume() {
        super.onResume();
        loadPantry();
    }

    private void loadPantry() {
        List<Ingredient> items = db.getAllIngredients();
        adapter.setItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        // Expiring-soon banner (controlled by the Settings toggle)
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int soon = 0;
        for (Ingredient i : items) {
            Integer d = i.daysUntilExpiry();
            if (d != null && d <= 3) soon++;
        }
        if (prefs.getBoolean(PREF_EXPIRY_ALERTS, true) && soon > 0) {
            tvBanner.setText(soon + " item(s) expired or expiring within 3 days!");
            tvBanner.setVisibility(View.VISIBLE);
        } else {
            tvBanner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEdit(Ingredient item) {
        Intent i = new Intent(this, AddEditIngredientActivity.class);
        i.putExtra(AddEditIngredientActivity.EXTRA_ID, item.id); // pass data via Intent extra
        startActivity(i);
    }

    @Override
    public void onDelete(Ingredient item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete ingredient")
                .setMessage("Remove " + item.name + " from your pantry?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteIngredient(item.id);
                    Toast.makeText(this, item.name + " deleted", Toast.LENGTH_SHORT).show();
                    loadPantry();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_suggested) {
            startActivity(new Intent(this, SuggestedRecipesActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
