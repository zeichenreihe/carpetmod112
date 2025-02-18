/*
 * 
Copyright (c) <2018> <Xcom>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
  
 * Code made for carpet client by Xcom
 * 
 * This is an experimental version of the nitwit villager AI.
 * The aim is to make them craft items if they have the items in there inventory.
 * 
 * Villagers are supposed to have 3 different tiers of crafting recipes. The 2 higher
 * tiers are unlockable by having the nitwit craft the first tier or the 2nd when it
 * unlocks. They pickup items based on the crafting they are doing and they will swap
 * crafting jobs if they have nothing to do for a while. They will also consume 
 * food to craft and randomly have a preferred food type they craft faster with and
 * a dislike food they craft slower with. The longer they craft the faster they become.
 * 
 */

package carpet.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import carpet.mixin.accessors.VillagerEntityAccessor;
import carpet.mixin.accessors.IngredientAccessor;
import com.google.common.collect.Lists;
import carpet.utils.Messenger;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.crafting.CraftingManager;
import net.minecraft.crafting.recipe.Recipe;
import net.minecraft.crafting.recipe.Ingredient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.living.mob.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.Identifier;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityAICrafter extends Goal {
	private final VillagerEntity villager;
	private int currentTask;
	private int craftingCooldown;

	private final int[] tasks = new int[3];
	private static final int tier2Unlock = 1200 * 8;
	private static final int tier3Unlock = 1200 * 64;
	private final Recipe[] taskList = new Recipe[3];

	private static final int foodSlot = 7;
	private final int[] food = new int[3];
	private int foodSize;
	private float foodSpeed;
	private static final Item[] foods = {Items.BREAD, Items.POTATO, Items.CARROT, Items.BEETROOT};

	private final Random randy = new Random();
	private int cooldown;
	private int batchSize;
	private int foodCooldown;

	private int idleTimer;
	private boolean craftingCanHappen; // optimization for not having to check
	// full
	// crafting
	private boolean statsDone = false;

	private int researchCraftingTable;
	private BlockPos craftingTablePosition;
	private String villagerName;

	private boolean inishilized = false;

	private static final String[] tier1 = {"blaze_powder", // 401
			"bucket", // 369
			"fire_charge", // 303
			"glowstone", // 295
			"gold_nugget", // 291
			"hay_block", // 258
			"lever", // 220
			"lit_pumpkin", // 183
			"nether_brick", // 158
			"prismarine", // 118
			"red_sandstone", // 83
			"redstone_torch", // 73
			"sandstone", // 71
			"snow", // 60
			"stick", // 49
			"sticky_piston", // 48
			"stonebrick", // 36
			"string_to_wool", // 35
			"sugar", // 34
			"spruce_planks", // 52
			"oak_planks", // 152
			"jungle_planks", // 232
			"dark_oak_planks", // 334
			"birch_planks", // 413
			"acacia_planks", // 427
			"trapped_chest", // 29
			"iron_trapdoor", // 239
			"brick_block", // 381
			"magma_cream", // 167
			"purpur_block", // 103
			"end_bricks", // 311
			"coarse_dirt", // 355
			"magma", // 168
			"mossy_cobblestone", // 162
	};

	private static final String[] tier2 = {"dark_prismarine", // 331
			"fence", // 306
			"fence_gate", // 305
			"furnace", // 299
			"gold_block", // 294
			"ladder", // 229
			"lapis_block", // 228
			"cobblestone_wall", // 353
			"melon_block", // 165
			"cobblestone_slab", // 354
			"minecart", // 163
			"diamond_block", // 326
			"iron_block", // 252
			"boat", // 391
			"bone_block", // 390
			"coal_block", // 356
			"cauldron", // 366
			"paper", // 136
			"quartz_slab", // 98
			"quartz_stairs", // 97
			"redstone_block", // 75
			"redstone_lamp", // 74
			"sandstone_slab", // 70
			"rail", // 94
			"sandstone_stairs", // 69
			"trapdoor", // 30
			"tripwire_hook", // 28
			"slime", // 64
			"sea_lantern", // 68
			"nether_wart_block", // 154
			"iron_ingot_from_block", // 246
			"redstone", // 76
			"prismarine_bricks", // 117
			"armor_stand", // 421
			"book", // 387
			"emerald_block", // 313
			"spectral_arrow", // 57
			"wooden_door", // 17
			"iron_door", // 249
	};

	private static final String[] tier3 = {"beacon", // 419
			"brewing_stand", // 382
			"chest", // 365
			"detector_rail", // 329
			"dispenser", // 316
			"dropper", // 315
			"anvil", // 422
			"ender_chest", // 308
			"activator_rail", // 424
			"fermented_spider_eye", // 304
			"tnt_minecart", // 32
			"noteblock", // 153
			"item_frame", // 238
			"daylight_detector", // 330
			"bow", // 385
			"comparator", // 352
			"golden_apple", // 290
			"golden_carrot", // 287
			"golden_rail", // 281
			"hopper", // 256
			"observer", // 149
			"sign", // 65
			"piston", // 122
			"speckled_melon", // 58
			"tnt", // 33
			"repeater", // 72
			"end_crystal", // 310
			"purple_shulker_box", // 108
			"end_rod", // 309
			"painting", // 137
	};

	/**
	 * Basic constructor for the crafting AI task
	 *
	 * @param theVillagerIn the villager object.
	 */
	public EntityAICrafter(VillagerEntity theVillagerIn) {
		this.villager = theVillagerIn;
	}

	/**
	 * Global update to set there stats.
	 */
	public void updateNitwit() {
		updateCareerID();
		setupFoodSpeed();
		calcCooldown();
		resetIdleTimer();
		setName();
		inishilized = true;
	}

	/**
	 * Used to update the crafting jobs and other settings of the crafter based on the saved data.
	 */
	private void updateCareerID() {
		if (((VillagerEntityAccessor) villager).getCareer() == 0) {
			randomiseStats();
		} else {
			decodeVillager();
		}
		fixFoodInventory();
		statsDone = true;
	}

	/**
	 * Fixes the food that is stuck in the wrong slots of the villager.
	 */
	private void fixFoodInventory() {
		SimpleInventory villagerInventory = villager.getVillagerInventory();
		ItemStack foodStack = villagerInventory.getStack(foodSlot);

		boolean dropWrongFoods = isFood(foodStack.getItem());

		for (int i = 0; i < villagerInventory.getSize() - 1; ++i) {
			ItemStack inventoryItem = villagerInventory.getStack(i);
			if (isFood(inventoryItem.getItem())) {
				if (dropWrongFoods) {
					dropItem(inventoryItem);
					villagerInventory.markDirty();
				} else {
					villagerInventory.setStack(foodSlot, inventoryItem.copy());
					inventoryItem.setSize(0);
					villagerInventory.markDirty();
					dropWrongFoods = true;
				}
			}
		}
	}

	/**
	 * Randomizes the stats of a newly created crafting villager.
	 */
	private void randomiseStats() {
		tasks[0] = randy.nextInt(tier1.length);
		tasks[1] = randy.nextInt(tier2.length);
		tasks[2] = randy.nextInt(tier3.length);

		food[0] = randy.nextInt(4);
		food[1] = randy.nextInt(4);
		while (food[0] == food[1]) {
			food[1] = randy.nextInt(4);
		}
		food[2] = 4 + randy.nextInt(4);

		foodSize = food[2];
		taskList[0] = getRecipe(tier1[tasks[0]]);
		taskList[1] = getRecipe(tier2[tasks[1]]);
		taskList[2] = getRecipe(tier3[tasks[2]]);
	}

	/**
	 * Decodes the stats from saved data and sets the stats of the villager.
	 */
	private void decodeVillager() {
		int taskEncoder = ((VillagerEntityAccessor) villager).getCareer();
		int foodEncoder = ((VillagerEntityAccessor) villager).getCareerLevel();

		for (int i = 0; i < 3; i++) {
			tasks[i] = taskEncoder % 100;
			taskEncoder = taskEncoder / 100;
		}
		food[2] = taskEncoder % 100;
		taskEncoder = taskEncoder / 100;
		currentTask = taskEncoder % 10;

		for (int i = 0; i < 2; i++) {
			food[i] = (foodEncoder % 10) % 4;
			foodEncoder = foodEncoder / 10;
		}

		foodSize = food[2];
		try {
			taskList[0] = getRecipe(tier1[tasks[0]]);
			taskList[1] = getRecipe(tier2[tasks[1]]);
			taskList[2] = getRecipe(tier3[tasks[2]]);
		} catch (Exception e) {
			Messenger.print_server_message(villager.getServer(), "A villager with nasty craftings was found and stats was rerolled.");
			randomiseStats();
		}
	}

	/**
	 * Encodes the data for saving the villagers stats on disk.
	 */
	private void encodeVillager() {
		int taskEncoder = tasks[0] + tasks[1] * 100 + tasks[2] * 10000 + food[2] * 1000000 + currentTask * 100000000;
		int foodEncoder = food[0] + food[1] * 10;

		((VillagerEntityAccessor) villager).setCareer(taskEncoder);
		((VillagerEntityAccessor) villager).setCareerLevel(foodEncoder);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	@Override
	public boolean canStart() {
		return true;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	@Override
	public boolean shouldContinue() {
		return this.currentTask >= 0 && super.shouldContinue();
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	@Override
	public void start() {
	}

	/**
	 * Updates the task
	 */
	@Override
	public void tick() {
		lookAtCraftingTables();

		if (craftingCooldown <= 0) {
			if (!craftingCanHappen) {
				if (idleTimer > 0) {
					idleTimer--;
				}
				return;
			}

			if (craftItems() && eatFood()) {
				calcCooldown();
				setName();
				craftingCooldown = cooldown;
				resetIdleTimer();
				encodeVillager();
			} else {
				craftingCanHappen = false;
			}
		} else {
			if (eatFood()) {
				VillagerEntityAccessor acc = (VillagerEntityAccessor) villager;
				if (acc.getRiches() < 12000000) {
					acc.setRiches(acc.getRiches() + 1);
				}
				craftingCooldown--;
				encodeVillager();
			}
		}
	}

	/**
	 * Makes the villager turn towards crafting tables and updates the villagers crafting table if it exists.
	 */
	private void lookAtCraftingTables() {
		if (craftingTablePosition != null && researchCraftingTable > 0) {
			researchCraftingTable--;
			villager.getLookControl().lookAt((double) craftingTablePosition.getX() + 0.5D,
					craftingTablePosition.getY() + 1.5,
					(double) craftingTablePosition.getZ() + 0.5D,
					10.0F,
					(float) villager.getLookPitchSpeed()
			);
		} else if (researchCraftingTable <= 0) {
			researchCraftingTable = 100;
			findCraftingTableNear();
		} else {
			researchCraftingTable--;
		}
	}

	/**
	 * Finds a crafting table in an area around the villager.
	 */
	private void findCraftingTableNear() {
		World worldIn = villager.getSourceWorld();
		BlockPos villagerpos = new BlockPos(villager);
		for (BlockPos pos : BlockPos.iterateRegion(villagerpos.add(-3, -1, -3), villagerpos.add(3, 4, 3))) {
			if (worldIn.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE) {
				craftingTablePosition = pos;
				craftingCanHappen = true;
				return;
			}
		}
		craftingTablePosition = null;
	}

	/**
	 * Sets the name of the villager for displaying what level they are at.
	 */
	private void setName() {
		String s = null;
		VillagerEntityAccessor acc = (VillagerEntityAccessor) villager;
		if (acc.getRiches() == 12000000) {
			s = "Grandmaster";
		} else if (acc.getRiches() > 8000000) {
			s = "Meister";
		} else if (acc.getRiches() > 4000000) {
			s = "Craftsman";
		} else if (acc.getRiches() > 2000000) {
			s = "Journeyman";
		} else if (acc.getRiches() > 1000000) {
			s = "Apprentice";
		} else if (acc.getRiches() > 500000) {
			s = "Novice";
		} else if (acc.getRiches() > tier3Unlock) {
			s = "Casual";
		} else if (acc.getRiches() > tier2Unlock) {
			s = "Nooblet";
		} else if (acc.getRiches() > 1000) {
			s = "Nitwit";
		}

		if (villagerName == null) villagerName = s;

		if (s != null && !villagerName.equals(s)) {
			villager.setCustomName(s);
		}
	}

	/**
	 * Resets the idle timer that causes the villager to swap foods or jobs.
	 */
	private void resetIdleTimer() {
		idleTimer = 200 + randy.nextInt(100);
	}

	/**
	 * Sets the food speed based of the current food in the inventory on the food preference of the villager.
	 */
	private void setupFoodSpeed() {
		SimpleInventory villagerInventory = villager.getVillagerInventory();
		ItemStack foodStack = getFoodStack(villagerInventory);
		setFoodSpeed(foodStack);
	}

	/**
	 * Recalculates the cooldowns and crafting amounts based on the experience of the villager.
	 */
	private void calcCooldown() {
		int welt = ((VillagerEntityAccessor) villager).getRiches();
		if (welt < 0) {
			welt = 1;
		}
		float foodSpeed = foodPreference();
		cooldown = (int) ((107 / Math.pow(10, 0.0000001692d * welt) + 19) * foodSpeed);
		batchSize = (welt / 1713000) + 1;
	}

	/**
	 * Returns the food preference speed.
	 *
	 * @return returns the speed multiplier based on the food.
	 */
	private float foodPreference() {
		return foodSpeed;
	}

	/**
	 * Returns the recipe that the villager is currently crafting.
	 *
	 * @return the recipe object that is being crafted currently.
	 */
	private Recipe currentTaskRecipe() {
		if (currentTask < 0 || currentTask >= taskList.length) {
			currentTask = 0;
		}
		return taskList[currentTask];
	}

	/**
	 * Main crafting logic that performs the crafting based on the job
	 *
	 * @return returns true if the crafting job is successful and false if not.
	 */
	private boolean craftItems() {
		if (!statsDone) {
			return false;
		}

		// tier 2 and above needs crafting table.
		if (craftingTablePosition == null && currentTask > 0) {
			return false;
		}

		SimpleInventory villagerInventory = villager.getVillagerInventory();
		ItemStack food = getFoodStack(villagerInventory);
		if (foodCooldown <= 0 && food.getSize() < foodSize) {
			return false;
		}

		boolean crafted = false;

		Map<ItemStack, Integer> map = genCraftingMap(currentTaskRecipe());

		for (int batch = 0; batch < batchSize; batch++) {
			Map<ItemStack, Integer> crafting = findRelativeInventoryItemsForCrafting(map, villagerInventory);

			if (crafting != null) {
				for (Map.Entry<ItemStack, Integer> entry : crafting.entrySet()) {
					entry.getKey().setSize(entry.getKey().getSize() - entry.getValue());
				}
				dropItem(currentTaskRecipe().apply(null));
				crafted = true;
			} else {
				break;
			}
		}

		if (crafted) {
			villagerInventory.markDirty();
		}

		return crafted;
	}

	/**
	 * Eats food if there is in the inventory and returns if the crafter is fed.
	 *
	 * @return Returns true if the villager is fed. False if lacks food.
	 */
	private boolean eatFood() {
		if (foodCooldown > 0) {
			foodCooldown--;
			return true;
		}
		SimpleInventory villagerInventory = villager.getVillagerInventory();
		ItemStack food = getFoodStack(villagerInventory);
		if (food.getSize() < foodSize) {
			return false;
		}

		food.setSize(food.getSize() - foodSize);
		foodCooldown = 160;
		return true;
	}

	/**
	 * The stack of food that is based on the last slot of the villager.
	 *
	 * @param villagerInventory The inventory object of the villager.
	 *
	 * @return Item stack of food based on the last slot.
	 */
	private static ItemStack getFoodStack(SimpleInventory villagerInventory) {
		return villagerInventory.getStack(foodSlot);
	}

	/**
	 * Drops all items the villager is crafting except the food that is in the last slot. This is done to switch jobs.
	 */
	private void dropJob() {
		SimpleInventory villagerInventory = villager.getVillagerInventory();
		for (int i = 0; i < villagerInventory.getSize() - 2; ++i) {
			ItemStack itemstack = villagerInventory.getStack(i);
			dropItem(itemstack.copy());
			itemstack.setSize(0);
		}
	}

	/**
	 * Find the relative inventory items of the currently selected job. A list of all inventory stacks and the amount that is needed to be deducted for the
	 * current job is returned.
	 *
	 * @param list              A list of the items needed for the currently selected job.
	 * @param villagerInventory Villager inventory object.
	 *
	 * @return List of item stacks and the amount needed to be deducted for the currently selected job. Return null if the inventory items arent sufficient for
	 * the recipe.
	 */
	private static @Nullable Map<ItemStack, Integer> findRelativeInventoryItemsForCrafting(Map<ItemStack, Integer> list, SimpleInventory villagerInventory) {
		Map<ItemStack, Integer> crafting = new HashMap<>();
		Map<ItemStack, Integer> map = new HashMap<>(list);

		for (int i = 0; i < villagerInventory.getSize() - 1; ++i) {
			for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
				ItemStack itemstack = villagerInventory.getStack(i);

				if (entry.getValue() > 0 && entry.getKey().getItem() == itemstack.getItem()) {
					int itemCount = map.get(entry.getKey());
					int invCount = itemstack.getSize();
					int reduce = Math.min(itemCount, invCount);
					int remains = itemCount - reduce;

					crafting.put(itemstack, reduce);

					map.put(entry.getKey(), remains);
				}
			}
		}

		for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
			if (entry.getValue() > 0) {
				return null;
			}
		}

		return crafting;
	}

	/**
	 * Creates a list of each item needed from crafting from the recipe object and the amount per item.
	 *
	 * @param recipe The recipe that is being used to create a list of items for.
	 *
	 * @return the list of items for the recipe and the amount per item.
	 */
	private Map<ItemStack, Integer> genCraftingMap(Recipe recipe) {
		Map<ItemStack, Integer> map = new HashMap<>();
		DefaultedList<Ingredient> list = recipe.getIngredients();

		for (Ingredient ig : list) {
			ItemStack[] stack = ((IngredientAccessor) ig).getStacks();
			if (stack.length > 0) {
				ItemStack is = stack[0];
				ItemStack is2 = itemIsInMap(map, is);
				if (is2 == null) {
					map.put(is, 1);
				} else {
					int i = map.get(is2);
					map.put(is2, i + 1);
				}
			}
		}

		return map;
	}

	/**
	 * Checks if the item stack type is in the list.
	 *
	 * @param map       The list that is being checked for if it contains the item stack type.
	 * @param itemstack The item stack type that is being checked for.
	 *
	 * @return Returns true if the item stack type is in the list.
	 */
	private @Nullable ItemStack itemIsInMap(Map<ItemStack, Integer> map, ItemStack itemstack) {
		for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
			if (entry.getKey().getItem() == itemstack.getItem()) return entry.getKey();
		}
		return null;
	}

	/**
	 * Gets the amount of items of a specific item type found in the current job (active recipe)
	 *
	 * @param item The item type that is being checked for in the active recipe.
	 *
	 * @return The amount of items found in the active recipe.
	 */
	private int getActiveRecipeCount(Item item) {
		int itemCount = 0;
		DefaultedList<Ingredient> list = currentTaskRecipe().getIngredients();
		for (Ingredient ig : list) {
			for (ItemStack is : ((IngredientAccessor) ig).getStacks()) {
				if (is.getItem() == item) {
					itemCount++;
				}
			}
		}
		return itemCount;
	}

	/**
	 * Drops the items out of the villagers head towards the specified facing direction or towards a crafting table if the villager has chosen it as the active
	 * crafting table.
	 *
	 * @param itemstack The item stack that is being thrown out of the villager.
	 */
	private void dropItem(ItemStack itemstack) {
		if (itemstack.isEmpty()) return;

		float f1 = villager.headYaw;
		float f2 = villager.pitch;

		if (craftingTablePosition != null) {
			double d0 = craftingTablePosition.getX() + 0.5D - villager.x;
			double d1 = craftingTablePosition.getY() + 1.5D - (villager.y + (double) villager.getEyeHeight());
			double d2 = craftingTablePosition.getZ() + 0.5D - villager.z;
			double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
			f1 = (float) (MathHelper.fastAtan2(d2, d0) * (180D / Math.PI)) - 90.0F;
			f2 = (float) (-(MathHelper.fastAtan2(d1, d3) * (180D / Math.PI)));
		}

		double d0 = villager.y - 0.30000001192092896D + villager.getEyeHeight();
		ItemEntity itemEntity = new ItemEntity(villager.world, villager.x, d0, villager.z, itemstack);
		float f = 0.3F;

		itemEntity.velocityX = -MathHelper.sin(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F) * f;
		itemEntity.velocityY = MathHelper.cos(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F) * f;
		itemEntity.velocityZ = -MathHelper.sin(f2 * 0.017453292F) * 0.3F + 0.1F;
		itemEntity.setDefaultPickUpDelay();
		villager.world.addEntity(itemEntity);
	}

	/**
	 * Updates the villager equipment based on the item type that is being picked up. If the item is a command block data is printed out, structure blocks
	 * delete all inventory items of the villager. If the villager can pickup the item the idle timer and the crafting is enabled. If the idle timer have kicked
	 * in the item is checked if it matches other recipes the villager have unlocked to switch jobs.
	 *
	 * @param itemEntity        Item stack that is being picked up by the villager.
	 * @param villagerInventory The villagers inventory object.
	 *
	 * @return Returns true if the villager should stop all other inventory actions.
	 */
	public boolean updateEquipment(ItemEntity itemEntity, SimpleInventory villagerInventory) {
		if (!inishilized) {
			return false;
		}
		ItemStack itemstack = itemEntity.getItemStack();
		Item item = itemstack.getItem();
		if (itemstack.getItem().getTranslationKey().equals(Blocks.COMMAND_BLOCK.getTranslationKey())) {
			readoutDebugInfoOnMe();
			itemEntity.remove();
			return true;
		} else if (itemstack.getItem().getTranslationKey().equals(Blocks.STRUCTURE_BLOCK.getTranslationKey())) {
			for (int i = 0; i < villagerInventory.getSize(); ++i) {
				villagerInventory.getStack(i).setSize(0);
			}
			itemEntity.remove();
			return true;
		}

		if (!statsDone) {
			return true;
		}

		boolean foodCheck = isFood(item);
		ItemStack editedStack = null;

		if (foodCheck) {
			editedStack = addFood(itemstack, villagerInventory);
			processItems(itemEntity, itemstack, editedStack);
		} else if (craftingItemForPickup(item, currentTaskRecipe())) {
			editedStack = addRecipeItem(itemstack, villagerInventory);
			processItems(itemEntity, itemstack, editedStack);
		}

		boolean itemPickedUp = false;
		if (editedStack != null && editedStack.getSize() != itemstack.getSize()) {
			craftingCanHappen = true;
			itemPickedUp = true;
		}

		// Dirty place to put job switcher but what the hell
		// If ground items didn't get picked up and the idle timer is zero,
		// check for job switch.
		if (idleTimer <= 0 && !foodCheck && !itemPickedUp) {
			craftingCanHappen = true;
			checkJobSwitch(item);
		}

		return true;
	}

	/**
	 * Checks if the item type matches in the recipe lists of the other jobs the villager have unlocked and switches job if match is found.
	 *
	 * @param item The item type that is being used to check if it matches the other jobs the villager have unlocked.
	 */
	private void checkJobSwitch(Item item) {
		int unlocked = getUnlockedLevel();

		if (unlocked > 1) {
			int newJob = randy.nextInt() % unlocked;
			while (currentTask == newJob || newJob < 0) {
				newJob = randy.nextInt() % unlocked;
			}
			if (craftingItemForPickup(item, taskList[newJob])) {
				switchJobTo(newJob);
			}
			idleTimer = 10;
		}
	}

	/**
	 * Switches the job and performs the dropping of items and resets the idle timer.
	 *
	 * @param job The new job that is being switched too.
	 */
	private void switchJobTo(int job) {
		currentTask = job;
		dropJob();
		resetIdleTimer();
	}

	/**
	 * Gets the unlocked levels based on the crafting experience of the villager.
	 *
	 * @return Returns the level of what level the crafting villager have unlocked based on the crafting experience.
	 */
	private int getUnlockedLevel() {
		int wealth = ((VillagerEntityAccessor) villager).getRiches();
		if (wealth < tier2Unlock) {
			return 1;
		} else if (wealth < tier3Unlock) {
			return 2;
		}
		return 3;
	}

	/**
	 * Adds the food item into the villagers inventory. Placed in the last slot of the villager. If another food type is found it is swaped if the idle timer
	 * have kicked in (no crafting done in a period of 10-15 seconds).
	 *
	 * @param stack             Item stack that is being placed into the villagers inventory.
	 * @param villagerInventory The villagers inventory object.
	 *
	 * @return Returns the resulting change after attempting to place the item stack into the villagers inventory. Unchanged if the item stack can't be placed
	 * anywhere and returns empty if it was placed into an empty slot or fully on top of another stack.
	 */
	private ItemStack addFood(ItemStack stack, SimpleInventory villagerInventory) {
		ItemStack groundItem = stack.copy();
		ItemStack inventoryItem = villagerInventory.getStack(foodSlot);

		if (inventoryItem.isEmpty()) {
			villagerInventory.setStack(foodSlot, groundItem);
			setFoodSpeed(groundItem);
			calcCooldown();
			resetIdleTimer();
			villagerInventory.markDirty();
			return ItemStack.EMPTY;
		} else {
			if (ItemStack.matchesNbt(inventoryItem, groundItem)) {
				int j = Math.min(villagerInventory.getMaxStackSize(), inventoryItem.getMaxSize());
				int k = Math.min(groundItem.getSize(), j - inventoryItem.getSize());

				if (k > 0) {
					inventoryItem.increase(k);
					groundItem.decrease(k);

					if (groundItem.isEmpty()) {
						villagerInventory.markDirty();
						return ItemStack.EMPTY;
					}
				}
			} else if (idleTimer <= 0) {
				dropItem(inventoryItem);
				villagerInventory.setStack(foodSlot, groundItem);
				setFoodSpeed(groundItem);
				calcCooldown();
				resetIdleTimer();
				villagerInventory.markDirty();
				return ItemStack.EMPTY;
			}
		}

		if (groundItem.getSize() != stack.getSize()) {
			villagerInventory.markDirty();
		}

		return groundItem;
	}

	/**
	 * Sets the speed of the food based on the villagers food preference matched towards the item stack (food item stack).
	 *
	 * @param itemstack The item stack that is being used to check what food preference the villager has.
	 */
	private void setFoodSpeed(ItemStack itemstack) {
		Item item = itemstack.getItem();
		int index = 0;

		for (Item im : foods) {
			if (im != item) {
				index++;
			} else {
				break;
			}
		}

		if (index == food[0]) {
			foodSpeed = 0.8f;
		} else if (index == food[1]) {
			foodSpeed = 1.2f;
		} else {
			foodSpeed = 1.0f;
		}
	}

	/**
	 * Adds the item into the villagers inventory. If the item is found it stacks the item. If the item is not found it places it into the first slot. Only one
	 * stack per item is used.
	 * <p>
	 * An exception is done on wooden planks where the wooden planks are stacked and the meta data of the plank is changed to the latest plank type that was
	 * stacked. This is done because of the villages limited inventory size and the large number of wooden planks in the game.
	 *
	 * @param stack             Item stack that is being placed into the villagers inventory.
	 * @param villagerInventory The villagers inventory object.
	 *
	 * @return Returns the resulting change after attempting to place the item stack into the villagers inventory. Unchanged if the item stack can't be placed
	 * anywhere and returns empty if it was placed into an empty slot or fully on top of another stack.
	 */
	private ItemStack addRecipeItem(ItemStack stack, SimpleInventory villagerInventory) {
		ItemStack groundItem = stack.copy();
		ItemStack inventoryItem;
		int emptySlot = -1;
		boolean planks = plankCheck(groundItem);

		for (int i = 0; i < villagerInventory.getSize() - 1; ++i) {
			inventoryItem = villagerInventory.getStack(i);

			if (inventoryItem.isEmpty() && emptySlot == -1) {
				emptySlot = i;
				continue;
			}

			if (ItemStack.matchesNbt(inventoryItem, groundItem)) {
				int j = Math.min(villagerInventory.getMaxStackSize(), inventoryItem.getMaxSize());
				int k = Math.min(groundItem.getSize(), j - inventoryItem.getSize());

				if (k > 0) {
					inventoryItem.increase(k);
					groundItem.decrease(k);

					if (groundItem.isEmpty()) {
						resetIdleTimer();
						villagerInventory.markDirty();
						return ItemStack.EMPTY;
					}
				}
				break;
			} else if (planks && !inventoryItem.isEmpty() && plankCheck(inventoryItem)) { // plankmerge
				int j = Math.min(villagerInventory.getMaxStackSize(), inventoryItem.getMaxSize());
				int k = Math.min(groundItem.getSize(), j - inventoryItem.getSize());

				if (k > 0) {
					inventoryItem.setDamage(groundItem.getDamage());
					inventoryItem.increase(k);
					groundItem.decrease(k);

					if (groundItem.isEmpty()) {
						resetIdleTimer();
						villagerInventory.markDirty();
						return ItemStack.EMPTY;
					}
				}
				break;
			}
		}

		if (emptySlot != -1) {
			resetIdleTimer();
			villagerInventory.setStack(emptySlot, groundItem);
			villagerInventory.markDirty();
			return ItemStack.EMPTY;
		}

		if (groundItem.getSize() != stack.getSize()) {
			resetIdleTimer();
			villagerInventory.markDirty();
		}

		return groundItem;
	}

	/**
	 * Checks if the item stack is a wooden plank of any type.
	 *
	 * @param itemstack Item stack that is being checked if its a plank or not.
	 *
	 * @return Returns true if the item stack is of the type plank.
	 */
	private boolean plankCheck(ItemStack itemstack) {
		return itemstack.getItem().getTranslationKey().equals(Blocks.PLANKS.getTranslationKey());
	}

	/**
	 * Used to stack items and delete the older item.
	 *
	 * @param itemEntity      The item object that is being used to delete if needed.
	 * @param itemEntityStack The stacking item stack that is being updated.
	 * @param itemEdited      The item stack that is used to stack on top of the stacking item stack.
	 */
	private void processItems(ItemEntity itemEntity, ItemStack itemEntityStack, ItemStack itemEdited) {
		if (itemEdited.isEmpty()) {
			itemEntity.remove();
		} else {
			itemEntityStack.setSize(itemEdited.getSize());
		}
	}

	/**
	 * Checks if the specified Item type is in the recipe that is being checked towards.
	 *
	 * @param item    The item type that is being checked if its in the recipe.
	 * @param irecipe The recipe that is being checked if the item type is in.
	 *
	 * @return Returns true if the item type is in the recipe.
	 */
	private boolean craftingItemForPickup(Item item, Recipe irecipe) {
		DefaultedList<Ingredient> list = irecipe.getIngredients();
		for (Ingredient ig : list) {
			for (ItemStack is : ((IngredientAccessor) ig).getStacks()) {
				if (is.getItem() == item) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the specific Item is a food that the villager can consume or not.
	 *
	 * @param item The item that is being checked if its a villager preferred food or not.
	 *
	 * @return Returns true if its a food that can be consumed.
	 */
	private boolean isFood(Item item) {
		for (Item im : foods) {
			if (im == item) return true;
		}
		return false;
	}

	/**
	 * Total list of all crafting IRecpie's.
	 *
	 * @return List of all IRecpie that can be crafted.
	 */
	private static List<Recipe> recipeList() {
		return Lists.newArrayList(CraftingManager.REGISTRY);
	}

	/**
	 * IRecpie info from the crafting list found in vanilla code.
	 *
	 * @param recipe The string used to get the specific recipe.
	 *
	 * @return The specific IRecpie being request.
	 */
	private static Recipe getRecipe(String recipe) {
		return CraftingManager.getRecipe(new Identifier(recipe));
	}


	/**
	 * Drops the inventory of the killed villager except for blacklisted items that can have the chance to be used as id-converters.
	 */
	public void dropInventory() {
		SimpleInventory villagerInventory = villager.getVillagerInventory();
		for (int j = 0; j < villagerInventory.getSize(); ++j) {
			ItemStack is = villagerInventory.getStack(j);
			boolean planks = plankCheck(is);
			boolean die = is.getItem() == Items.DYE;
			if (!planks && !die) {
				villager.dropItem(is, 0.0F);
			}
		}
	}

	/**
	 * Server printout of all the information related to this specific crafting villager.
	 */
	private void readoutDebugInfoOnMe() {
		SimpleInventory villagerInventory = villager.getVillagerInventory();
		StringBuilder sb = new StringBuilder();
		try {
			calcCooldown();
			sb.append("Crafter info:\n");
			sb.append("Craftings:\n");
			for (int i = 0; i < taskList.length; ++i) {
				Recipe ir = taskList[i];
				sb.append("tier ").append(i + 1).append(": ").append(ir.getResult().getTranslationKey()).append("\n");
			}
			sb.append("Current Task: ").append(currentTaskRecipe().getResult().getTranslationKey()).append("\n");
			sb.append("Crafting Cooldown(ticks)/Batch Size: ").append(cooldown).append("/").append(batchSize).append("\n");
			sb.append("Food consumption per craft: ").append(food[2]).append("\n");
			sb.append("Food preference: ")
					.append(foods[food[0]].getName(new ItemStack(foods[food[0]])))
					.append("\nFood dislike: ")
					.append(foods[food[1]].getName(new ItemStack(foods[food[1]])))
					.append("\n");
			sb.append("Crafting experience: ").append(((VillagerEntityAccessor) villager).getRiches()).append("\n");

			sb.append("Inventory: \n");
			for (int j = 0; j < villagerInventory.getSize(); ++j) {
				sb.append("Slot: ")
						.append(j + 1)
						.append(": ")
						.append(villagerInventory.getStack(j).getTranslationKey())
						.append(" : ")
						.append(villagerInventory.getStack(j).getSize())
						.append("\n");
			}
		} catch (Exception ignored) {
		}

		Messenger.print_server_message(villager.getServer(), sb.toString());
	}
}
