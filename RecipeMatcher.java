package com.example.smartpantry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * THE CORE LOGIC (assignment section 2.3): strict matching.
 *
 * A recipe is suggested ONLY if EVERY ingredient it needs is in the pantry in at least the
 * required quantity. 4 out of 5 ingredients = NOT suggested.
 *
 * Robustness to real-world messiness:
 *  - names are normalised (case, whitespace, simple plurals: "Tomatoes" == "tomato")
 *  - units are converted to a base unit (1 kg == 1000 g, 1 tbsp == 15 ml, ...)
 *  - duplicate pantry entries of the same ingredient are summed
 *  - incompatible dimensions (e.g. pantry has grams but recipe wants pieces) do NOT match
 */
public class RecipeMatcher {

    /** Lower-case, trim and singularise a name so "Tomatoes", "tomato " and "TOMATO" are equal. */
    public static String normaliseName(String raw) {
        if (raw == null) return "";
        String n = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (n.length() > 3) {
            if (n.endsWith("ies")) {
                n = n.substring(0, n.length() - 3) + "y";          // berries -> berry
            } else if (n.endsWith("oes")) {
                n = n.substring(0, n.length() - 2);                // tomatoes -> tomato
            } else if (n.endsWith("s") && !n.endsWith("ss") && !n.endsWith("us")) {
                n = n.substring(0, n.length() - 1);                // eggs -> egg
            }
        }
        return n;
    }

    /** Which kind of measurement a unit is: "mass", "volume" or "count". */
    static String dimension(String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        switch (u) {
            case "g": case "kg": case "mg":
                return "mass";
            case "ml": case "l": case "tsp": case "tbsp": case "cup":
                return "volume";
            default:
                return "count";
        }
    }

    /** Convert a quantity to the base unit of its dimension (g, ml or pieces). */
    static double toBase(double qty, String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        switch (u) {
            case "kg":   return qty * 1000;
            case "mg":   return qty / 1000;
            case "l":    return qty * 1000;
            case "tsp":  return qty * 5;
            case "tbsp": return qty * 15;
            case "cup":  return qty * 240;
            default:     return qty; // g, ml, pcs
        }
    }

    private static String key(String name, String unit) {
        return normaliseName(name) + "|" + dimension(unit);
    }

    /** Returns only the recipes the user can make RIGHT NOW from the pantry. */
    public static List<Recipe> findStrictMatches(List<Ingredient> pantry, List<Recipe> recipes) {
        // 1. Total up the pantry: key = normalised name + dimension, value = quantity in base unit.
        Map<String, Double> totals = new HashMap<>();
        for (Ingredient p : pantry) {
            String k = key(p.name, p.unit);
            Double current = totals.get(k);
            totals.put(k, (current == null ? 0 : current) + toBase(p.quantity, p.unit));
        }

        // 2. A recipe qualifies only if ALL of its ingredients are covered.
        List<Recipe> matches = new ArrayList<>();
        for (Recipe r : recipes) {
            boolean canMake = !r.ingredients.isEmpty();
            for (RecipeIngredient ri : r.ingredients) {
                Double have = totals.get(key(ri.name, ri.unit));
                double need = toBase(ri.quantity, ri.unit);
                if (have == null || have + 1e-9 < need) {
                    canMake = false;   // missing or not enough -> reject the whole recipe
                    break;
                }
            }
            if (canMake) matches.add(r);
        }
        return matches;
    }
}
