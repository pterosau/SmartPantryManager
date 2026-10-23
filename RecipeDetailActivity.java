package com.example.smartpantry;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Recipe Detail screen: full ingredient list and method for the selected recipe. */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "recipe_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        setTitle("Recipe");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        Recipe r = new DatabaseHelper(this).getRecipe(id);
        if (r == null) {          // defensive: should never happen
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvName)).setText(r.name);

        StringBuilder sb = new StringBuilder();
        for (RecipeIngredient ri : r.ingredients) {
            sb.append("\u2022 ").append(Ingredient.formatQty(ri.quantity)).append(" ")
              .append(ri.unit).append("  ").append(ri.name).append("\n");
        }
        ((TextView) findViewById(R.id.tvIngredients)).setText(sb.toString().trim());
        ((TextView) findViewById(R.id.tvSteps)).setText(r.steps);
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
