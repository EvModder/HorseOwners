package net.evmodder.HorseOwners.listeners;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import net.evmodder.EvLib.FileIO;
import net.evmodder.EvLib.util.Pair;
import net.evmodder.HorseOwners.HorseLibrary;
import net.evmodder.HorseOwners.HorseManager;

public class BreedListener implements Listener{
	private HorseManager plugin;
	enum ParentType{ MOTHER, FATHER, RANDOM };
	final boolean saveLineage, nameAtBirth, tameAtBirth, claimAtBirth, tweakStatsAtBirth, inbredMutation;
	final int INBRED_MUT_DIST;
	ParentType ownerAtBirth;
	final double MAX_JUMP, MAX_SPEED, MAX_HEALTH;
	final String[] horseNameList;
	final Random rand;

	public BreedListener(){
		plugin = HorseManager.getPlugin();
		saveLineage = plugin.getConfig().getBoolean("save-horse-lineage", true);
		nameAtBirth = plugin.getConfig().getBoolean("name-at-birth", false);
		tameAtBirth = plugin.getConfig().getBoolean("tame-at-birth", false);
		claimAtBirth = plugin.getConfig().getBoolean("claim-at-birth", true);
		tweakStatsAtBirth = plugin.getConfig().getBoolean("extra-random-factor-at-birth", false);
		inbredMutation = plugin.getConfig().getBoolean("inbred-mutations", true);
		INBRED_MUT_DIST = plugin.getConfig().getInt("inbred-mutation-distance", 2);
		try{ownerAtBirth = ParentType.valueOf(plugin.getConfig()
				.getString("owner-at-birth-is-owner-of", "RANDOM").toUpperCase());}
		catch(IllegalArgumentException ex){ownerAtBirth = ParentType.RANDOM;}
		horseNameList = FileIO.loadResource(plugin, "horse-names.txt").split("\n");

		MAX_JUMP = HorseLibrary.denormalizeJump(plugin.getConfig().getDouble("max-jump", 5.29));
		MAX_SPEED = HorseLibrary.denormalizeSpeed(plugin.getConfig().getDouble("max-speed", 14.5125));
		MAX_HEALTH = plugin.getConfig().getDouble("max-health", 30);
		rand = new Random();
	}

	String getRandomName(){
		for(int i=0; i<20; ++i){
			String name = horseNameList[rand.nextInt(horseNameList.length)];
			if(!plugin.horseExists(name)) return name;
		}
		return UUID.randomUUID().toString();
	}

	int getBloodlineDistance(Entity horse1, Entity horse2){
		if(horse1.equals(horse2)) return 0;
		if(plugin.getHorseParents(horse1) == null || plugin.getHorseParents(horse2) == null)
			return Integer.MAX_VALUE;
		ArrayDeque<Pair<Integer, String>> horse1Ancestors = new ArrayDeque<Pair<Integer, String>>();
		horse1Ancestors.add(new Pair<Integer, String>(0, horse1.getCustomName()));
		HashMap<String, Integer> distToHorse1 = new HashMap<String, Integer>();
		while(!horse1Ancestors.isEmpty()){
			Pair<Integer, String> ancestor = horse1Ancestors.remove();
			distToHorse1.put(HorseLibrary.cleanName(ancestor.b), ancestor.a);
			for(String parent : plugin.getHorseParents(ancestor.b)){
				horse1Ancestors.add(new Pair<Integer, String>(ancestor.a + 1, parent));
			}
		}
		PriorityQueue<Pair<Integer, String>> horse2Ancestors = new PriorityQueue<Pair<Integer, String>>();
		horse2Ancestors.add(new Pair<Integer, String>(0, horse2.getCustomName()));
		int minDist = Integer.MAX_VALUE;
		while(!horse2Ancestors.isEmpty()){
			Pair<Integer, String> ancestor = horse2Ancestors.remove();
			if(ancestor.a > minDist) break;
			Integer dist = distToHorse1.get(ancestor.b);
			if(dist != null && dist < minDist) minDist =  Math.max(dist, ancestor.a);
			for(String parent : plugin.getHorseParents(ancestor.b)){
				horse2Ancestors.add(new Pair<Integer, String>(ancestor.a + 1, parent));
			}
		}
		return minDist;
	}

	UUID getOwnerFromParents(Entity mother, Entity father, ParentType preferredParent){
		UUID mothersOwner = mother.getCustomName() == null ? null : plugin.getHorseOwner(mother.getCustomName());
		UUID fathersOwner = father.getCustomName() == null ? null : plugin.getHorseOwner(father.getCustomName());
		switch(ownerAtBirth){
			case MOTHER:
				return mothersOwner == null ? fathersOwner : mothersOwner;
			case FATHER:
				return fathersOwner == null ? mothersOwner : fathersOwner;
			case RANDOM:
			default:
				if(mothersOwner == null) return fathersOwner;
				if(fathersOwner == null) return mothersOwner;
				return rand.nextBoolean() ? mothersOwner : fathersOwner;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)// In case event is cancelled
	public void onBreed(EntityBreedEvent evt){
		if(plugin.isClaimableHorseType(evt.getEntity()) && evt.getEntity() instanceof AbstractHorse){
			AbstractHorse h = (AbstractHorse)evt.getEntity();
			plugin.getLogger().info("Breed event");

			//jump
			if(h.getJumpStrength() > MAX_JUMP) h.setJumpStrength(MAX_JUMP);

			//speed
			double speed = h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
			if(tweakStatsAtBirth)//remove up to 50% of the speed or add up to 30% of the speed
				speed *= ((rand.nextInt(81)+50)/100.0);
			if(speed > MAX_SPEED) speed = MAX_SPEED;
			h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);

			//health
			double health = h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if(tweakStatsAtBirth)//remove up to 50% of the health or add up to 20% of the health
				health *= ((rand.nextInt(71)+50)/100.0);
			if(health > MAX_HEALTH) health = MAX_HEALTH;
			HorseLibrary.setMaxHealth(h, health);

			UUID owner = getOwnerFromParents(evt.getMother(), evt.getFather(), ownerAtBirth);
			if(nameAtBirth){
				h.setCustomName(getRandomName());
				if(claimAtBirth && owner != null) plugin.addHorse(owner, h);
			}
			if(tameAtBirth){
				h.setTamed(true);
				if(owner != null){
					OfflinePlayer player = plugin.getServer().getOfflinePlayer(owner);
					if(player != null) h.setOwner(player);
				}
			}
			if(saveLineage){
				//Save mother and father in meta, even if names are null
				String mother = evt.getMother().getCustomName();
				String father = evt.getFather().getCustomName();

				if(mother != null) HorseLibrary.setMother(h, mother);
				if(father != null) HorseLibrary.setFather(h, father);
				plugin.updateData(h);
			}
			if(inbredMutation && getBloodlineDistance(evt.getMother(), evt.getFather()) <= INBRED_MUT_DIST){
				//remove up to 20% of the jump or add up to 5% of the jump
				double jump = h.getJumpStrength() * ((rand.nextInt(26)+80)/100D);
				if(jump > MAX_JUMP) jump = MAX_JUMP;
				h.setJumpStrength(jump);

				//remove up to 20% of the speed or add up to 5% of the speed
				speed = h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
				speed *= ((rand.nextInt(26)+80)/100D);
				if(speed > MAX_SPEED) speed = MAX_SPEED;
				h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);

				//remove up to 50% of the health or add up to 10% of the health
				health = h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				health *= ((rand.nextInt(61)+50)/100D);
				if(health > MAX_HEALTH) health = MAX_HEALTH;
				HorseLibrary.setMaxHealth(h, health);
			}
			plugin.getLogger().info("Final jump, speed, health: "+h.getJumpStrength()+", "+speed+", "+health);
		}
	}
}