package com.example.smartpantry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite database. Tables:
 *   pantry(id, name, quantity, unit, expiry)                -> user data, full CRUD
 *   recipes(id, name, steps)                                -> seeded on first run
 *   recipe_ingredients(id, recipe_id, name, quantity, unit) -> seeded on first run
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "smartpantry.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pantry (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, expiry TEXT)");
        db.execSQL("CREATE TABLE recipes (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, steps TEXT NOT NULL)");
        db.execSQL("CREATE TABLE recipe_ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "recipe_id INTEGER NOT NULL, name TEXT NOT NULL, quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL, FOREIGN KEY(recipe_id) REFERENCES recipes(id))");
        seedRecipes(db); // pre-load the recipe collection on first run
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS recipe_ingredients");
        db.execSQL("DROP TABLE IF EXISTS recipes");
        db.execSQL("DROP TABLE IF EXISTS pantry");
        onCreate(db);
    }

    // ===================== PANTRY CRUD =====================

    /** CREATE */
    public long addIngredient(Ingredient i) {
        return getWritableDatabase().insert("pantry", null, toValues(i));
    }

    /** READ (all) */
    public List<Ingredient> getAllIngredients() {
        List<Ingredient> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("pantry", null, null, null, null, null, "name COLLATE NOCASE");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    /** READ (one) */
    public Ingredient getIngredient(long id) {
        Cursor c = getReadableDatabase().query("pantry", null, "id=?", new String[]{String.valueOf(id)},
                null, null, null);
        Ingredient result = c.moveToFirst() ? fromCursor(c) : null;
        c.close();
        return result;
    }

    /** UPDATE */
    public int updateIngredient(Ingredient i) {
        return getWritableDatabase().update("pantry", toValues(i), "id=?", new String[]{String.valueOf(i.id)});
    }

    /** DELETE */
    public int deleteIngredient(long id) {
        return getWritableDatabase().delete("pantry", "id=?", new String[]{String.valueOf(id)});
    }

    private ContentValues toValues(Ingredient i) {
        ContentValues v = new ContentValues();
        v.put("name", i.name);
        v.put("quantity", i.quantity);
        v.put("unit", i.unit);
        v.put("expiry", i.expiry);
        return v;
    }

    private Ingredient fromCursor(Cursor c) {
        return new Ingredient(c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getString(c.getColumnIndexOrThrow("expiry")));
    }

    // ===================== RECIPES =====================

    /** All recipes, each with its ingredient list loaded. */
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        List<Recipe> list = new ArrayList<>();
        Cursor c = db.query("recipes", null, null, null, null, null, "name");
        while (c.moveToNext()) list.add(recipeFromCursor(db, c));
        c.close();
        return list;
    }

    public Recipe getRecipe(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("recipes", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        Recipe r = c.moveToFirst() ? recipeFromCursor(db, c) : null;
        c.close();
        return r;
    }

    private Recipe recipeFromCursor(SQLiteDatabase db, Cursor c) {
        Recipe r = new Recipe(c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("steps")));
        Cursor ic = db.query("recipe_ingredients", null, "recipe_id=?",
                new String[]{String.valueOf(r.id)}, null, null, null);
        while (ic.moveToNext()) {
            r.ingredients.add(new RecipeIngredient(
                    ic.getString(ic.getColumnIndexOrThrow("name")),
                    ic.getDouble(ic.getColumnIndexOrThrow("quantity")),
                    ic.getString(ic.getColumnIndexOrThrow("unit"))));
        }
        ic.close();
        return r;
    }

    // ===================== SEED DATA =====================

    /** Ingredient string format: "name|quantity|unit;name|quantity|unit;..." */
    private void addRecipe(SQLiteDatabase db, String name, String steps, String ingredients) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("steps", steps);
        long id = db.insert("recipes", null, v);
        for (String part : ingredients.split(";")) {
            String[] p = part.split("\\|");
            ContentValues iv = new ContentValues();
            iv.put("recipe_id", id);
            iv.put("name", p[0]);
            iv.put("quantity", Double.parseDouble(p[1]));
            iv.put("unit", p[2]);
            db.insert("recipe_ingredients", null, iv);
        }
    }

    private void seedRecipes(SQLiteDatabase db) {
        addRecipe(db, "Scrambled Eggs",
                "1. Whisk the eggs with the salt.\n2. Melt the butter in a pan on medium heat.\n3. Pour in the eggs and stir gently until just set.",
                "eggs|3|pcs;butter|10|g;salt|1|tsp");
        addRecipe(db, "Tomato Pasta",
                "1. Boil the pasta until tender and drain.\n2. Fry the chopped garlic in olive oil for 1 minute.\n3. Add chopped tomatoes and salt; simmer 10 minutes.\n4. Toss with the pasta.",
                "pasta|200|g;tomatoes|4|pcs;garlic|2|pcs;olive oil|2|tbsp;salt|1|tsp");
        addRecipe(db, "Cheese Omelette",
                "1. Whisk the eggs.\n2. Melt butter in a pan and pour in the eggs.\n3. When almost set, add grated cheese and fold in half.",
                "eggs|3|pcs;cheese|50|g;butter|10|g");
        addRecipe(db, "Pancakes",
                "1. Mix flour, sugar, eggs and milk into a smooth batter.\n2. Melt a little butter in a pan.\n3. Fry ladlefuls of batter 2 minutes per side.",
                "flour|200|g;milk|300|ml;eggs|2|pcs;sugar|2|tbsp;butter|20|g");
        addRecipe(db, "Egg Fried Rice",
                "1. Fry the chopped onion in oil.\n2. Add the cooked rice and stir-fry 3 minutes.\n3. Push aside, scramble the eggs, then mix together.\n4. Season with soy sauce.",
                "rice|200|g;eggs|2|pcs;onion|1|pcs;soy sauce|2|tbsp;oil|2|tbsp");
        addRecipe(db, "Cheese Toast",
                "1. Butter the bread.\n2. Top with sliced cheese.\n3. Grill or toast until the cheese bubbles.",
                "bread|2|pcs;cheese|40|g;butter|10|g");
        addRecipe(db, "Mashed Potatoes",
                "1. Peel, chop and boil the potatoes until soft.\n2. Drain and mash with butter, warm milk and salt.",
                "potatoes|500|g;butter|30|g;milk|100|ml;salt|1|tsp");
        addRecipe(db, "Garlic Bread",
                "1. Mix softened butter with crushed garlic.\n2. Spread on the bread.\n3. Bake at 200C for 8-10 minutes.",
                "bread|4|pcs;butter|40|g;garlic|2|pcs");
        addRecipe(db, "Chicken Stir Fry",
                "1. Slice the chicken, onion and carrots.\n2. Fry the chicken in oil until cooked through.\n3. Add the vegetables and stir-fry 5 minutes.\n4. Finish with soy sauce.",
                "chicken|300|g;onion|1|pcs;carrots|2|pcs;soy sauce|2|tbsp;oil|1|tbsp");
        addRecipe(db, "Vegetable Soup",
                "1. Chop the carrots, potatoes and onion.\n2. Simmer with the stock cube in 1 litre of water for 25 minutes.\n3. Season with salt.",
                "carrots|2|pcs;potatoes|2|pcs;onion|1|pcs;stock cube|1|pcs;salt|1|tsp");
        addRecipe(db, "Banana Pancakes",
                "1. Mash the bananas.\n2. Stir in eggs and flour.\n3. Fry small spoonfuls in a hot pan until golden.",
                "bananas|2|pcs;eggs|2|pcs;flour|100|g");
        addRecipe(db, "Tuna Mayo Sandwich",
                "1. Drain the tuna and mix with mayonnaise.\n2. Spread on a slice of bread and top with the second slice.",
                "bread|2|pcs;tuna|1|pcs;mayonnaise|2|tbsp");
        addRecipe(db, "Beans on Toast",
                "1. Heat the baked beans in a pot.\n2. Toast and butter the bread.\n3. Pour the beans over the toast.",
                "baked beans|200|g;bread|2|pcs;butter|10|g");
        addRecipe(db, "Egg Fried Noodles",
                "1. Cook the noodles and drain.\n2. Scramble the eggs in hot oil.\n3. Add the noodles and soy sauce and toss for 2 minutes.",
                "noodles|200|g;eggs|2|pcs;soy sauce|2|tbsp;oil|1|tbsp");
        addRecipe(db, "Rice and Beans",
                "1. Fry the chopped onion in oil.\n2. Add the beans and cooked rice.\n3. Heat through and season with salt.",
                "rice|200|g;beans|200|g;onion|1|pcs;oil|1|tbsp;salt|1|tsp");
        addRecipe(db, "Tomato Soup",
                "1. Fry onion and garlic in olive oil.\n2. Add chopped tomatoes and the stock cube with 500 ml water.\n3. Simmer 20 minutes, then blend.",
                "tomatoes|6|pcs;onion|1|pcs;garlic|2|pcs;stock cube|1|pcs;olive oil|1|tbsp");
        addRecipe(db, "French Toast",
                "1. Whisk eggs, milk and sugar.\n2. Dip the bread slices in the mixture.\n3. Fry in butter until golden on both sides.",
                "bread|4|pcs;eggs|2|pcs;milk|100|ml;sugar|1|tbsp;butter|10|g");
        addRecipe(db, "Potato Omelette",
                "1. Slice the potatoes and onion and fry slowly in oil until soft.\n2. Mix with beaten eggs.\n3. Cook in the pan until set, flipping once.",
                "potatoes|300|g;eggs|4|pcs;onion|1|pcs;oil|3|tbsp");
    }
}
