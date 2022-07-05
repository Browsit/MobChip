package me.gamercoder215.mobchip.abstraction;

import me.gamercoder215.mobchip.EntityBody;
import me.gamercoder215.mobchip.ai.behavior.BehaviorResult;
import me.gamercoder215.mobchip.ai.controller.EntityController;
import me.gamercoder215.mobchip.ai.goal.*;
import me.gamercoder215.mobchip.ai.goal.target.*;
import me.gamercoder215.mobchip.ai.memories.Memory;
import me.gamercoder215.mobchip.ai.navigation.EntityNavigation;
import me.gamercoder215.mobchip.ai.navigation.NavigationNode;
import me.gamercoder215.mobchip.ai.navigation.NavigationPath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftSound;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.entity.*;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ChipUtil1_18_R2 implements ChipUtil {

    private static ItemStack fromNMS(net.minecraft.world.item.ItemStack item) { return CraftItemStack.asBukkitCopy(item); }

    @Override
    public void addCustomPathfinder(CustomPathfinder p, int priority, boolean target) {
        Mob m = p.getEntity();
        net.minecraft.world.entity.Mob mob = toNMS(m);
        GoalSelector s = target ? mob.targetSelector : mob.goalSelector;

        Goal g = new Goal() {
            @Override
            public boolean canUse() {
                return p.canStart();
            }
            @Override
            public boolean canContinueToUse() {
                return p.canContinueToUse();
            }
            @Override
            public boolean isInterruptable() {
                return p.canInterrupt();
            }

            @Override
            public void start() {
                p.start();
            }

            @Override
            public void tick() {
                p.tick();
            }

        };

        Pathfinder.PathfinderFlag[] flags = p.getFlags() == null ? new Pathfinder.PathfinderFlag[0] : p.getFlags();
        for (Pathfinder.PathfinderFlag f : flags) {
            EnumSet<Goal.Flag> nmsFlags = g.getFlags() == null ? EnumSet.allOf(Goal.Flag.class) : EnumSet.copyOf(g.getFlags());
            nmsFlags.add(toNMS(f));
            g.setFlags(nmsFlags);
        }

        s.addGoal(priority, g);
    }

    @Override
    public Set<WrappedPathfinder> getGoals(Mob m, boolean target) {
        net.minecraft.world.entity.Mob mob = toNMS(m);
        GoalSelector s = target ? mob.targetSelector : mob.goalSelector;

        Set<WrappedPathfinder> pF = new HashSet<>();
        s.getAvailableGoals().forEach(w -> pF.add(new WrappedPathfinder(fromNMS(w.getGoal()), w.getPriority())));

        return pF;
    }

    @Override
    public Collection<WrappedPathfinder> getRunningGoals(Mob m, boolean target) {
        net.minecraft.world.entity.Mob mob = toNMS(m);
        GoalSelector s = target ? mob.targetSelector : mob.goalSelector;

        Collection<WrappedPathfinder> l = new HashSet<>();
        s.getRunningGoals().forEach(w -> l.add(new WrappedPathfinder(fromNMS(w.getGoal()), w.getPriority())));

        return l;
    }

    @Override
    public void setFlag(Mob m, Pathfinder.PathfinderFlag flag, boolean target, boolean value) {
        net.minecraft.world.entity.Mob mob = toNMS(m);
        GoalSelector s = target ? mob.targetSelector : mob.goalSelector;
        if (value) s.enableControlFlag(toNMS(flag)); else s.disableControlFlag(toNMS(flag));
    }

    private static Class<? extends net.minecraft.world.entity.LivingEntity> toNMS(Class<? extends LivingEntity> clazz) {
        try {
            Method m = clazz.getDeclaredMethod("getHandle");
            return m.getReturnType().asSubclass(net.minecraft.world.entity.LivingEntity.class);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());

            return null;
        }
    }

    private static net.minecraft.world.item.ItemStack toNMS(ItemStack i) {
        return CraftItemStack.asNMSCopy(i);
    }

    private static SoundEvent toNMS(Sound s) {
        return CraftSound.getSoundEffect(s);
    }

    @Override
    public void addPathfinder(Pathfinder b, int priority, boolean target) {
        Mob mob = b.getEntity();
        net.minecraft.world.entity.Mob m = toNMS(mob);
        GoalSelector s = target ? m.targetSelector : m.goalSelector;

        String name = b.getInternalName().startsWith("PathfinderGoal") ? b.getInternalName().replace("PathfinderGoal", "") : b.getInternalName();

        final Goal g = switch (b.getInternalName()) {
            case "AvoidTarget" -> {
                PathfinderAvoidEntity<?> p = (PathfinderAvoidEntity<?>) b;
                yield new AvoidEntityGoal<>((PathfinderMob) m, toNMS(p.getFilter()), p.getMaxDistance(), p.getSpeedModifier(), p.getSprintModifier());
            }
            case "ArrowAttack" -> {
                PathfinderRangedAttack p = (PathfinderRangedAttack) b;
                yield new RangedAttackGoal((RangedAttackMob) m, p.getSpeedModifier(), p.getMinAttackInterval(), p.getMaxAttackInterval(), p.getRange());
            }
            case "Beg" -> {
                PathfinderBeg p = (PathfinderBeg) b;
                yield new BegGoal((net.minecraft.world.entity.animal.Wolf) m, p.getRange());
            }
            case "BowShoot" -> {
                PathfinderRangedBowAttack p = (PathfinderRangedBowAttack) b;
                yield new RangedBowAttackGoal((net.minecraft.world.entity.monster.Monster) m, p.getSpeedModifier(), p.getInterval(), p.getRange());
            }
            case "BreakDoor" -> {
                PathfinderBreakDoor p = (PathfinderBreakDoor) b;
                yield new BreakDoorGoal(m, p.getBreakTime(), d -> p.getCondition().test(fromNMS(d)));
            }
            case "Breath" -> {
                PathfinderBreathAir p = (PathfinderBreathAir) b;
                yield new BreathAirGoal((PathfinderMob) m);
            }
            case "Breed" -> {
                PathfinderBreed p = (PathfinderBreed) b;
                yield new BreedGoal((net.minecraft.world.entity.animal.Animal) m, p.getSpeedModifier());
            }
            case "CatSitOnBed" -> {
                PathfinderCatOnBed p = (PathfinderCatOnBed) b;
                yield new CatLieOnBedGoal((net.minecraft.world.entity.animal.Cat) m, p.getSpeedModifier(), Math.min((int) p.getRange(), 1));
            }
            case "ClimbOnTopOfPowderSnowGoal" -> {
                PathfinderClimbPowderedSnow p = (PathfinderClimbPowderedSnow) b;
                yield new ClimbOnTopOfPowderSnowGoal(m, toNMS(mob.getWorld()));
            }
            case "CrossbowAttack" -> {
                PathfinderRangedCrossbowAttack p = (PathfinderRangedCrossbowAttack) b;
                yield new RangedCrossbowAttackGoal((net.minecraft.world.entity.monster.Monster) m, p.getSpeedModifier(), p.getRange());
            }
            case "DoorOpen" -> {
                PathfinderOpenDoor p = (PathfinderOpenDoor) b;
                yield new OpenDoorGoal(m, p.mustClose());
            }
            case "EatTile" -> {
                PathfinderEatTile p = (PathfinderEatTile) b;
                yield new EatBlockGoal(m);
            }
            case "FishSchool" -> {
                PathfinderFollowFishLeader p = (PathfinderFollowFishLeader) b;
                yield new FollowFlockLeaderGoal((AbstractSchoolingFish) m);
            }
            case "FleeSun" -> {
                PathfinderFleeSun p = (PathfinderFleeSun) b;
                yield new FleeSunGoal((PathfinderMob) m, p.getSpeedModifier());
            }
            case "Float" -> {
                PathfinderFloat p = (PathfinderFloat) b;
                yield new FloatGoal(m);
            }
            case "FollowBoat" -> {
                PathfinderFollowBoat p = (PathfinderFollowBoat) b;
                yield new FollowBoatGoal((PathfinderMob) m);
            }
            case "FollowEntity" -> {
                PathfinderFollowMob p = (PathfinderFollowMob) b;
                yield new FollowMobGoal(m, p.getSpeedModifier(), p.getStopDistance(), p.getRange());
            }
            case "FollowOwner" -> {
                PathfinderFollowOwner p = (PathfinderFollowOwner) b;
                yield new FollowOwnerGoal((TamableAnimal) m, p.getSpeedModifier(), p.getStartDistance(), p.getStopDistance(), p.canFly());
            }
            case "FollowParent" -> {
                PathfinderFollowParent p = (PathfinderFollowParent) b;
                yield new FollowParentGoal((TamableAnimal) m, p.getSpeedModifier());
            }
            case "JumpOnBlock" -> {
                PathfinderCatOnBlock p = (PathfinderCatOnBlock) b;
                yield new CatSitOnBlockGoal((net.minecraft.world.entity.animal.Cat) m, p.getSpeedModifier());
            }
            case "LeapAtTarget" -> {
                PathfinderLeapAtTarget p = (PathfinderLeapAtTarget) b;
                yield new LeapAtTargetGoal(m, p.getHeight());
            }
            case "LlamaFollow" -> {
                PathfinderLlamaFollowCaravan p = (PathfinderLlamaFollowCaravan) b;
                yield new LlamaFollowCaravanGoal((net.minecraft.world.entity.animal.horse.Llama) m, p.getSpeedModifier());
            }
            case "LookAtPlayer" -> {
                PathfinderLookAtEntity<?> p = (PathfinderLookAtEntity) b;
                yield new LookAtPlayerGoal(m, toNMS(p.getFilter()), p.getRange(), p.getProbability(), p.isHorizontal());
            }
            case "LookAtTradingPlayer" -> {
                PathfinderLookAtTradingPlayer p = (PathfinderLookAtTradingPlayer) b;
                yield new LookAtTradingPlayerGoal((net.minecraft.world.entity.npc.AbstractVillager) m);
            }
            case "MeleeAttack" -> {
                PathfinderMeleeAttack p = (PathfinderMeleeAttack) b;
                yield new MeleeAttackGoal((PathfinderMob) m, p.getSpeedModifier(), p.mustSee());
            }
            case "MoveThroughVillage" -> {
                PathfinderMoveThroughVillage p = (PathfinderMoveThroughVillage) b;
                yield new MoveThroughVillageGoal((net.minecraft.world.entity.npc.Villager) m, p.getSpeedModifier(), p.mustBeNight(), p.getMinDistance(), p.canUseDoors());
            }
            case "MoveTowardsRestriction" -> {
                PathfinderMoveTowardsRestriction p = (PathfinderMoveTowardsRestriction) b;
                yield new MoveTowardsRestrictionGoal((PathfinderMob) m, p.getSpeedModifier());
            }
            case "MoveTowardsTarget" -> {
                PathfinderMoveTowardsTarget p = (PathfinderMoveTowardsTarget) b;
                yield new MoveTowardsTargetGoal((PathfinderMob) m, p.getSpeedModifier(), p.getRange());
            }
            case "NearestVillage" -> {
                PathfinderRandomStrollThroughVillage p = (PathfinderRandomStrollThroughVillage) b;
                yield new StrollThroughVillageGoal((PathfinderMob) m, p.getInterval());
            }
            case "OcelotAttack" -> {
                PathfinderOcelotAttack p = (PathfinderOcelotAttack) b;
                yield new OcelotAttackGoal(m);
            }
            case "OfferFlower" -> {
                PathfinderOfferFlower p = (PathfinderOfferFlower) b;
                yield new OfferFlowerGoal((net.minecraft.world.entity.animal.IronGolem) m);
            }
            case "Panic" -> {
                PathfinderPanic p = (PathfinderPanic) b;
                yield new PanicGoal((PathfinderMob) m, p.getSpeedModifier());
            }
            case "Perch" -> {
                PathfinderRideShoulder p = (PathfinderRideShoulder) b;
                yield new LandOnOwnersShoulderGoal((ShoulderRidingEntity) m);
            }
            case "Raid" -> {
                PathfinderMoveToRaid p = (PathfinderMoveToRaid) b;
                yield new PathfindToRaidGoal<>((net.minecraft.world.entity.raid.Raider) m);
            }
            case "RandomFly" -> {
                PathfinderRandomStrollFlying p = (PathfinderRandomStrollFlying) b;
                yield new WaterAvoidingRandomFlyingGoal((PathfinderMob) m, p.getSpeedModifier());
            }
            case "RandomLookaround" -> {
                PathfinderRandomLook p = (PathfinderRandomLook) b;
                yield new RandomLookAroundGoal(m);
            }
            case "RandomStroll" -> {
                PathfinderRandomStroll p = (PathfinderRandomStroll) b;
                yield new RandomStrollGoal((PathfinderMob) m, p.getSpeedModifier(), p.getInterval());
            }
            case "RandomStrollLand" -> {
                PathfinderRandomStrollLand p = (PathfinderRandomStrollLand) b;
                yield new WaterAvoidingRandomStrollGoal((PathfinderMob) m, p.getSpeedModifier(), p.getProbability());
            }
            case "RandomSwim" -> {
                PathfinderRandomSwim p = (PathfinderRandomSwim) b;
                yield new RandomSwimmingGoal((PathfinderMob) m, p.getSpeedModifier(), p.getInterval());
            }
            case "RemoveBlock" -> {
                PathfinderRemoveBlock p = (PathfinderRemoveBlock) b;
                yield new RemoveBlockGoal(((CraftBlock) p.getBlock()).getNMS().getBlock(), (PathfinderMob) m, p.getSpeedModifier(), Math.min((int) p.getBlock().getLocation().distance(mob.getLocation()), 1));
            }
            case "RestrictSun" -> {
                PathfinderRestrictSun p = (PathfinderRestrictSun) b;
                yield new RestrictSunGoal((PathfinderMob) m);
            }
            case "Sit" -> {
                PathfinderSit p = (PathfinderSit) b;
                yield new SitWhenOrderedToGoal((TamableAnimal) m);
            }
            case "StrollVillage" -> {
                PathfinderRandomStrollToVillage p = (PathfinderRandomStrollToVillage) b;
                yield new MoveBackToVillageGoal((PathfinderMob) m, p.getSpeedModifier(), true);
            }
            case "StrollVillageGolem" -> {
                PathfinderRandomStrollInVillage p = (PathfinderRandomStrollInVillage) b;
                yield new GolemRandomStrollInVillageGoal((PathfinderMob) m, p.getSpeedModifier());
            }
            case "Swell" -> {
                PathfinderSwellCreeper p = (PathfinderSwellCreeper) b;
                yield new SwellGoal((net.minecraft.world.entity.monster.Creeper) m);
            }
            case "Tame" -> {
                PathfinderTameHorse p = (PathfinderTameHorse) b;
                yield new RunAroundLikeCrazyGoal((net.minecraft.world.entity.animal.horse.AbstractHorse) m, p.getSpeedModifier());
            }
            case "Tempt" -> {
                PathfinderTempt p = (PathfinderTempt) b;
                yield new TemptGoal((PathfinderMob) m, p.getSpeedModifier(), Ingredient.of(p.getItems().stream().map(CraftItemStack::asNMSCopy)), true);
            }
            case "TradeWithPlayer" -> {
                PathfinderTradePlayer p = (PathfinderTradePlayer) b;
                yield new TradeWithPlayerGoal((net.minecraft.world.entity.npc.AbstractVillager) m);
            }
            case "UseItem" -> {
                PathfinderUseItem p = (PathfinderUseItem) b;
                yield new UseItemGoal<>(m, toNMS(p.getItem()), toNMS(p.getFinishSound()), e -> p.getCondition().test(fromNMS(e)));
            }
            case "Water" -> {
                PathfinderFindWater p = (PathfinderFindWater) b;
                yield new TryFindWaterGoal((PathfinderMob) m);
            }
            case "WaterJump" -> {
                PathfinderDolphinJump p = (PathfinderDolphinJump) b;
                yield new DolphinJumpGoal((net.minecraft.world.entity.animal.Dolphin) m, p.getInterval());
            }
            case "ZombieAttack" -> {
                PathfinderZombieAttack p = (PathfinderZombieAttack) b;
                yield new ZombieAttackGoal((net.minecraft.world.entity.monster.Zombie) m, p.getSpeedModifier(), p.mustSee());
            }
            case "UniversalAngerReset" -> {
                PathfinderResetAnger p = (PathfinderResetAnger) b;
                yield new ResetUniversalAngerTargetGoal<>((net.minecraft.world.entity.Mob & NeutralMob) m, p.isAlertingOthers());
            }

            // Target

            case "DefendVillage" -> {
                PathfinderDefendVillage p = (PathfinderDefendVillage) b;
                yield new DefendVillageTargetGoal((net.minecraft.world.entity.animal.IronGolem) m);
            }
            case "HurtByTarget" -> {
                PathfinderHurtByTarget p = (PathfinderHurtByTarget) b;
                List<Class<? extends net.minecraft.world.entity.LivingEntity>> classes = new ArrayList<>();
                p.getIgnoring().stream().map(EntityType::getEntityClass).forEach(c -> classes.add(toNMS(c.asSubclass(LivingEntity.class))));

                yield new HurtByTargetGoal((PathfinderMob) m, classes.toArray(new Class[0]));
            }
            case "NearestAttackableTarget" -> {
                PathfinderNearestAttackableTarget p = (PathfinderNearestAttackableTarget) b;
                yield new NearestAttackableTargetGoal<>(m, toNMS(p.getFilter()), p.getInterval(), p.mustSee(), p.mustReach(), t -> p.getCondition().test(fromNMS(t)));
            }
            case "OwnerHurtByTarget" -> {
                PathfinderOwnerHurtByTarget p = (PathfinderOwnerHurtByTarget) b;
                yield new OwnerHurtByTargetGoal((TamableAnimal) m);
            }
            case "OwnerHurtTarget" -> {
                PathfinderOwnerHurtTarget p = (PathfinderOwnerHurtTarget) b;
                yield new OwnerHurtTargetGoal((TamableAnimal) m);
            }

            default -> {
                if (b instanceof CustomPathfinder p) {
                    yield new Goal() {
                        @Override
                        public boolean canUse() {
                            return p.canStart();
                        }

                        @Override
                        public boolean canContinueToUse() {
                            return p.canContinueToUse();
                        }

                        @Override
                        public boolean isInterruptable() {
                            return p.canInterrupt();
                        }

                        @Override
                        public void start() {
                            p.start();
                        }

                        @Override
                        public void tick() {
                            p.tick();
                        }
                    };
                } else yield null;
            }
        };

        if (g == null) return;
        s.addGoal(priority, g);
    }

    private static BehaviorResult.Status fromNMS(Behavior.Status status) {
        if (status == Behavior.Status.STOPPED) return BehaviorResult.Status.STOPPED;
        return BehaviorResult.Status.RUNNING;
    }

    private static final class BehaviorResult1_18_R2 extends BehaviorResult {
        private final Behavior<? super net.minecraft.world.entity.LivingEntity> b;
        private final net.minecraft.world.entity.Mob mob;
        private final ServerLevel l;

        private BehaviorResult1_18_R2(Behavior<? super net.minecraft.world.entity.LivingEntity> b, net.minecraft.world.entity.Mob mob) {
            this.b = b;
            this.mob = mob;
            this.l = toNMS(Bukkit.getWorld(mob.level.getWorld().getUID()));

            b.tryStart(l, mob, 0);
        }

        @Override
        public @NotNull Status getStatus() {
            return fromNMS(b.getStatus());
        }

        @Override
        public void stop() {
            b.doStop(l, mob, 0);
        }
    }

    private static LivingEntity fromNMS(net.minecraft.world.entity.LivingEntity l) {
        return (LivingEntity) l.getBukkitEntity();
    }

    private static MemoryModuleType<?> toNMS(Memory<?> mem) {
        return Registry.MEMORY_MODULE_TYPE.get(new ResourceLocation(mem.getKey().getKey()));
    }

    @Override
    public BehaviorResult runBehavior(Mob m, String behaviorName, Object... args) {
        return runBehavior(m, behaviorName, Behavior.class.getPackage().getName(), args);
    }

    @Override
    public BehaviorResult runBehavior(Mob m, String behaviorName, String packageName, Object... args) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        String packageN = packageName.replace("{V}", "v1_18_R2");

        for (int i = 0; i < args.length; i++) {
            Object o = args[i];
            if (o instanceof Villager.Profession) args[i] = toNMS((Villager.Profession) o);
            if (o instanceof Memory<?>) args[i] = toNMS((Memory<?>) o);

            if (o instanceof Predicate) args[i] = (Predicate) obj -> {
                if (obj instanceof net.minecraft.world.entity.Mob) return ((Predicate<Mob>) o).test(fromNMS((net.minecraft.world.entity.Mob) obj));

                return ((Predicate) o).test(obj);
            };

            if (o instanceof Function) args[i] = (Function) obj -> {
                if (obj instanceof net.minecraft.world.entity.LivingEntity) return ((Function<LivingEntity, ?>) o).apply(fromNMS((net.minecraft.world.entity.LivingEntity) obj));

                return ((Function) o).apply(obj);
            };
        }

        try {
            Class<?> bClass = Class.forName(packageName + "." + behaviorName);
            Constructor<?> c = bClass.getConstructor(Arrays.stream(args).map(Object::getClass).toArray(Class[]::new));
            Behavior<? super net.minecraft.world.entity.LivingEntity> b = (Behavior<? super net.minecraft.world.entity.LivingEntity>) c.newInstance(args);
            return new BehaviorResult1_18_R2(b, nms);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return null;
        }
    }

    private static ServerPlayer toNMS(Player p) { return ((CraftPlayer) p).getHandle(); }

    private static final class EntityController1_18_R2 implements EntityController {

        private final JumpControl jumpC;
        private final MoveControl moveC;
        private final LookControl lookC;

        private final Mob m;

        public EntityController1_18_R2(Mob m) {
            net.minecraft.world.entity.Mob nms = toNMS(m);
            this.lookC = nms.getLookControl();
            this.moveC = nms.getMoveControl();
            this.jumpC = nms.getJumpControl();
            this.m = m;
        }

        @Override
        public EntityController jump() {
            jumpC.jump();
            jumpC.tick();
            return this;
        }

        @Override
        public boolean isLookingAtTarget() {
            Vector dir = m.getLocation().getDirection();
            int x = dir.getBlockX();
            int y = dir.getBlockY();
            int z = dir.getBlockZ();
            return lookC.getWantedX() == x && lookC.getWantedY() == y && lookC.getWantedZ() == z;
        }

        @Override
        public EntityController moveTo(double x, double y, double z, double speedMod) {
            moveC.setWantedPosition(x, y, z, speedMod);
            moveC.tick();
            return this;
        }

        @Override
        public EntityController strafe(float fwd, float right) {
            moveC.strafe(fwd, right);
            moveC.tick();
            return this;
        }

        @Override
        public double getCurrentSpeedModifier() {
            return moveC.getSpeedModifier();
        }

        @Override
        public Location getTargetMoveLocation() {
            return new Location(m.getWorld(), moveC.getWantedX(), moveC.getWantedY(), moveC.getWantedZ());
        }

        @Override
        public Location getTargetLookLocation() {
            return new Location(m.getWorld(), lookC.getWantedX(), lookC.getWantedY(), lookC.getWantedZ());
        }

        @Override
        public EntityController lookAt(double x, double y, double z) {
            lookC.setLookAt(x, y, z);
            lookC.tick();
            return this;
        }

    }

    private static final class NavigationPath1_18_R2 implements NavigationPath {
        private String name;
        private final Mob m;
        private final Path handle;

        NavigationPath1_18_R2(@NotNull Path nms, @NotNull Mob m) {
            this.m = m;
            this.name = "bukkitpath";
            this.handle = nms;
        }

        private final List<NavigationNode> nodes = new ArrayList<>();

        /**
         * Advances this path.
         */
        @Override
        public void advance() {
            this.getHandle().advance();
            Node n = handle.getNextNode();
            new EntityController1_18_R2(m).moveTo(n.x, n.y, n.z);
        }

        /**
         * Get this Path's Name.
         * @return this path's name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Sets this Path's Name.
         * @param name this path's new name
         */
        public void setName(@NotNull String name) {
            this.name = name;
        }

        public Path getHandle() {
            return this.handle;
        }

        /**
         * Whether this NavigationPath is complete.
         * @return true if complete, else false
         */
        @Override
        public boolean isDone() {
            return this.handle.isDone();
        }

        /**
         * Get the size of this NavigationPath.
         * @return size
         */
        public int size() {
            return nodes.size();
        }

        /**
         * Whether this NavigationPath is empty.
         * @return true if empty, else false
         */
        @Override
        public boolean isEmpty() {
            return nodes.isEmpty();
        }

        /**
         * Whether this Path contains this Navigation Node.
         * @param o NavigationNode
         * @return true if contains, else false
         */
        @Override
        public boolean contains(@Nullable NavigationNode o) {
            return nodes.contains(o);
        }

        @Override
        @NotNull
        public Iterator<NavigationNode> iterator() {
            return nodes.iterator();
        }

        /**
         * Converts this NavigationPath into an Array of Nodes.
         * @return Array of NavigationNode
         */
        @NotNull
        @Override
        public NavigationNode[] toArray() {
            return nodes.toArray(new NavigationNode[0]);
        }

        /**
         * Returns the index of this Navigation Node.
         * @param o NavigationNode to fetch
         * @return Index found
         * @see List#indexOf(Object)
         */
        @Override
        public int indexOf(@Nullable NavigationNode o) {
            return nodes.indexOf(o);
        }

        /**
         * Returns the last index of this Navigation Node.
         * @param o NavigationNode to fetch
         * @return Index found
         * @see List#lastIndexOf(Object)
         */
        @Override
        public int lastIndexOf(@Nullable NavigationNode o) {
            return nodes.lastIndexOf(o);
        }
    }

    private static final class EntityNavigation1_18_R2 implements EntityNavigation {

        private final PathNavigation handle;

        private int speedMod;
        private int range;
        private final List<NavigationNode> points;
        private BlockPos finalPos;

        private final Mob m;

        EntityNavigation1_18_R2(Mob m) {
            this.handle = toNMS(m).getNavigation();
            this.points = new ArrayList<>();

            this.speedMod = 1;
            this.range = Integer.MAX_VALUE;
            this.m = m;
        }

        @Override
        public double getSpeedModifier() {
            return this.speedMod;
        }

        @Override
        public void setSpeedModifier(double mod) throws IllegalArgumentException {
            if (mod > Integer.MAX_VALUE) throw new IllegalArgumentException("Must be integer");
            this.speedMod = (int) Math.floor(mod);
        }

        @Override
        public EntityNavigation recompute() {
            this.handle.recomputePath();
            return this;
        }

        @Override
        public EntityNavigation addPoint(@NotNull NavigationNode point) {
            this.points.add(point);
            return this;
        }

        @Override
        public EntityNavigation addPoint(int index, @NotNull NavigationNode point) {
            this.points.add(index, point);
            return this;
        }

        @Override
        public EntityNavigation removePoint(@NotNull NavigationNode point) {
            this.points.remove(point);
            return this;
        }

        @Override
        public EntityNavigation removePoint(int index) {
            this.points.remove(index);
            return this;
        }

        @Override
        @NotNull
        public NavigationPath buildPath() {
            return new NavigationPath1_18_R2(handle.createPath(finalPos, range, speedMod), m);
        }

        @Override
        public EntityNavigation setFinalPoint(@NotNull NavigationNode node) {
            this.finalPos = new BlockPos(node.getX(), node.getY(), node.getZ());
            return this;
        }

        @Override
        public EntityNavigation setRange(int range) {
            this.range = range;
            return this;
        }
    }

    private static final class EntityBody1_18_R2 implements EntityBody {
        private final net.minecraft.world.entity.Mob nmsMob;

        EntityBody1_18_R2(Mob nmsMob) {
            this.nmsMob = toNMS(nmsMob);
        }

        /**
         * Whether this Entity is Left Handed.
         *
         * @return true if left-handed, else false
         */
        @Override
        public boolean isLeftHanded() {
            return nmsMob.isLeftHanded();
        }

        /**
         * Sets this Entity to be left-handed.
         *
         * @param leftHanded true if left-handed, else false
         */
        @Override
        public void setLeftHanded(boolean leftHanded) {
            nmsMob.setLeftHanded(leftHanded);
        }

        @Override
        public boolean canBreatheUnderwater() {
            return nmsMob.canBreatheUnderwater();
        }

        @Override
        public boolean shouldDiscardFriction() {
            return nmsMob.shouldDiscardFriction();
        }

        @Override
        public void setDiscardFriction(boolean discard) {
            nmsMob.setDiscardFriction(discard);
        }

        /**
         * Makes this Mob interact with a Player.
         *
         * @param p    Player to interact with
         * @param hand Hand to use
         * @return Result of interaction
         */
        @Override
        public InteractionResult interact(@NotNull Player p, @Nullable InteractionHand hand) {
            final net.minecraft.world.InteractionHand h;

            if (hand == InteractionHand.OFF_HAND) h = net.minecraft.world.InteractionHand.OFF_HAND;
            else h = net.minecraft.world.InteractionHand.MAIN_HAND;

            return switch (nmsMob.interact(toNMS(p), h)) {
                case SUCCESS -> InteractionResult.SUCCESS;
                case CONSUME -> InteractionResult.CONSUME;
                case CONSUME_PARTIAL -> InteractionResult.CONSUME_PARTIAL;
                case FAIL -> InteractionResult.FAIL;
                default -> InteractionResult.PASS;
            };
        }

        @Override
        public boolean isSensitiveToWater() {
            return nmsMob.isSensitiveToWater();
        }

        @Override
        public boolean isAffectedByPotions() {
            return nmsMob.isAffectedByPotions();
        }

        @Override
        public boolean isBlocking() {
            return nmsMob.isBlocking();
        }

        @Override
        public float getArmorCoverPercentage() {
            return nmsMob.getArmorCoverPercentage();
        }

        @Override
        public void useItem(@Nullable InteractionHand hand) {
            if (hand == null) return;

            final net.minecraft.world.InteractionHand h;
            if (hand == InteractionHand.OFF_HAND) h = net.minecraft.world.InteractionHand.OFF_HAND;
            else h = net.minecraft.world.InteractionHand.MAIN_HAND;

            nmsMob.startUsingItem(h);
        }

        @Override
        public boolean isUsingItem() {
            return nmsMob.isUsingItem();
        }

        @Override
        public boolean isFireImmune() {
            return nmsMob.fireImmune();
        }

        @Override
        public boolean isSwinging() {
            return nmsMob.swinging;
        }

        @Override
        public boolean canRideUnderwater() {
            return nmsMob.rideableUnderWater();
        }

        @Override
        public boolean isInvisibleTo(@Nullable Player p) {
            return nmsMob.isInvisibleTo(toNMS(p));
        }

        @Override
        public @NotNull InteractionHand getMainHand() {
            if (nmsMob.getMainArm() == HumanoidArm.LEFT) return InteractionHand.OFF_HAND;
            return InteractionHand.MAIN_HAND;
        }
    }

    @Override
    public EntityController getController(Mob m) {
        return new EntityController1_18_R2(m);
    }

    @Override
    public EntityNavigation getNavigation(Mob m) {
        return new EntityNavigation1_18_R2(m);
    }

    @Override
    public EntityBody getBody(Mob m) {
        return new EntityBody1_18_R2(m);
    }

    private static DamageSource toNMS(EntityDamageEvent.DamageCause c) {
        return switch (c) {
            case FIRE -> DamageSource.IN_FIRE;
            case LIGHTNING -> DamageSource.LIGHTNING_BOLT;
            case FIRE_TICK -> DamageSource.ON_FIRE;
            case SUFFOCATION -> DamageSource.IN_WALL;
            case LAVA -> DamageSource.LAVA;
            case HOT_FLOOR -> DamageSource.HOT_FLOOR;
            case CRAMMING -> DamageSource.CRAMMING;
            case DROWNING -> DamageSource.DROWN;
            case STARVATION -> DamageSource.STARVE;
            case CONTACT -> DamageSource.CACTUS;
            case MAGIC -> DamageSource.MAGIC;
            case FALL -> DamageSource.FALL;
            case FLY_INTO_WALL -> DamageSource.FLY_INTO_WALL;
            case VOID -> DamageSource.OUT_OF_WORLD;
            case WITHER -> DamageSource.WITHER;
            case FALLING_BLOCK -> DamageSource.FALLING_BLOCK;
            case DRAGON_BREATH -> DamageSource.DRAGON_BREATH;
            case FREEZE -> DamageSource.FREEZE;
            case DRYOUT -> DamageSource.DRY_OUT;
            default -> DamageSource.GENERIC;
        };
    }

    private static ItemEntity toNMS(org.bukkit.entity.Item i) {
        return (ItemEntity) ((CraftItem) i).getHandle();
    }

    private static net.minecraft.world.entity.LivingEntity toNMS(LivingEntity en) {
        return ((CraftLivingEntity) en).getHandle();
    }

    private static Object toNMS(String key, Object value) {
        final Object nmsValue;

        if (value instanceof Location l) {
            if (key.equals("nearest_bed") || key.equals("celebrate_location") || key.equals("nearest_repellent")) nmsValue = new BlockPos(l.getX(), l.getY(), l.getZ());
            else nmsValue = GlobalPos.of(toNMS(l.getWorld()).dimension(), new BlockPos(l.getX(), l.getY(), l.getZ()));
        }
        else if (value instanceof Location[] ls) {
            List<GlobalPos> p = new ArrayList<>();

            for (Location l : ls) {
                p.add(GlobalPos.of(toNMS(l.getWorld()).dimension(), new BlockPos(l.getX(), l.getY(), l.getZ())));
            }

            nmsValue = p;
        }
        else if (value instanceof Player p) {
            if (key.equals("liked_player")) nmsValue = p.getUniqueId();
            else nmsValue = toNMS(p);
        }
        else if (value instanceof Memory.WalkingTarget t) nmsValue = new WalkTarget(toNMS(t.getLocation()), (float) t.getSpeedModifier(), t.getDistance());
        else if (value instanceof LivingEntity l) nmsValue = toNMS(l);
        else if (value instanceof Entity e) {
            if (key.equals("angry_at")) nmsValue = e.getUniqueId();
            else nmsValue = toNMS(e);
        }
        else if (value instanceof org.bukkit.block.Block[] b) {
            final Collection<GlobalPos> s;
            if (key.equals("doors_to_close")) s = new HashSet<>();
            else s = new ArrayList<>();

            for (org.bukkit.block.Block bl : b) {
                Location l = bl.getLocation();
                s.add(GlobalPos.of(toNMS(l.getWorld()).dimension(), new BlockPos(l.getX(), l.getY(), l.getZ())));
            }
            nmsValue = s;
        }
        else if (value instanceof Villager[] vs) {
            List<net.minecraft.world.entity.LivingEntity> s = new ArrayList<>();
            for (Villager v : vs) s.add(toNMS(v));
            nmsValue = s;
        }
        else if (value instanceof Player[] ps) {
            List<net.minecraft.world.entity.player.Player> s = new ArrayList<>();
            for (Player p : ps) s.add(toNMS(p));
            nmsValue = s;
        }
        else if (value instanceof LivingEntity[] ls) {
            List<net.minecraft.world.entity.LivingEntity> s = new ArrayList<>();
            for (LivingEntity l : ls) s.add(toNMS(l));
            nmsValue = s;
        }
        else if (value instanceof EntityDamageEvent.DamageCause c) nmsValue = toNMS(c);
        else nmsValue = value;

        return nmsValue;
    }

    private static Object fromNMS(Mob m, String key, Object nmsValue) {
        Object value = nmsValue;

        if (nmsValue instanceof GlobalPos l) {
            BlockPos pos = l.pos();
            World w = ((CraftServer) Bukkit.getServer()).getHandle().getServer().registryHolder.registryOrThrow(Registry.DIMENSION_REGISTRY).get(l.dimension()).getWorld();
            value = new Location(w, pos.getX(), pos.getY(), pos.getZ());
        }
        else if (nmsValue instanceof List<?> li) {
            switch (key) {
                case "secondary_job_site", "interactable_doors" -> {
                    List<Location> l = new ArrayList<>();
                    li.forEach(o -> l.add((Location) fromNMS(m, key, o)));
                    value = l.toArray(new Location[0]);
                }
                case "nearest_players" -> {
                    List<Player> l = new ArrayList<>();
                    li.forEach(o -> l.add(Bukkit.getPlayer(((net.minecraft.world.entity.player.Player) o).getUUID())));
                    value = l.toArray(new Player[0]);
                }
                case "visible_villager_babies" -> {
                    List<Villager> l = new ArrayList<>();
                    li.forEach(o -> l.add((Villager) fromNMS((net.minecraft.world.entity.LivingEntity) o)));
                    value = l.toArray(new Villager[0]);
                }
                case "mobs" -> {
                    List<LivingEntity> vl = new ArrayList<>();
                    li.forEach(o -> vl.add(fromNMS((net.minecraft.world.entity.LivingEntity) o)));
                    value = vl.toArray(new LivingEntity[0]);
                }
            }
        }
        else if (nmsValue instanceof net.minecraft.world.entity.player.Player p) value = Bukkit.getPlayer(p.getUUID());
        else if (nmsValue instanceof WalkTarget t) {
            BlockPos p = t.getTarget().currentBlockPosition();
            value = new Memory.WalkingTarget(new Location(m.getWorld(), p.getX(), p.getY(), p.getZ()), t.getSpeedModifier(), t.getCloseEnoughDist());
        }
        else if (nmsValue instanceof net.minecraft.world.entity.LivingEntity l) value = Bukkit.getEntity(l.getUUID());
        else if (nmsValue instanceof Set<?> s) {
            if (key.equals("doors_to_close")) {
                List<org.bukkit.block.Block> l = new ArrayList<>();
                s.forEach(o -> l.add((org.bukkit.block.Block) fromNMS(m, key, o)));
                value = l.toArray(new org.bukkit.block.Block[0]);
            }
        }
        else if (value instanceof DamageSource c) value = fromNMS(c);
        else value = nmsValue;

        return value;
    }

    private static EntityDamageEvent.DamageCause fromNMS(DamageSource c) {
        return switch (c.msgId) {
            case "inFire" -> FIRE;
            case "lightningBolt" -> LIGHTNING;
            case "onFire" -> FIRE_TICK;
            case "lava" -> LAVA;
            case "hotFloor" -> HOT_FLOOR;
            case "inWall" -> SUFFOCATION;
            case "cramming" -> CRAMMING;
            case "drown" -> DROWNING;
            case "starve" -> STARVATION;
            case "cactus", "sweetBerryBush", "stalagmite" -> CONTACT;
            case "fall" -> FALL;
            case "flyIntoWall" -> FLY_INTO_WALL;
            case "outOfWorld" -> VOID;
            case "magic" -> MAGIC;
            case "wither" -> WITHER;
            case "anvil", "fallingBlock", "fallingStalactite" -> FALLING_BLOCK;
            case "dragonBreath" -> DRAGON_BREATH;
            case "dryout" -> DRYOUT;
            case "freeze" -> FREEZE;
            default -> CUSTOM;
        };
    }


    @Override
    public <T> void setMemory(Mob mob, Memory<T> m, T value) {
        if (value == null) {
            removeMemory(mob, m);
            return;
        }

        net.minecraft.world.entity.Mob nms = toNMS(mob);
        MemoryModuleType type = toNMS(m);
        String key = Registry.MEMORY_MODULE_TYPE.getKey(type).getPath();
        Object nmsValue = toNMS(key, value);

        nms.getBrain().setMemory(type, nmsValue);
    }

    @Override
    public <T> void setMemory(Mob mob, Memory<T> m, T value, long durationTicks) {
        if (value == null) {
            removeMemory(mob, m);
            return;
        }

        net.minecraft.world.entity.Mob nms = toNMS(mob);
        MemoryModuleType type = toNMS(m);
        String key = Registry.MEMORY_MODULE_TYPE.getKey(type).getPath();
        Object nmsValue = toNMS(key, value);

        nms.getBrain().setMemoryWithExpiry(type, nmsValue, durationTicks);
    }

    @Override
    public <T> T getMemory(Mob mob, Memory<T> m) {
        net.minecraft.world.entity.Mob nms = toNMS(mob);
        MemoryModuleType type = toNMS(m);
        String key = Registry.MEMORY_MODULE_TYPE.getKey(type).getPath();

        return m.getBukkitClass().cast(fromNMS(mob, key, nms.getBrain().getMemory(type)));
    }

    @Override
    public long getExpiry(Mob mob, Memory<?> m) {
        net.minecraft.world.entity.Mob nms = toNMS(mob);
        MemoryModuleType type = toNMS(m);

        return nms.getBrain().getTimeUntilExpiry(type);
    }

    @Override
    public boolean contains(Mob mob, Memory<?> m) {
        net.minecraft.world.entity.Mob nms = toNMS(mob);
        MemoryModuleType type = toNMS(m);

        return nms.getBrain().hasMemoryValue(type);
    }

    @Override
    public void removeMemory(Mob mob, Memory<?> m) {
        net.minecraft.world.entity.Mob nms = toNMS(mob);
        MemoryModuleType<?> type = toNMS(m);
        nms.getBrain().eraseMemory(type);
    }

    @Override
    public boolean isRestricted(Mob m) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        return nms.isWithinRestriction();
    }

    @Override
    public void clearRestriction(Mob m) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        nms.clearRestriction();
    }

    @Override
    public void restrictTo(Mob m, double x, double y, double z, int radius) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        nms.restrictTo(new BlockPos(x, y, z), radius);
    }

    @Override
    public Location getRestriction(Mob m) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        BlockPos c = nms.getRestrictCenter();
        return new Location(m.getWorld(), c.getX(), c.getY(), c.getZ());
    }

    @Override
    public int getRestrictionRadius(Mob m) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        return ((int) nms.getRestrictRadius()) < 0 ? Integer.MAX_VALUE : (int) nms.getRestrictRadius();
    }

    @Override
    public boolean hasRestriction(Mob m) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        return nms.hasRestriction();
    }

    @Override
    public boolean canSee(Mob m, Entity en) {
        net.minecraft.world.entity.Mob nms = toNMS(m);
        return nms.getSensing().hasLineOfSight(toNMS(en));
    }

    private static net.minecraft.world.entity.Entity toNMS(Entity en) {
        return ((CraftEntity) en).getHandle();
    }

    private static VillagerProfession toNMS(Villager.Profession p) {
        return switch (p) {
            case FARMER -> VillagerProfession.FARMER;
            case FISHERMAN -> VillagerProfession.FISHERMAN;
            case LIBRARIAN -> VillagerProfession.LIBRARIAN;
            case WEAPONSMITH -> VillagerProfession.WEAPONSMITH;
            case TOOLSMITH -> VillagerProfession.TOOLSMITH;
            case BUTCHER -> VillagerProfession.BUTCHER;
            case FLETCHER -> VillagerProfession.FLETCHER;
            case MASON -> VillagerProfession.MASON;
            case CLERIC -> VillagerProfession.CLERIC;
            case ARMORER -> VillagerProfession.ARMORER;
            case NITWIT -> VillagerProfession.NITWIT;
            case SHEPHERD -> VillagerProfession.SHEPHERD;
            case CARTOGRAPHER -> VillagerProfession.CARTOGRAPHER;
            case LEATHERWORKER -> VillagerProfession.LEATHERWORKER;
            default -> VillagerProfession.NONE;
        };
    }

    private static net.minecraft.world.entity.Mob getEntity(Goal g, String name) {
        try {
            Class<?> clazz = g.getClass();
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return (net.minecraft.world.entity.Mob) f.get(g);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return null;
        }
    }

    private static <T extends Entity> Class<? extends T> fromNMS(Class<? extends net.minecraft.world.entity.Entity> clazz, Class<T> cast) {
        try {
            Class<?> bukkit = Entity.class.getDeclaredMethod("getBukkitEntity").getReturnType();
            return bukkit.asSubclass(cast);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());

            return cast;
        }
    }

    private static net.minecraft.world.entity.Mob toNMS(Mob m) { return ((CraftMob) m).getHandle(); }

    private static EntityType[] getEntityTypes(Class<?>... nms) {
        List<EntityType> types = new ArrayList<>();
        for (Class<?> c : nms) {

            Class<? extends Entity> bukkit = fromNMS((Class<? extends net.minecraft.world.entity.Entity>) c, Entity.class);
            for (EntityType t : EntityType.values()) if (t.getEntityClass().isAssignableFrom(bukkit)) types.add(t);
        }
        return types.toArray(new EntityType[0]);
    }

    private static Difficulty toNMS(org.bukkit.Difficulty d) {
        return switch (d) {
            case PEACEFUL -> Difficulty.PEACEFUL;
            case EASY -> Difficulty.EASY;
            case NORMAL -> Difficulty.NORMAL;
            case HARD -> Difficulty.HARD;
        };
    }

    private static org.bukkit.Difficulty fromNMS(Difficulty d) {
        return switch (d) {
            case PEACEFUL -> org.bukkit.Difficulty.PEACEFUL;
            case EASY -> org.bukkit.Difficulty.EASY;
            case NORMAL -> org.bukkit.Difficulty.NORMAL;
            case HARD -> org.bukkit.Difficulty.HARD;
        };
    }

    private static PathfinderMob toNMS(Creature c) { return ((CraftCreature) c).getHandle();}

    private Goal.Flag toNMS(Pathfinder.PathfinderFlag f) {
        return switch (f) {
            case MOVEMENT -> Goal.Flag.MOVE;
            case JUMPING -> Goal.Flag.JUMP;
            case TARGETING -> Goal.Flag.TARGET;
            case LOOKING -> Goal.Flag.LOOK;
        };
    }

    private static Pathfinder.PathfinderFlag fromNMS(Goal.Flag f) {
        return switch (f) {
            case MOVE -> Pathfinder.PathfinderFlag.MOVEMENT;
            case JUMP -> Pathfinder.PathfinderFlag.JUMPING;
            case TARGET -> Pathfinder.PathfinderFlag.TARGETING;
            case LOOK -> Pathfinder.PathfinderFlag.LOOKING;
        };
    }

    private static float getFloat(Goal o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getFloat(o);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return 0;
        }
    }

    private static double getDouble(Goal o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getDouble(o);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return 0;
        }
    }

    private static boolean getBoolean(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getBoolean(o);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return false;
        }
    }

    private static int getInt(Goal o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(o);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return 0;
        }
    }

    private static <T> T getObject(Goal o, String name, Class<T> clazz) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return clazz.cast(f.get(o));
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return null;
        }
    }

    private static Mob fromNMS(net.minecraft.world.entity.Mob m) { return (Mob) m.getBukkitEntity(); }

    private static World fromNMS(Level l) { return l.getWorld(); }

    private static ServerLevel toNMS(World w) { return ((CraftWorld) w).getHandle(); }

    private static BlockPos toNMS(Location l) { return new BlockPos(l.getX(), l.getY(), l.getZ()); }

    private static List<ItemStack> fromNMS(Ingredient in) { return Arrays.stream(in.itemStacks).map(CraftItemStack::asBukkitCopy).collect(Collectors.toList()); }

    private static Sound fromNMS(SoundEvent s) { return CraftSound.getBukkit(s); }

    private static Mob getEntity(Goal g) {
        try {
            for (Field f : g.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getDeclaringClass().isAssignableFrom(net.minecraft.world.entity.Mob.class) && Modifier.isFinal(f.getModifiers())) {
                    return fromNMS((net.minecraft.world.entity.Mob) f.get(g));
                }
            }
            if (g.getClass().getSuperclass().isAssignableFrom(Goal.class)) return getEntity((Goal) g.getClass().getSuperclass().cast(g));
            else return null;
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return null;
        }
    }

    private static Object invoke(Goal g, String method, Object... args) {
        try {
            Method m = g.getClass().getDeclaredMethod(method);
            m.setAccessible(true);

            return m.invoke(g, args);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
            return null;
        }
    }

    private static CustomPathfinder custom(Goal g) {
        return new CustomPathfinder(getEntity(g)) {
            @Override
            public @NotNull PathfinderFlag[] getFlags() {
                PathfinderFlag[] flags = new PathfinderFlag[g.getFlags().size()];
                int i = 0;
                for (Goal.Flag f : g.getFlags()) {
                    flags[i] = fromNMS(f);
                    i++;
                }
                return flags;
            }

            @Override
            public boolean canStart() {
                return g.canUse();
            }

            @Override
            public void start() {
                g.start();
            }

            @Override
            public void tick() {
                g.tick();
            }

            @Override
            public String getInternalName() {
                return g.getClass().getSimpleName();
            }
        };
    }

    private static BlockPos getPosWithBlock(net.minecraft.world.level.block.Block block, BlockPos bp, BlockGetter g) {
        if (g.getBlockState(bp).is(block)) return bp;
        else {
            BlockPos[] bp1 = new BlockPos[]{new BlockPos(bp.below()),  bp.west(), bp.east(), bp.north(), bp.south(), new BlockPos(bp.above())};
            for (BlockPos bps : bp1) if (g.getBlockState(bps).is(block)) return bps;
            return null;
        }
    }

    private static Location fromNMS(BlockPos p, World w) { return new Location(w, p.getX(), p.getY(), p.getZ()); }

    private Pathfinder fromNMS(Goal g) {
        Mob m = getEntity(g);
        String name = g.getClass().getSimpleName();

        if (name.startsWith("PathfinderGoal")) {
            name = name.replace("PathfinderGoal", "");

            return switch (name) {
                case "AvoidTarget" -> new PathfinderAvoidEntity<>((Creature) m, fromNMS(getObject(g, "f", Class.class), LivingEntity.class), getFloat(g, "c"), getDouble(g, "i"), getDouble(g, "j"));
                case "ArrowAttack" -> new PathfinderRangedAttack(m, getDouble(g, "e"), getFloat(g, "i"), getInt(g, "g"), getInt(g, "h"));
                case "Beg" -> new PathfinderBeg((Wolf) m);
                case "BowShoot" -> new PathfinderRangedBowAttack(m, getDouble(g, "b"), (float) Math.sqrt(getFloat(g, "d")), getInt(g, "c"));
                case "BreakDoor" -> new PathfinderBreakDoor(m, getInt(g, "i"), d -> getObject(g, "h", Predicate.class).test(toNMS(d)));
                case "Breath" -> new PathfinderBreathAir((Creature) m);
                case "Breed" -> new PathfinderBreed((Animals) m, getDouble(g, "g"));
                case "CatSitOnBed" -> new PathfinderCatOnBed((Cat) m, getDouble(MoveToBlockGoal.class.cast(g), "b"), getInt(MoveToBlockGoal.class.cast(g), "l"));
                case "CrossbowAttack" -> new PathfinderRangedCrossbowAttack((Pillager) m, getDouble(g, "d"), (float) Math.sqrt(getFloat(g, "e")));
                case "DoorOpen" -> new PathfinderOpenDoor(m, getBoolean(g, "a"));
                case "WaterJump" -> new PathfinderDolphinJump((Dolphin) m, getInt(g, "c"));
                case "EatTile" -> new PathfinderEatTile(m);
                case "Water" -> new PathfinderFindWater((Creature) m);
                case "FleeSun" -> new PathfinderFleeSun((Creature) m, getDouble(g, "e"));
                case "Float" -> new PathfinderFloat(m);
                case "FollowBoat" -> new PathfinderFollowBoat((Creature) m);
                case "FollowEntity" -> new PathfinderFollowMob(m, getDouble(g, "d"), getFloat(g, "g"), getFloat(g, "i"));
                case "FollowOwner" -> new PathfinderFollowOwner((Tameable) m, getDouble(g, "h"), getFloat(g, "l"), getFloat(g, "k"), getBoolean(g, "n"));
                case "FollowParent" -> new PathfinderFollowParent((Animals) m, getDouble(g, "f"));
                case "HorseTrap" -> new PathfinderSkeletonTrap((SkeletonHorse) m);
                case "LeapAtTarget" -> new PathfinderLeapAtTarget(m, getFloat(g, "c"));
                case "JumpOnBlock" -> new PathfinderCatOnBlock((Cat) m, getDouble(g, "g"));
                case "LlamaFollow" -> new PathfinderLlamaFollowCaravan((Llama) m, getDouble(g, "b"));
                case "LookAtPlayer" -> new PathfinderLookAtEntity<>(m, fromNMS(getObject(g, "a", Class.class), LivingEntity.class), getFloat(g, "d"), getFloat(g, "e"), getBoolean(g, "i"));
                case "LookAtTradingPlayer" -> new PathfinderLookAtTradingPlayer((AbstractVillager) m);
                case "MeleeAttack" -> new PathfinderMeleeAttack((Creature) m, getDouble(g, "b"), getBoolean(g, "c"));
                case "MoveThroughVillage" -> new PathfinderMoveThroughVillage((Creature) m, getObject(g, "b", BooleanSupplier.class), getDouble(g, "b"), getInt(g, "g"), getBoolean(g, "e"));
                case "NearestVillage" -> new PathfinderRandomStrollThroughVillage((Creature) m, getInt(g, "b"));
                case "GotoTarget" -> new PathfinderMoveToBlock((Creature) m, l -> (boolean) invoke(g, "a", toNMS(l.getWorld()), toNMS(l)), getDouble(g, "b"), getInt(g, "l"), getInt(g, "m"));
                case "Raid" -> new PathfinderMoveToRaid((Raider) m);
                case "MoveTowardsRestriction" -> new PathfinderMoveTowardsRestriction((Creature) m, getDouble(g, "e"));
                case "MoveTowardsTarget" -> new PathfinderMoveTowardsTarget((Creature) m, getDouble(g, "f"), getFloat(g, "g"));
                case "OcelotAttack" -> new PathfinderOcelotAttack((Ocelot) m);
                case "OfferFlower" -> new PathfinderOfferFlower((IronGolem) m);
                case "Panic" -> new PathfinderPanic((Creature) m, getDouble(g, "c"));
                case "Perch" -> new PathfinderRideShoulder((Parrot) m);
                case "RandomLookaround" -> new PathfinderRandomLook(m);
                case "RandomStroll" -> new PathfinderRandomStroll((Creature) m, getDouble(g, "f"), getInt(g, "g"));
                case "RandomStrollLand" -> new PathfinderRandomStrollLand((Creature) m, getDouble(RandomStrollGoal.class.cast(g), "f"), getFloat(g, "j"));
                case "RandomSwim" -> new PathfinderRandomSwim((Creature) m, getDouble(RandomStrollGoal.class.cast(g), "f"), getInt(RandomStrollGoal.class.cast(g), "g"));
                case "RandomFly" -> new PathfinderRandomStrollFlying((Creature) m, getDouble(RandomStrollGoal.class.cast(g), "f"));
                case "RemoveBlock" -> new PathfinderRemoveBlock((Creature) m, m.getWorld().getBlockAt(fromNMS(getPosWithBlock( getObject(g, "g", Block.class), toNMS(m.getLocation()), toNMS(m.getWorld())), m.getWorld())), getDouble(MoveToBlockGoal.class.cast(g), "b"));
                case "RestrictSun" -> new PathfinderRestrictSun((Creature) m);
                case "Sit" -> new PathfinderSit((Tameable) m);
                case "StrollVillage" -> new PathfinderRandomStrollToVillage((Creature) m, getDouble(RandomStrollGoal.class.cast(g), "f"));
                case "StrollVillageGolem" -> new PathfinderRandomStrollInVillage((Creature) m, getDouble(RandomStrollGoal.class.cast(g), "f"));
                case "Swell" -> new PathfinderSwellCreeper((Creeper) m);
                case "Tame" -> new PathfinderTameHorse((AbstractHorse) m);
                case "Tempt" -> new PathfinderTempt((Creature) m, getDouble(g, "e"), fromNMS(getObject(g, "m", Ingredient.class)));
                case "TradeWithPlayer" -> new PathfinderTradePlayer((AbstractVillager) m);
                case "UniversalAngerReset" -> new PathfinderResetAnger(m, getBoolean(g, "c"));
                case "UseItem" -> new PathfinderUseItem(m, fromNMS(getObject(g, "b", net.minecraft.world.item.ItemStack.class)), en -> getObject(g, "c", Predicate.class).test(toNMS(en)), fromNMS(getObject(g, "d", SoundEvent.class)));
                case "ZombieAttack" -> new PathfinderZombieAttack((Zombie) m, getDouble(MeleeAttackGoal.class.cast(g), "b"), getBoolean(MeleeAttackGoal.class.cast(g), "c"));

                // Target
                case "NearestAttackableTarget" -> new PathfinderNearestAttackableTarget<>(m, fromNMS(getObject(g, "a", Class.class), LivingEntity.class), getInt(g, "b"), getBoolean(TargetGoal.class.cast(g), "f"), getBoolean(TargetGoal.class.cast(g), "d"));
                case "DefendVillage" -> new PathfinderDefendVillage((IronGolem) m);
                case "HurtByTarget" -> new PathfinderHurtByTarget((Creature) m, getEntityTypes(getObject(g, "i", Class[].class)));
                case "OwnerHurtByTarget" -> new PathfinderOwnerHurtByTarget((Tameable) m);
                case "OwnerHurtTarget" -> new PathfinderOwnerHurtTarget((Tameable) m);

                default -> custom(g);
            };
        } else {
            if (name.equalsIgnoreCase("ClimbOnTopOfPowderSnowGoal")) return new PathfinderClimbPowderedSnow(m, fromNMS(getObject(g, "b", Level.class)));
            return custom(g);
        }
    }

}
