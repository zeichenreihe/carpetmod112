package carpet.helpers;

import carpet.carpetclient.CarpetClientServer;
import carpet.mixin.accessors.RecipeManagerAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import net.minecraft.crafting.CraftingManager;
import net.minecraft.crafting.recipe.Recipe;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.resource.Identifier;
import net.minecraft.util.JsonUtils;

public class CustomCrafting {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String CARPET_DIRECTORY_RECIPES = "carpet/recipes";
	private static final ArrayList<Pair<String, JsonObject>> recipeList = new ArrayList<>();
	private static final HashSet<Recipe> recipes = new HashSet<>();

	public static boolean registerCustomRecipes(boolean result) throws IOException {
		if (!result) {
			return false;
		}

		Gson gson = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
		Path path = Paths.get(CARPET_DIRECTORY_RECIPES);
		if (!Files.isDirectory(path)) {
			Files.createDirectories(path);
		}

		Iterator<Path> iterator = Files.walk(path).iterator();

		while (iterator.hasNext()) {
			Path path1 = iterator.next();

			if ("json".equals(FilenameUtils.getExtension(path1.toString()))) {
				Path path2 = path.relativize(path1);
				String s = FilenameUtils.removeExtension(path2.toString()).replaceAll("\\\\", "/");
				Identifier resourcelocation = new Identifier(s);
				BufferedReader bufferedreader = null;

				try {
					try {
						bufferedreader = Files.newBufferedReader(path1);
						JsonObject json = JsonUtils.fromJson(gson, bufferedreader, JsonObject.class);
						recipeList.add(Pair.of(s, json));
						Recipe ir = RecipeManagerAccessor.invokeParseRecipeJson(json);
						recipes.add(ir);
						CraftingManager.register(s, ir);
					} catch (JsonParseException jsonparseexception) {
                        LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
						return false;
					} catch (IOException ioexception) {
                        LOGGER.error("Couldn't read recipe {} from {}", resourcelocation, path1, ioexception);
						return false;
					}
				} finally {
					IOUtils.closeQuietly(bufferedreader);
				}
			}
		}

		return true;
	}

	public static ArrayList<Pair<String, JsonObject>> getRecipeList() {
		return recipeList;
	}

	public static boolean filterCustomRecipesForOnlyCarpetClientUsers(Recipe recipe, ServerPlayerEntity player) {
		return !recipes.contains(recipe) || CarpetClientServer.isPlayerRegistered(player);
	}
}

