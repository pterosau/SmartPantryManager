package com.example.smartpantry;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

/** Add / Edit Ingredient screen. If an ingredient id is passed in the Intent, it runs in edit mode. */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "ingredient_id";
    private static final String[] UNITS = {"g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs"};

    private DatabaseHelper db;
    private EditText etName, etQuantity, etExpiry;
    private Spinner spUnit;
    private long editingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DatabaseHelper(this);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        spUnit = findViewById(R.id.spUnit);
        Button btnSave = findViewById(R.id.btnSave);

        spUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, UNITS));

        // Read the Intent extra: -1 means "add", anything else means "edit that row"
        editingId = getIntent().getLongExtra(EXTRA_ID, -1);
        if (editingId != -1) {
            setTitle("Edit Ingredient");
            Ingredient existing = db.getIngredient(editingId);
            if (existing != null) {
                etName.setText(existing.name);
                etQuantity.setText(Ingredient.formatQty(existing.quantity));
                etExpiry.setText(existing.expiry == null ? "" : existing.expiry);
                for (int i = 0; i < UNITS.length; i++) {
                    if (UNITS[i].equals(existing.unit)) spUnit.setSelection(i);
                }
            }
        } else {
            setTitle("Add Ingredient");
        }

        etExpiry.setOnClickListener(v -> showDatePicker());
        etExpiry.setOnLongClickListener(v -> { etExpiry.setText(""); return true; });
        btnSave.setOnClickListener(v -> save());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) ->
                etExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** Validate the form, then INSERT (add mode) or UPDATE (edit mode). */
    private void save() {
        String name = etName.getText().toString().trim();
        String qtyText = etQuantity.getText().toString().trim();

        // ---- input validation ----
        if (name.isEmpty()) {
            etName.setError("Please enter an ingredient name");
            return;
        }
        if (name.length() < 2) {
            etName.setError("Name must be at least 2 characters");
            return;
        }
        double qty;
        try {
            qty = Double.parseDouble(qtyText);
        } catch (NumberFormatException e) {
            etQuantity.setError("Please enter a valid number");
            return;
        }
        if (qty <= 0) {
            etQuantity.setError("Quantity must be greater than 0");
            return;
        }

        String unit = spUnit.getSelectedItem().toString();
        String expiry = etExpiry.getText().toString().trim(); // optional

        if (editingId == -1) {
            db.addIngredient(new Ingredient(0, name, qty, unit, expiry));
            Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
        } else {
            db.updateIngredient(new Ingredient(editingId, name, qty, unit, expiry));
            Toast.makeText(this, "Ingredient updated", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
