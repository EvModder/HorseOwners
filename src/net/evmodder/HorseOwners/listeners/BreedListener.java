package net.evmodder.HorseOwners.listeners;

import java.util.Random;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import net.evmodder.EvLib.FileIO;
import net.evmodder.HorseOwners.HorseLibrary;
import net.evmodder.HorseOwners.HorseManager;

public class BreedListener implements Listener{
	private HorseManager plugin;
	enum ParentType{ MOTHER, FATHER, RANDOM };
	final boolean saveLineage, nameAtBirth, tameAtBirth, claimAtBirth, tweakStatsAtBirth, inbredMutation;
	//final int INBRED_MUT_DIST;
	final int INBRED_THRESHOLD;
	ParentType ownerAtBirth;
	final double MAX_JUMP, MAX_SPEED, MAX_HEALTH; // Not normalized
	final double MIN_JUMP = 0.4D, MIN_SPEED = 0.1125D, MIN_HEALTH = 15;
	final boolean PRINT_DEBUG = true;

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
		//INBRED_MUT_DIST = plugin.getConfig().getInt("inbred-mutation-distance", 2);
		INBRED_THRESHOLD = plugin.getConfig().getInt("inbred-threshold-percent", 7);
		try{ownerAtBirth = ParentType.valueOf(plugin.getConfig()
				.getString("owner-at-birth-is-owner-of", "RANDOM").toUpperCase());}
		catch(IllegalArgumentException ex){ownerAtBirth = ParentType.RANDOM;}
		horseNameList = FileIO.loadResource(plugin, "horse-names.txt").split("\n");

		double normalJump = plugin.getConfig().getDouble("max-jump", 5.29);
		double normalSpeed = plugin.getConfig().getDouble("max-speed", 14.5125);
		double normalHealth = plugin.getConfig().getDouble("max-health", 30);
		MAX_JUMP = normalJump > 0 ? HorseLibrary.denormalizeJump(normalJump) : -1;
		MAX_SPEED = normalSpeed > 0 ? HorseLibrary.denormalizeSpeed(normalSpeed) : -1;
		MAX_HEALTH = normalHealth > 0 ? normalHealth : -1;
		rand = new Random();
	}

	String getRandomName(){
		for(int i=0; i<20; ++i){
			String name = horseNameList[rand.nextInt(horseNameList.length)];
			if(!plugin.horseExists(name)) return name;
		}
		return UUID.randomUUID().toString();
	}

	double uniformRandom(double a, double b){
		double scale = b - a;
		return rand.nextDouble()*scale + a;
	}

	private String getOffspringDNA(Entity parent1, Entity parent2){
		char[] dna1 = HorseLibrary.getDNA(parent1, rand).toCharArray();
		char[] dna2 = HorseLibrary.getDNA(parent2, rand).toCharArray();
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<100; ++i) builder.append(rand.nextBoolean() ? dna1[i] : dna2[i]);
		return builder.toString();
	}
	int getGeneticOverlap(Entity horse1, Entity horse2){
		char[] dna1 = HorseLibrary.getDNA(horse1, rand).toCharArray();
		char[] dna2 = HorseLibrary.getDNA(horse2, rand).toCharArray();
		int overlap = 0;
		for(int i=0; i<100; ++i) if(dna1[i] == dna2[i]) ++overlap;
		return overlap;
	}

	/*int getBloodlineDistance(Entity horse1, Entity horse2){
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
	}*/

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

	void tweakAndLimitAttributes(AbstractHorse horse){
		//Natural min, max: jump(0.4,1.0), speed(0.1125,0.3375 | 4.8375,14.5125), health(15,30)
		//For average 2 parents and 3rd, random horse, have 3rd horse stats be [min-max]%90, [max-min(parents)]%10
		//Do above, but smooth curve (point1=min, point2=max, point3=parent)
		//jump
		double jump = horse.getJumpStrength();
		if(tweakStatsAtBirth) jump *= ((rand.nextInt(6)+97)/100.0);//remove up to 3% or add up to 2% of the speed
		if(MAX_JUMP != -1 && jump > MAX_JUMP) jump = MAX_JUMP;
		horse.setJumpStrength(jump);
		//speed
		double speed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		if(tweakStatsAtBirth) speed *= ((rand.nextInt(6)+97)/100.0);//remove up to 3% or add up to 2% of the speed
		if(MAX_SPEED != -1 && speed > MAX_SPEED) speed = MAX_SPEED;
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
		//health
		double health = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		if(tweakStatsAtBirth) health *= ((rand.nextInt(6)+97)/100.0);//remove up to 3% or add up to 2% of the health
		if(MAX_HEALTH != -1 && health > MAX_HEALTH) health = MAX_HEALTH;
		horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
		horse.setHealth(health);
		if(PRINT_DEBUG) plugin.getLogger().info("Tweaked jump/speed/health: "+jump+" / "+speed+" / "+health);
	}

	void generateAttributesWithinLimit(AbstractHorse horse, AbstractHorse mother, AbstractHorse father){
		//Natural min, max: jump(0.4,1.0), speed(0.1125,0.3375 | 4.8375,14.5125), health(15,30)
		double mother_jump = mother.getJumpStrength();
		double father_jump = father.getJumpStrength();
		double mother_speed = mother.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		double father_speed = father.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		double mother_health = mother.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double father_health = father.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double max_jump = Math.max(MIN_JUMP, Math.min(mother_jump, father_jump));
		double max_speed = Math.max(MIN_SPEED, Math.min(mother_speed, father_speed));
		double max_health = Math.max(MIN_HEALTH, Math.min(mother_health, father_health));
		if(MAX_JUMP != -1 && max_jump > MAX_JUMP) max_jump = MAX_JUMP;
		if(MAX_SPEED != -1 && max_speed > MAX_SPEED) max_speed = MAX_SPEED;
		if(MAX_HEALTH != -1 && max_health > MAX_HEALTH) max_health = MAX_HEALTH;
		//compute random 3rd parent
		double random_jump = MIN_JUMP + rand.nextDouble()*(max_jump - MIN_JUMP);
		double random_speed = MIN_SPEED + rand.nextDouble()*(max_speed - MIN_SPEED);
		double random_health = MIN_HEALTH + rand.nextDouble()*(max_health - MIN_HEALTH);
		//set jump
		double jump = (random_jump + mother_jump + father_jump) / 3D;
		horse.setJumpStrength(jump);
		//set speed
		double speed = (random_speed + mother_speed + father_speed) / 3D;
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
		//set health
		double health = (random_health + mother_health + father_health) / 3D;
		horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
		horse.setHealth(health);
		if(PRINT_DEBUG){
			plugin.getLogger().info("Father jump/speed/health: "+father_jump+" / "+father_speed+" / "+father_health);
			plugin.getLogger().info("Mother jump/speed/health: "+mother_jump+" / "+mother_speed+" / "+mother_health);
			plugin.getLogger().info("Offspring jump/speed/health: "+jump+" / "+speed+" / "+health);
		}
	}

	@EventHandler
	public void onBreed(EntityBreedEvent evt){
		if(evt.getEntity() instanceof AbstractHorse){
			if(PRINT_DEBUG) plugin.getLogger().info("EntityBreedEvent called");
			AbstractHorse h = (AbstractHorse)evt.getEntity();
			generateAttributesWithinLimit(h, (AbstractHorse)evt.getMother(), (AbstractHorse)evt.getFather());

			UUID owner = getOwnerFromParents(evt.getMother(), evt.getFather(), ownerAtBirth);
			if(nameAtBirth){
				h.setCustomName(getRandomName());
				if(claimAtBirth && owner != null) plugin.addClaimedHorse(owner, h);
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

				// TODO: DNA is currently only used for inbreeding mutations
				String dna = getOffspringDNA(evt.getMother(), evt.getFather());
				HorseLibrary.setDNA(h, dna);
				plugin.updateData(evt.getMother());//Update mother & father (in case DNA was generated)
				plugin.updateData(evt.getFather());
			}
			if(inbredMutation){
				int overlapPercent = getGeneticOverlap(evt.getMother(), evt.getFather()); // [0,100]
				if(PRINT_DEBUG) plugin.getLogger().info("Genetic overlap of bred horses: "+overlapPercent+"%");
				if(overlapPercent > INBRED_THRESHOLD){
					double overlapMod = overlapPercent/100D; // [0,1]

					//remove up to 70% of the jump or add up to 6% of the jump
					double jump = h.getJumpStrength() * uniformRandom(1D - 0.70*overlapMod, 1D + 0.06*overlapMod);
					h.setJumpStrength(jump);
	
					//remove up to 70% of the speed or add up to 6% of the speed
					double speed = h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() *
							uniformRandom(1D - 0.70*overlapMod, 1D + 0.06*overlapMod);
					h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
	
					//remove up to 100% of the health or add up to 5% of the health
					double health = h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() *
							uniformRandom(1D - 1.00*overlapMod, 1D + 0.05*overlapMod);
					h.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
					h.setHealth(health);
					if(PRINT_DEBUG) plugin.getLogger().info("Mutated(Inbred) jump/speed/health: "+jump+" / "+speed+" / "+health);
				}
			}
			tweakAndLimitAttributes(h);
			plugin.updateData(h);
		}
	}
}